import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
    jacoco
}

android {
    namespace = "com.tiktok.sparkling"
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
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.jvmArgs("-Xmx2048m", "-XX:MaxMetaspaceSize=512m")
                it.systemProperty("robolectric.logging.enabled", "true")
                it.systemProperty(
                    "user.home",
                    buildDir.resolve("robolectric-home").absolutePath,
                )
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.json:json:20231013")
    testImplementation("androidx.fragment:fragment-testing:1.6.2")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    api(project(":sparkling-method"))

    // lynx dependencies
    api(libs.lynx)
    api(libs.lynx.jssdk)
    api(libs.lynx.trace)
    api(libs.primjs)
    api(libs.lynx.service.image)
    api(libs.lynx.service.log)
    api(libs.lynx.service.http)
//    api(libs.okhttp)

    api(libs.lynx.service.devtool)
    api(libs.lynx.devtool)
}

tasks.withType<Test>().configureEach {
    doFirst {
        buildDir.resolve("robolectric-home").mkdirs()
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

val publishingGroupId =
    (findProperty("SPARKLING_PUBLISHING_GROUP_ID") as? String)
        ?: System.getenv("SPARKLING_PUBLISHING_GROUP_ID")
        ?: "com.tiktok.sparkling"
val publishingVersion =
    (findProperty("SPARKLING_PUBLISHING_VERSION") as? String)
        ?: System.getenv("SPARKLING_PUBLISHING_VERSION")
        ?: "2.0.0"

val androidSourcesJar by tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")
    from(
        android.sourceSets
            .getByName("main")
            .java.srcDirs,
    )
}

val emptyJavadocJar by tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
}

afterEvaluate {
    extensions.configure<PublishingExtension>("publishing") {
        publications {
            create<MavenPublication>("release") {
                groupId = publishingGroupId
                artifactId = "sparkling"
                version = publishingVersion

                from(components["release"])
                artifact(androidSourcesJar)
                artifact(emptyJavadocJar)

                pom {
                    name.set("sparkling-sdk")
                    description.set("Sparkling Android SDK core module")
                    url.set("https://github.com/tiktok/sparkling")

                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("tiktok")
                            name.set("TikTok")
                            email.set("opensource@tiktok.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/tiktok/sparkling.git")
                        developerConnection.set("scm:git:ssh://github.com:tiktok/sparkling.git")
                        url.set("https://github.com/tiktok/sparkling")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "MavenCentral"
                // Central Portal staging API (replaces legacy s01.oss.sonatype.org shut down June 2025)
                val repoUrl =
                    (findProperty("mavenCentralRepoUrl") as? String)
                        ?: System.getenv("MAVEN_CENTRAL_REPO_URL")
                        ?: "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                url = uri(repoUrl)
                credentials {
                    username = (findProperty("mavenCentralUsername") as? String)
                        ?: System.getenv("MAVEN_CENTRAL_USERNAME")
                        ?: ""
                    password = (findProperty("mavenCentralPassword") as? String)
                        ?: System.getenv("MAVEN_CENTRAL_PASSWORD")
                        ?: ""
                }
            }
        }
    }
}

// Signing configuration
signing {
    val signingKeyId =
        (findProperty("signing.keyId") as? String)
            ?: System.getenv("SIGNING_KEY_ID")
    val signingPassword =
        (findProperty("signing.password") as? String)
            ?: System.getenv("SIGNING_PASSWORD")
    val signingSecretKeyRingFile =
        (findProperty("signing.secretKeyRingFile") as? String)
            ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    val signingKey = System.getenv("SIGNING_KEY")

    if (!signingKeyId.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        if (!signingKey.isNullOrBlank()) {
            // Use in-memory key (for CI/CD)
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            println("Using in-memory GPG key for signing")
        } else if (!signingSecretKeyRingFile.isNullOrBlank() && file(signingSecretKeyRingFile).exists()) {
            // Use key ring file (for local development)
            useInMemoryPgpKeys(signingKeyId, file(signingSecretKeyRingFile).readText(), signingPassword)
            println("Using GPG key ring file for signing: $signingSecretKeyRingFile")
        } else {
            println("Warning: Signing key ID and password provided but no key content available")
        }
    } else {
        println("Warning: GPG signing not configured. Set SIGNING_KEY_ID and SIGNING_PASSWORD environment variables.")
    }
}

// Sign publications after they are configured
afterEvaluate {
    signing {
        val hasSigningConfig =
            !(
                System.getenv("SIGNING_KEY_ID").isNullOrBlank() ||
                    System.getenv("SIGNING_PASSWORD").isNullOrBlank()
            )
        if (hasSigningConfig) {
            sign(extensions.getByType<PublishingExtension>().publications["release"])
        } else {
            println("Skipping signing for publication 'release' - no signing configuration")
        }
    }
}
