pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NovaFiles 2.0"

include(":app")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:ui")
include(":core:navigation")
include(":core:storage")
include(":data:files")
include(":feature:files")
include(":feature:dualpane")
include(":core:media")
include(":feature:music")
include(":feature:video")
include(":feature:gallery")
include(":core:security")
include(":feature:vault")
include(":feature:analyzer")
include(":feature:cleaner")
include(":feature:ftp")
include(":feature:settings")
