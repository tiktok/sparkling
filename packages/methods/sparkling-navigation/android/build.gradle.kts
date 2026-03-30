plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    jacoco
}

android {
    namespace = "com.tiktok.sparkling.method.router"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    val sparklingVersion = (findProperty("SPARKLING_ANDROID_SDK_VERSION") as? String)
        ?: System.getenv("SPARKLING_ANDROID_SDK_VERSION")
        ?: "2.1.0-rc.12"
    api("com.tiktok.sparkling:sparkling-method:$sparklingVersion")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("testDebugUnitTest"))

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val mainSrc = "${project.projectDir}/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))

    val debugJavaTree = layout.buildDirectory.dir("intermediates/javac/debug").map { dir ->
        dir.asFileTree.matching {
            exclude(fileFilter)
        }
    }
    val debugKotlinTree = layout.buildDirectory.dir("tmp/kotlin-classes/debug").map { dir ->
        dir.asFileTree.matching {
            exclude(fileFilter)
        }
    }
    classDirectories.setFrom(debugJavaTree, debugKotlinTree)

    val unitTestCoverageExec = layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    val jacocoExec = layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")
    executionData.setFrom(files(unitTestCoverageExec, jacocoExec))

    onlyIf {
        executionData.files.any { it.exists() }
    }
    outputs.upToDateWhen { false }
}
