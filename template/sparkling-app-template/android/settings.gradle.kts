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

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.lynxsdk.lynx:lynx-library-plugin:3.9.0")
    }
}

apply(plugin = "org.lynxsdk.library-settings")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Sparkling"
include(":app")

// BEGIN SPARKLING AUTOLINK
val sparklingAutolinkProjects =
    listOf<Pair<String, java.io.File>>(
        "sparkling-navigation" to file("../node_modules/sparkling-navigation/android"),
    )
sparklingAutolinkProjects.forEach { (name, dir) ->
    include(":$name")
    project(":$name").projectDir = dir
}
// END SPARKLING AUTOLINK
