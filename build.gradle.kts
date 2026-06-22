// Root aggregator project. The actual libraries live in the :bigint (core) and
// :bigint-math subprojects. Plugin versions are declared here once with
// `apply false`; the subprojects apply them without a version.
plugins {
    kotlin("multiplatform") version "2.2.0" apply false
    id("org.jetbrains.dokka") version "2.0.0" apply false
}
