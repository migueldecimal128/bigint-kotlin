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

/*
 * Dokka tasks (V2 migration helpers may print a warning; that's fine).
 * Keep these simple so the plugin resolves correctly.
 */
tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("documentation/html"))
}

tasks.dokkaGfm {
    outputDirectory.set(layout.buildDirectory.dir("documentation/markdown"))
}

/*
 * Optional: a JVM test task configured with diagnostic args (keeps your prior behavior).
 * It depends on jvmTestClasses, so it's only relevant if the JVM tests are present.
 */
tasks.register<Test>("testHsdis") {
    group = "verification"
    description = "Runs tests with JIT disassembly enabled (JVM-only)"

    // depend on jvm test classes task - will exist for KMP JVM target
    dependsOn("jvmTestClasses")
    useJUnitPlatform()

    jvmArgs(
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+PrintInlining",
        "-XX:+PrintAssembly",
        "-XX:PrintAssemblyOptions=syntax=intel",
        "-XX:CompileThreshold=1"
    )

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



/*
 * Kotlin Multiplatform configuration.
 *
 * Note:
 *  - jvmToolchain(...) is set at the kotlin-extension level (affects all JVM compilations).
 *  - This file intentionally does NOT call the newer compilerOptions DSL (which had
 *    caused unresolved-reference errors in your environment). The Kotlin plugin will
 *    emit suggestions but this configuration is stable and will build.
 */
@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {

    applyDefaultHierarchyTemplate()

    // ---------------------------
    // 1. Target configurations
    // ---------------------------

    jvm {
        // your existing JVM test setup (keep it)
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js {
        browser {
            testTask {
                useMocha {
                    timeout = "30000"  // 30 seconds in milliseconds
                }
            }
        }
        nodejs {
            testTask {
                useMocha {
                    timeout = "30000"  // 30 seconds in milliseconds
                }
            }
        }
    }

    wasmJs {
        browser {
            testTask {
                // Extend timeout for browser tests
                testLogging.showStandardStreams = true

                // Use Karma configuration
                useMocha {
                    timeout = "60s" // or "60000" for milliseconds
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

    // ---------------------------
    // 2. Source-set hierarchy
    // ---------------------------

    sourceSets {
        /*
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }

         */

        val commonMain by getting {
            dependencies {
                // Use the artifact name as a string, passed to a specific dependency helper
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // Intermediate source set shared by every non-JVM target. Holds the pure
        // Kotlin unsignedMulHi actual so native/js/wasmJs/wasmWasi share one copy
        // (the JVM keeps its own Math.unsignedMultiplyHigh actual).
        val nonJvmMain by creating {
            dependsOn(commonMain)
        }
        val nativeMain by getting { dependsOn(nonJvmMain) }
        val jsMain by getting { dependsOn(nonJvmMain) }
        val wasmJsMain by getting { dependsOn(nonJvmMain) }
        val wasmWasiMain by getting { dependsOn(nonJvmMain) }

        val macosX64Main by getting
        //val macosArm64Main by getting
        //val linuxX64Main by getting
        //val mingwX64Main by getting


    }

}

/*
 * Optional: copy native libs for tests if you use native test fixtures.
 * If you don't have any native directory, this task is harmless but can be removed.
 */
val nativeInputDir = layout.projectDirectory.dir("native")
val nativeTestDir = layout.buildDirectory.dir("native")

tasks.register<Copy>("copyNativeForTests") {
    from(nativeInputDir)
    into(nativeTestDir)
}

/*
 * Ensure all Test tasks (JVM) get configured to run and see native lib path if necessary.
 * If you don't use JNA/native libs, this is harmless (property simply set).
 */
tasks.withType<Test> {
    dependsOn("copyNativeForTests")
    // set property even if nativeTestDir doesn't contain anything
    systemProperty("jna.library.path", nativeTestDir.get().asFile.absolutePath)

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
                "kotlinMultiplatform" -> artifactId = "bigint"
                "jvm" -> artifactId = "bigint-jvm"
                "js" -> artifactId = "bigint-js"
                "wasmJs" -> artifactId = "bigint-wasm-js"
                "wasmWasi" -> artifactId = "bigint-wasm-wasi"
                "macosX64" -> artifactId = "bigint-macosx64"
                "linuxX64" -> artifactId = "bigint-linuxx64"
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