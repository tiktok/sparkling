package com.example.sparkling.go

data class SparklingAutolinkModule(val name: String, val androidPackage: String?, val className: String?)

object SparklingAutolink {
    val modules = listOf(
        SparklingAutolinkModule(name = "sparkling-media", androidPackage = "", className = ""),
        SparklingAutolinkModule(name = "sparkling-storage", androidPackage = "com.tiktok.sparkling.methods.storage", className = "StorageMethod"),
        SparklingAutolinkModule(name = "sparkling-navigation", androidPackage = "com.tiktok.sparkling.methods.router", className = "RouterMethod")
    )
}
