import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.Test
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("maven-publish")
    id("signing")
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

group = "com.decimal128"
version = "0.9.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("documentation/html"))
}

tasks.dokkaGfm {
    outputDirectory.set(layout.buildDirectory.dir("documentation/markdown"))
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {

    applyDefaultHierarchyTemplate()

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js {
        browser {
            testTask {
                useMocha {
                    timeout = "30000"
                }
            }
        }
        nodejs {
            testTask {
                useMocha {
                    timeout = "30000"
                }
            }
        }
    }

    wasmJs {
        browser {
            testTask {
                testLogging.showStandardStreams = true
                useMocha {
                    timeout = "60s"
                }
            }
        }
        nodejs()
    }

    wasmWasi {
        nodejs()
    }

    macosX64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                api(project(":bigint"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
        showStandardStreams = true
    }
}

afterEvaluate {
    publishing {
        publications.withType<MavenPublication>().configureEach {
            when (name) {
                "kotlinMultiplatform" -> artifactId = "bigint-math"
                "jvm" -> artifactId = "bigint-math-jvm"
                "js" -> artifactId = "bigint-math-js"
                "wasmJs" -> artifactId = "bigint-math-wasm-js"
                "wasmWasi" -> artifactId = "bigint-math-wasm-wasi"
                "macosX64" -> artifactId = "bigint-math-macosx64"
                "linuxX64" -> artifactId = "bigint-math-linuxx64"
            }
            pom {
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }
}
