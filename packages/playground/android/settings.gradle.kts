pluginManagement {
    repositories {
        mavenLocal()
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
}
}

rootProject.name = "Sparkling"
include(":app")
include(":sparkling")
project(":sparkling").projectDir = file("../../../packages/sparkling-sdk/android/sparkling")
include(":sparkling-method")
project(":sparkling-method").projectDir = file("../../../packages/sparkling-method/android")

// BEGIN SPARKLING AUTOLINK
val sparklingAutolinkProjects = listOf<Pair<String, java.io.File>>(
  "sparkling-media" to file("../../methods/sparkling-media/android"),
  "sparkling-navigation" to file("../../methods/sparkling-navigation/android"),
  "sparkling-storage" to file("../../methods/sparkling-storage/android"),
  "sparkling-debug-tool" to file("../../sparkling-debug-tool/android")
)
sparklingAutolinkProjects.forEach { (name, dir) ->
    include(":$name")
    project(":$name").projectDir = dir
}
// END SPARKLING AUTOLINK
