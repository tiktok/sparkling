import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.tiktok.sparkling.debugtool"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
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
    implementation(libs.lynx)
    implementation(libs.lynx.service.log)
    implementation(libs.lynx.service.devtool)
    implementation(libs.lynx.devtool)
}

val publishingGroupId = (findProperty("SPARKLING_PUBLISHING_GROUP_ID") as? String)
    ?: System.getenv("SPARKLING_PUBLISHING_GROUP_ID")
    ?: "com.tiktok.sparkling"
val publishingVersion = (findProperty("SPARKLING_PUBLISHING_VERSION") as? String)
    ?: System.getenv("SPARKLING_PUBLISHING_VERSION")
    ?: "2.0.0"

val androidSourcesJar by tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

val emptyJavadocJar by tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
}

afterEvaluate {
    extensions.configure<PublishingExtension>("publishing") {
        publications {
            create<MavenPublication>("release") {
                groupId = publishingGroupId
                artifactId = "sparkling-debug-tool"
                version = publishingVersion

                from(components["release"])
                artifact(androidSourcesJar)
                artifact(emptyJavadocJar)

                pom {
                    name.set("sparkling-debug-tool")
                    description.set("Sparkling Android Lynx debug tool module")
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
                val repoUrl = (findProperty("mavenCentralRepoUrl") as? String)
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

signing {
    val signingKeyId = (findProperty("signing.keyId") as? String)
        ?: System.getenv("SIGNING_KEY_ID")
    val signingPassword = (findProperty("signing.password") as? String)
        ?: System.getenv("SIGNING_PASSWORD")
    val signingSecretKeyRingFile = (findProperty("signing.secretKeyRingFile") as? String)
        ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    val signingKey = System.getenv("SIGNING_KEY")

    if (!signingKeyId.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        if (!signingKey.isNullOrBlank()) {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            println("Using in-memory GPG key for signing")
        } else if (!signingSecretKeyRingFile.isNullOrBlank() && file(signingSecretKeyRingFile).exists()) {
            useInMemoryPgpKeys(signingKeyId, file(signingSecretKeyRingFile).readText(), signingPassword)
            println("Using GPG key ring file for signing: $signingSecretKeyRingFile")
        } else {
            println("Warning: Signing key ID and password provided but no key content available")
        }
    } else {
        println("Warning: GPG signing not configured. Set SIGNING_KEY_ID and SIGNING_PASSWORD environment variables.")
    }
}

afterEvaluate {
    signing {
        val hasSigningConfig = !(System.getenv("SIGNING_KEY_ID").isNullOrBlank() ||
            System.getenv("SIGNING_PASSWORD").isNullOrBlank())
        if (hasSigningConfig) {
            sign(extensions.getByType<PublishingExtension>().publications["release"])
        } else {
            println("Skipping signing for publication 'release' - no signing configuration")
        }
    }
}
