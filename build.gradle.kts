// Root build file for the public checkout-android-example repo. sync-sample.yml
// writes this in place of the SDK's root build.gradle.kts, which carries release
// plumbing (ABI baselines, japicmp, Kover CLI) the sample doesn't ship.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.firebase.app.distribution) apply false
    alias(libs.plugins.google.services) apply false
}
