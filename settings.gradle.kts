pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Add repository for Kotlin/Native prebuilt binaries
        maven {
            url = uri("https://download.jetbrains.com/kotlin/native/builds")
            name = "Kotlin Native"
        }
    }
}

rootProject.name = "WorkoutApp"
include(":app")
include(":shared")