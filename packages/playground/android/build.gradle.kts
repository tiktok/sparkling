plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}

buildscript {
    repositories {
        mavenLocal()
    }
}

// Apply centralized Maven Publish config for all subprojects (safe-guarded for library modules)
subprojects {
    apply(from = "$rootDir/publish.gradle")
}
