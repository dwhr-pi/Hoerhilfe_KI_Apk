param(
    [string]$ProjectRoot = "C:\Users\danie\Documents\GitHub\Ultimate_KI_Setup\android-hearing-assist"
)

$ErrorActionPreference = "Stop"

$repoRoot = Join-Path $ProjectRoot "local-maven"
$repos = @(
    "https://dl.google.com/dl/android/maven2",
    "https://repo.maven.apache.org/maven2",
    "https://plugins.gradle.org/m2"
)

$visited = [System.Collections.Generic.HashSet[string]]::new()
$depMgmt = @{}

function Expand-Properties {
    param(
        [string]$Value,
        [hashtable]$Properties
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $Value
    }

    $expanded = $Value
    $guard = 0
    while ($expanded -match '\$\{([^}]+)\}' -and $guard -lt 20) {
        $guard++
        $expanded = [regex]::Replace($expanded, '\$\{([^}]+)\}', {
            param($match)
            $name = $match.Groups[1].Value
            if ($Properties.ContainsKey($name)) {
                return [string]$Properties[$name]
            }
            return $match.Value
        })
    }

    return $expanded
}

function Normalize-Version {
    param(
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $Value
    }

    $trimmed = $Value.Trim()
    if (($trimmed.StartsWith("[") -or $trimmed.StartsWith("(")) -and ($trimmed.EndsWith("]") -or $trimmed.EndsWith(")"))) {
        $inner = $trimmed.Substring(1, $trimmed.Length - 2)
        if ($inner -notmatch ",") {
            return $inner
        }
        $parts = $inner.Split(",") | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
        if ($parts.Count -gt 0) {
            return $parts[-1].Trim()
        }
    }

    return $trimmed
}

function Get-ChildText {
    param(
        [System.Xml.XmlNode]$Node,
        [string]$Name
    )

    $child = $Node.SelectSingleNode("*[local-name()='$Name']")
    if ($null -eq $child) {
        return $null
    }
    return $child.InnerText.Trim()
}

function Save-RemoteFile {
    param(
        [string]$RelativePath,
        [string]$TargetFile
    )

    foreach ($repo in $repos) {
        $uri = "$repo/$RelativePath"
        try {
            Invoke-WebRequest -Uri $uri -OutFile $TargetFile | Out-Null
            return $true
        } catch {
        }
    }
    return $false
}

