plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    jacoco
}

android {
    namespace = "com.tiktok.sparkling.method.storage"
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
                "proguard-rules.pro",
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

    testOptions {
        unitTests.all {
            it.extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class.java) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    val sparklingVersion =
        (findProperty("SPARKLING_ANDROID_SDK_VERSION") as? String)
            ?: System.getenv("SPARKLING_ANDROID_SDK_VERSION")
            ?: "2.1.0-rc.12"
    if (rootProject.findProject(":sparkling-method") != null) {
        api(project(":sparkling-method"))
    } else {
        api("com.tiktok.sparkling:sparkling-method:$sparklingVersion")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("testDebugUnitTest"))

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
        )

    val mainSrc = "${project.projectDir}/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))

    val debugJavaTree =
        layout.buildDirectory.dir("intermediates/javac/debug").map { dir ->
            dir.asFileTree.matching {
                exclude(fileFilter)
            }
        }
    val debugKotlinTree =
        layout.buildDirectory.dir("tmp/kotlin-classes/debug").map { dir ->
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
