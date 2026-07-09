import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.firebase.app.distribution) apply false
}

// Conditionally apply Google Services plugin only when the app module itself builds a Prod variant.
// We must scope the check to app-specific tasks so that other modules' Prod tasks
// (e.g. payments-core:testProdDebugUnitTest) don't accidentally trigger the plugin.
if (project.hasProperty("applyGoogleServices") ||
    gradle.startParameter.taskNames.any { task ->
        val normalized = task.removePrefix(":")
        val isAppTask = normalized.startsWith("app:") || !normalized.contains(":")
        isAppTask && (normalized.contains("Prod") || normalized.contains("distributeToFirebase"))
    }
) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    compileSdk = 36

    flavorDimensions += "contentType"
    productFlavors {
        create("development") {
            dimension = "contentType"
            applicationIdSuffix = ".dev"
        }
        create("prod") {
            dimension = "contentType"
        }
    }
    namespace = "com.spreedly.app"

    lint {
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val keystoreFile = project.rootProject.file("apikeys.properties")
        val exampleKeystoreFile = project.rootProject.file("apikeys.properties.example")
        val properties = Properties()

        when {
            keystoreFile.exists() -> {
                properties.load(keystoreFile.inputStream())
            }

            exampleKeystoreFile.exists() -> {
                properties.load(exampleKeystoreFile.inputStream())
                println("Warning: Using example apikeys.properties file with dummy values")
            }

            else -> {
                throw GradleException("Neither apikeys.properties nor apikeys.properties.example found!")
            }
        }
        minSdk = 26
        targetSdk = 35

        applicationId = "com.spreedly.app"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val token = properties.getProperty("token") ?: ""
        val nonce = properties.getProperty("nonce") ?: ""
        val timestamp = properties.getProperty("timestamp") ?: ""
        val certificateToken = properties.getProperty("certificateToken") ?: ""
        val signature = properties.getProperty("signature") ?: ""
        val environmentKey = properties.getProperty("environmentKey") ?: ""
        val forterSiteId = properties.getProperty("forterSiteId") ?: ""
        val stripePublishableKey = properties.getProperty("stripePublishableKey") ?: ""

        buildConfigField("String", "TOKEN", "\"${token.replace("\"", "\\\"")}\"")
        buildConfigField("String", "NONCE", "\"${nonce.replace("\"", "\\\"")}\"")
        buildConfigField("String", "TIMESTAMP", "\"${timestamp.replace("\"", "\\\"")}\"")
        buildConfigField("String", "CERTIFICATE_TOKEN", "\"${certificateToken.replace("\"", "\\\"")}\"")
        buildConfigField("String", "SIGNATURE", "\"${signature.replace("\"", "\\\"")}\"")
        buildConfigField("String", "ENVIRONMENT_KEY", "\"${environmentKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "FORTER_SITE_ID", "\"${forterSiteId.replace("\"", "\\\"")}\"")
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"${stripePublishableKey.replace("\"", "\\\"")}\"")

        val enableJavaOffsitePayment =
            project.findProperty("enableJavaOffsitePayment")?.toString()?.toBooleanStrictOrNull() ?: false
        buildConfigField("boolean", "ENABLE_JAVA_OFFSITE_PAYMENT", enableJavaOffsitePayment.toString())
    }

    buildTypes {
        debug {
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/NOTICE.md"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // ✅ Use paymentsheet which includes payments-core and hosted-fields
    implementation("com.spreedly:checkout-paymentsheet:1.1.0")
    implementation("com.spreedly:checkout-braintree-apm:1.1.0")
    implementation("com.spreedly:checkout-stripe-apm:1.1.0")
    implementation("com.spreedly:checkout-stripe-radar:1.1.0")
    implementation("com.spreedly:checkout-threeds:1.1.0")

    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // For traditional XML layouts
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.ktor.client.mock)

    // Android Test dependencies for integration tests
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.activity.compose)
    androidTestImplementation(libs.hilt.android.testing)

    // AndroidX test dependencies
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Firebase App ID will be resolved at runtime
tasks.register("distributeToFirebase") {
    group = "distribution"
    description = "Builds APK, bumps version, and uploads to Firebase App Distribution"

    val buildType = project.findProperty("buildType")?.toString() ?: "debug"
    val testGroup = project.findProperty("group")?.toString() ?: "Internal"
    val releaseNotesFile = file("release-notes.txt")
    val buildGradleFile = file("build.gradle.kts")

    doLast {
        // Resolve Firebase App ID at runtime
        val firebaseAppId = System.getenv("FIREBASE_APP_ID")
            ?: project.findProperty("FIREBASE_APP_ID")?.toString()
            ?: throw GradleException("FIREBASE_APP_ID must be set as environment variable or project property")

        val authCheckOutput = ByteArrayOutputStream()
        val authCheckError = ByteArrayOutputStream()

        val authCheckResult = providers
            .exec {
            commandLine("firebase", "projects:list")
            standardOutput = authCheckOutput
            errorOutput = authCheckError
            isIgnoreExitValue = true
        }.result
            .get()

        println("Firebase auth check output:\n$authCheckOutput")
        println("Firebase auth check error:\n$authCheckError")

        if (authCheckResult.exitValue != 0) {
            throw GradleException(
                """
        Firebase CLI Authentication Error detected.
        Please run:
          firebase login --reauth
        or for CI environments:
          firebase login:ci
        to renew your credentials.
    """.trimIndent(),
            )
        }

        // Validate build type
        if (buildType !in listOf("debug", "release")) {
            throw GradleException("Unknown build type: $buildType. Supported: debug, release")
        }

        // Validate tester group
        val validGroups = listOf("Internal", "R Systems", "Internal,R Systems")
        if (testGroup !in validGroups) {
            throw GradleException("Unknown tester group: $testGroup. Supported: ${validGroups.joinToString(", ")}")
        }

        val gradleTask = when (buildType) {
            "release" -> "assembleRelease"
            else -> "assembleDebug"
        }

        val apkPath = when (buildType) {
            "release" -> "build/outputs/apk/release/app-release.apk"
            else -> "build/outputs/apk/debug/app-debug.apk"
        }

        println("📦 Build Type: $buildType")
        println("⚙ Gradle Task: $gradleTask")
        println("📄 APK Path: $apkPath")

        // Update versionCode and versionName
        println("🔧 Updating versionCode and versionName...")

        val lines = buildGradleFile.readLines().toMutableList()
        var versionCodeIndex = -1
        var versionNameIndex = -1
        var versionCode: Int? = null
        var versionName: String? = null

        val versionCodeRegex = Regex("""versionCode\s*=\s*(\d+)""")
        val versionNameRegex = Regex("""versionName\s*=\s*"(.*?)"""")

        lines.forEachIndexed { idx, line ->
            versionCodeRegex.find(line)?.let { match ->
                versionCode = match.groupValues[1].toInt()
                versionCodeIndex = idx
            }
            versionNameRegex.find(line)?.let { match ->
                versionName = match.groupValues[1]
                versionNameIndex = idx
            }
        }

        if (versionCode == null || versionName == null) {
            throw GradleException("Failed to locate versionCode or versionName in build.gradle.kts")
        }

        val newVersionCode = versionCode!! + 1

        val parts = versionName!!.split(".").map { it.toInt() }.toMutableList()
        while (parts.size < 3) {
            parts.add(0)
        }
        parts[2] += 1
        val newVersionName = parts.joinToString(".")

        lines[versionCodeIndex] = "        versionCode = $newVersionCode"
        lines[versionNameIndex] = "        versionName = \"$newVersionName\""
        buildGradleFile.writeText(lines.joinToString("\n"))

        println("✅ Bumped to versionCode=$newVersionCode, versionName=$newVersionName")

        // Run Gradle build task
        println("🔨 Building APK...")
        providers
            .exec {
            commandLine("./gradlew", gradleTask)
            workingDir = project.rootDir
        }.result
            .get()

        val apkFile = file(apkPath)
        if (!apkFile.exists()) {
            throw GradleException("APK not found at path: $apkPath")
        }

        // Ensure release notes
        if (!releaseNotesFile.exists()) {
            releaseNotesFile.writeText("Auto-generated release for production $buildType $newVersionName")
        }

        // Check Firebase CLI
        val checkFirebase = ByteArrayOutputStream()
        val firebaseCheckResult = providers
            .exec {
            commandLine("firebase", "--version")
            standardOutput = checkFirebase
            isIgnoreExitValue = true
        }.result
            .get()
        if (firebaseCheckResult.exitValue != 0) {
            throw GradleException("Firebase CLI not found. Install it from https://firebase.google.com/docs/cli")
        }

        // Upload to Firebase
        println("🚀 Uploading APK to Firebase App Distribution...")
        providers
            .exec {
            commandLine(
                "firebase", "appdistribution:distribute", apkPath,
                "--release-notes-file", releaseNotesFile.absolutePath,
                "--app", firebaseAppId,
                "--groups", testGroup,
            )
        }.result
            .get()

        println("🎉 Firebase distribution complete.")
    }
}
