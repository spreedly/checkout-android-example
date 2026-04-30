// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.binary.compatibility.validator) apply true
    alias(libs.plugins.versions) apply true
    alias(libs.plugins.firebase.app.distribution) apply false
    alias(libs.plugins.google.services) apply false
}

tasks.withType<Jar>().configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

apiValidation {
    ignoredProjects += listOf("app")
}


// Add Compose-specific lint task


// Configure dependencies so root project can aggregate coverage and documentation from all modules

// Configure Dokka V2 multi-module documentation

// Configure documentation settings for subprojects with Dokka applied

// Map each Android submodule's prodDebug variant into the "custom" Kover variant
// so the root project can generate merged variant-specific reports.
// Also propagate the root-level report filters so per-module reports match the
// aggregated report (exclude Activity, UI, model, and generated classes).

// Create a task to generate unified documentation using Dokka V2

// Add convenience task that's easier to remember

// ============================================================================
// Gradle Versions Plugin Configuration
// ============================================================================
// Configure dependency update checking for automated updates
// ============================================================================

tasks.withType<DependencyUpdatesTask> {
    // Only show stable versions (no alpha, beta, rc, etc.)
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }

    // Output directory for reports
    outputDir = "${project.layout.buildDirectory.get()}/dependencyUpdates"
    reportfileName = "report"
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
