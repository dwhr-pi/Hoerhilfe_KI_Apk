plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

allprojects {
    val externalBuildRoot = file("C:/Users/danie/AppData/Local/Temp/hearingassist-gradle-build")
    layout.buildDirectory.set(externalBuildRoot.resolve(name))
}
