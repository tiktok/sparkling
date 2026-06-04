package com.example.sparkling.go

data class SparklingAutolinkModule(
    val name: String,
    val androidPackage: String?,
    val className: String?,
    val methodClassNames: List<String> = emptyList(),
)

object SparklingAutolink {
    val modules =
        listOf(
            SparklingAutolinkModule(
                name = "sparkling-navigation",
                androidPackage = "com.tiktok.sparkling.method.router",
                className = "RouterMethod",
                methodClassNames =
                    listOf(
                        "com.tiktok.sparkling.method.router.open.RouterOpenMethod",
                        "com.tiktok.sparkling.method.router.close.RouterCloseMethod",
                    ),
            ),
        )
}