function Read-Pom {
    param(
        [string]$GroupId,
        [string]$ArtifactId,
        [string]$Version
    )

    $basePath = ($GroupId -replace '\.', '\') + "\$ArtifactId\$Version"
    $pomPath = Join-Path $repoRoot "$basePath\$ArtifactId-$Version.pom"
    if (-not (Test-Path $pomPath)) {
        New-Item -ItemType Directory -Force -Path (Split-Path $pomPath) | Out-Null
        if (-not (Save-RemoteFile -RelativePath (($GroupId -replace '\.', '/') + "/$ArtifactId/$Version/$ArtifactId-$Version.pom") -TargetFile $pomPath)) {
            throw "Missing POM for ${GroupId}:${ArtifactId}:${Version}"
        }
    }
    return [xml](Get-Content -Path $pomPath -Raw)
}

function Mirror-Artifact {
    param(
        [string]$GroupId,
        [string]$ArtifactId,
        [string]$Version,
        [ValidateSet("jar", "aar", "pom")]
        [string]$Packaging = "jar"
    )

    $Version = Normalize-Version $Version
    if ([string]::IsNullOrWhiteSpace($Version)) {
        throw "No version available for ${GroupId}:${ArtifactId}"
    }

    $key = "${GroupId}:${ArtifactId}:${Version}"
    if ($visited.Contains($key)) {
        return
    }
    $visited.Add($key) | Out-Null

    $pom = Read-Pom -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version
    $project = $pom.project
    $props = @{}
    $props["project.groupId"] = (Get-ChildText $project "groupId")
    $props["project.artifactId"] = (Get-ChildText $project "artifactId")
    $props["project.version"] = (Get-ChildText $project "version")
    $props["pom.groupId"] = $props["project.groupId"]
    $props["pom.artifactId"] = $props["project.artifactId"]
    $props["pom.version"] = $props["project.version"]

    $parentNode = $project.SelectSingleNode("*[local-name()='parent']")
    if ($null -ne $parentNode) {
        if (-not $props["project.groupId"]) {
            $props["project.groupId"] = Get-ChildText $parentNode "groupId"
        }
        if (-not $props["project.version"]) {
            $props["project.version"] = Get-ChildText $parentNode "version"
        }
        $props["parent.groupId"] = Get-ChildText $parentNode "groupId"
        $props["parent.version"] = Get-ChildText $parentNode "version"
    }

    $propertiesNode = $project.SelectSingleNode("*[local-name()='properties']")
    if ($null -ne $propertiesNode) {
        foreach ($child in $propertiesNode.ChildNodes) {
            if ($child.NodeType -eq [System.Xml.XmlNodeType]::Element) {
                $props[$child.LocalName] = $child.InnerText.Trim()
            }
        }
    }

    $effectivePackaging = Get-ChildText $project "packaging"
    if ([string]::IsNullOrWhiteSpace($effectivePackaging)) {
        $effectivePackaging = $Packaging
    }
    if ($effectivePackaging -eq "bundle") {
        $effectivePackaging = "jar"
    }

    $artifactBase = ($GroupId -replace '\.', '\') + "\$ArtifactId\$Version"
    $artifactDir = Join-Path $repoRoot $artifactBase
    New-Item -ItemType Directory -Force -Path $artifactDir | Out-Null

    if ($effectivePackaging -ne "pom") {
        $fileName = "$ArtifactId-$Version.$effectivePackaging"
        $artifactPath = Join-Path $artifactDir $fileName
        if (-not (Test-Path $artifactPath)) {
            if (-not (Save-RemoteFile -RelativePath (($GroupId -replace '\.', '/') + "/$ArtifactId/$Version/$fileName") -TargetFile $artifactPath)) {
                throw "Missing artifact for ${GroupId}:${ArtifactId}:${Version} ($effectivePackaging)"
            }
        }
    }

    $moduleName = "$ArtifactId-$Version.module"
    $modulePath = Join-Path $artifactDir $moduleName
    if (-not (Test-Path $modulePath)) {
        Save-RemoteFile -RelativePath (($GroupId -replace '\.', '/') + "/$ArtifactId/$Version/$moduleName") -TargetFile $modulePath | Out-Null
        if ((Test-Path $modulePath) -and (Get-Item $modulePath).Length -eq 0) {
            Remove-Item $modulePath -Force
        }
    }

    $depMgmtNodes = $project.SelectNodes("*[local-name()='dependencyManagement']/*[local-name()='dependencies']/*[local-name()='dependency']")
    foreach ($depNode in $depMgmtNodes) {
        $depGroup = Expand-Properties (Get-ChildText $depNode "groupId") $props
        $depArtifact = Expand-Properties (Get-ChildText $depNode "artifactId") $props
        $depVersion = Normalize-Version (Expand-Properties (Get-ChildText $depNode "version") $props)
        $depType = Expand-Properties (Get-ChildText $depNode "type") $props
        $depScope = Expand-Properties (Get-ChildText $depNode "scope") $props

        if (-not $depType) {
            $depType = "jar"
        }

        if ($depVersion) {
            $depMgmt["${depGroup}:${depArtifact}"] = @{ version = $depVersion; type = $depType }
        }

        if ($depScope -eq "import" -and $depType -eq "pom") {
            Mirror-Artifact -GroupId $depGroup -ArtifactId $depArtifact -Version $depVersion -Packaging "pom"
        }
    }

    $depNodes = $project.SelectNodes("*[local-name()='dependencies']/*[local-name()='dependency']")
    foreach ($depNode in $depNodes) {
        $depGroup = Expand-Properties (Get-ChildText $depNode "groupId") $props
        $depArtifact = Expand-Properties (Get-ChildText $depNode "artifactId") $props
        $depVersion = Normalize-Version (Expand-Properties (Get-ChildText $depNode "version") $props)
        $depScope = Expand-Properties (Get-ChildText $depNode "scope") $props
        $depOptional = Expand-Properties (Get-ChildText $depNode "optional") $props
        $depType = Expand-Properties (Get-ChildText $depNode "type") $props

        if ([string]::IsNullOrWhiteSpace($depGroup) -or [string]::IsNullOrWhiteSpace($depArtifact)) {
            continue
        }
        if ($depScope -in @("test", "provided", "system")) {
            continue
        }
        if ($depOptional -eq "true") {
            continue
        }
        if ([string]::IsNullOrWhiteSpace($depVersion) -and $depMgmt.ContainsKey("${depGroup}:${depArtifact}")) {
            $depVersion = $depMgmt["${depGroup}:${depArtifact}"].version
            if (-not $depType) {
                $depType = $depMgmt["${depGroup}:${depArtifact}"].type
            }
        }
        if ([string]::IsNullOrWhiteSpace($depVersion)) {
            continue
        }
        if ([string]::IsNullOrWhiteSpace($depType)) {
            $depType = "jar"
        }
        if ($depType -eq "bundle") {
            $depType = "jar"
        }
        if ($depType -notin @("jar", "aar", "pom")) {
            $depType = "jar"
        }

        Mirror-Artifact -GroupId $depGroup -ArtifactId $depArtifact -Version $depVersion -Packaging $depType
    }
}

$roots = @(
    @{ g = "com.android.application"; a = "com.android.application.gradle.plugin"; v = "8.5.2"; p = "pom" },
    @{ g = "org.jetbrains.kotlin.android"; a = "org.jetbrains.kotlin.android.gradle.plugin"; v = "1.9.24"; p = "pom" },
    @{ g = "androidx.compose"; a = "compose-bom"; v = "2024.06.00"; p = "pom" },
    @{ g = "androidx.core"; a = "core-ktx"; v = "1.13.1"; p = "aar" },
    @{ g = "androidx.activity"; a = "activity-compose"; v = "1.9.0"; p = "aar" },
    @{ g = "androidx.lifecycle"; a = "lifecycle-runtime-ktx"; v = "2.8.3"; p = "aar" },
    @{ g = "androidx.lifecycle"; a = "lifecycle-viewmodel"; v = "2.8.3"; p = "aar" },
    @{ g = "androidx.lifecycle"; a = "lifecycle-viewmodel-ktx"; v = "2.8.3"; p = "aar" },
    @{ g = "androidx.lifecycle"; a = "lifecycle-viewmodel-savedstate"; v = "2.8.3"; p = "aar" },
    @{ g = "androidx.savedstate"; a = "savedstate"; v = "1.2.1"; p = "aar" },
    @{ g = "androidx.compose.runtime"; a = "runtime"; v = "1.6.8"; p = "aar" },
    @{ g = "androidx.compose.runtime"; a = "runtime-saveable"; v = "1.6.8"; p = "aar" },
    @{ g = "androidx.compose.material3"; a = "material3"; v = $null; p = "aar" },
    @{ g = "androidx.compose.material3"; a = "material3-android"; v = "1.2.1"; p = "aar" },
    @{ g = "androidx.compose.ui"; a = "ui"; v = $null; p = "aar" },
    @{ g = "androidx.compose.ui"; a = "ui-android"; v = "1.6.8"; p = "aar" },
    @{ g = "androidx.compose.ui"; a = "ui-tooling-preview"; v = $null; p = "aar" },
    @{ g = "androidx.compose.foundation"; a = "foundation"; v = $null; p = "aar" },
    @{ g = "androidx.compose.material"; a = "material-icons-extended"; v = $null; p = "aar" },
    @{ g = "androidx.compose.ui"; a = "ui-tooling"; v = $null; p = "aar" },
    @{ g = "androidx.compose.ui"; a = "ui-test-manifest"; v = $null; p = "aar" },
    @{ g = "androidx.compose.ui"; a = "ui-test-junit4"; v = $null; p = "aar" },
    @{ g = "org.jetbrains.kotlinx"; a = "kotlinx-coroutines-android"; v = "1.8.1"; p = "jar" },
    @{ g = "org.jetbrains.kotlin"; a = "kotlin-stdlib-common"; v = "1.9.24"; p = "jar" },
    @{ g = "junit"; a = "junit"; v = "4.13.2"; p = "jar" },
    @{ g = "androidx.test.ext"; a = "junit"; v = "1.2.1"; p = "aar" },
    @{ g = "androidx.test.espresso"; a = "espresso-core"; v = "3.6.1"; p = "aar" }
)

Mirror-Artifact -GroupId "androidx.compose" -ArtifactId "compose-bom" -Version "2024.06.00" -Packaging "pom"

foreach ($root in $roots) {
    $version = $root.v
    if (-not $version -and $depMgmt.ContainsKey("$($root.g):$($root.a)")) {
        $version = $depMgmt["$($root.g):$($root.a)"].version
    }
    Mirror-Artifact -GroupId $root.g -ArtifactId $root.a -Version $version -Packaging $root.p
}

Write-Host "Mirrored artifacts into $repoRoot"
