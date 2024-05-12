import java.io.FileInputStream
import java.util.Properties

val localPropertiesFile: File = File(rootDir, "local.properties")
val localProperties: Properties = Properties()

if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "siOptik"
include(":app")
include(":opencv")

if (localProperties.containsKey("opencvSdkPath")) {
    project(":opencv").projectDir = File(localProperties.getProperty("opencvSdkPath"))
} else {
    throw Error("opencvSdkPath is not initialized in local.properties")
}
