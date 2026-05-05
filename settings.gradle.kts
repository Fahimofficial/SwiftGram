rootProject.name = "SwiftGram"
include(":app")
include(":domain")
include(":data")
include(":core")

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
        
        // Add local repository for TDLib artifacts (if using local files)
        flatDir {
            dirs("data/libs")
        }
    }
}
