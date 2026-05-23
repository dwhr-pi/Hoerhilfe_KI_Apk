$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$jbrBin = "C:\Program Files\Android\Android Studio\jbr\bin"
$keytool = Join-Path $jbrBin "keytool.exe"

if (-not (Test-Path $keytool)) {
    throw "keytool.exe wurde nicht gefunden unter $keytool"
}

$keystoreDir = Join-Path $repoRoot "keystore"
New-Item -ItemType Directory -Force -Path $keystoreDir | Out-Null

$storeFile = Join-Path $keystoreDir "hearing-assist-release.jks"
$alias = "hearingassist"

$storePassword = Read-Host "Keystore-Passwort"
$keyPassword = Read-Host "Key-Passwort"
$dname = Read-Host "Distinguished Name, z. B. CN=Dein Name, OU=Dev, O=Firma, L=Berlin, S=Berlin, C=DE"

& $keytool -genkeypair `
    -v `
    -keystore $storeFile `
    -alias $alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass $storePassword `
    -keypass $keyPassword `
    -dname $dname

$propertiesFile = Join-Path $repoRoot "release-signing.properties"
@"
storeFile=keystore/hearing-assist-release.jks
storePassword=$storePassword
keyAlias=$alias
keyPassword=$keyPassword
"@ | Set-Content -Path $propertiesFile -Encoding ASCII

Write-Host ""
Write-Host "Keystore erzeugt:"
Write-Host "  $storeFile"
Write-Host "Signing-Konfiguration geschrieben:"
Write-Host "  $propertiesFile"
