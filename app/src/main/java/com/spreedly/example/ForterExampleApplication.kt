package com.spreedly.example

import android.app.Application
import android.util.Log

/**
 * Example Application class showing optional direct Forter SDK usage for advanced scenarios.
 *
 * **Note:** The Spreedly SDK initializes Forter internally when `forterSiteId` is provided
 * in `SpreedlySDKInitOptions`. This class is only needed for advanced use cases where
 * direct Forter SDK access is required (e.g., custom tracking).
 *
 * ## Important
 *
 * - For standard 3DS, pass `forterSiteId` to `SpreedlySDKInitOptions` — no Application class needed
 * - This example is for advanced scenarios requiring direct Forter SDK interaction
 *
 * ## Forter SDK Dependency
 *
 * The Forter 3DS SDK (`com.forter.mobile:forter3ds:2.0.4`) is included as a transitive
 * dependency of `checkout-payments-core`. No explicit dependency is needed.
 *
 * ## References
 *
 * - [Forter Android Installation Guide](https://assets.ctfassets.net/sdx4pteldsvw/5yTZe9SK9IgvpvWkcozzBk/60bfd3137de955031cec9e995b919734/Installation_on_Android_Studio____android_documentation.pdf)
 * - [Forter Application Class Setup](https://assets.ctfassets.net/sdx4pteldsvw/1ydpbFFlmZ3slbQWUtMk53/4b3f4ef77db0d5ea7ae2b4c5907cb29b/Android_Application_Class____android_documentation.pdf)
 *
 * @see com.spreedly.sdk.Spreedly for Spreedly SDK initialization
 */
class ForterExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Forter SDK for 3DS support
        initializeForterSDK()
    }

    /**
     * Initialize Forter SDK for 3DS authentication.
     *
     * This method demonstrates the required Forter SDK setup. When Forter SDK
     * dependency is added, uncomment the implementation below.
     */
    private fun initializeForterSDK() {
        try {
            Log.i("ForterExample", "Initializing Forter SDK for 3DS support")

            // TODO: Uncomment when Forter SDK dependency is added
            // Step 1: Get Forter SDK instance
            // val forterSDK = com.forter.mobile.sdk.ForterSDK.getInstance()

            // Step 2: Get unique device identifier
            // val mobileId = com.forter.mobile.sdk.ForterIntegrationUtils.getDeviceUID(this)

            // Step 3: Initialize with your Forter Site ID
            // Get Site ID from: https://portal.forter.com/app/integration/credentials/
            // val forterSiteId = BuildConfig.FORTER_SITE_ID  // Store in BuildConfig or apikeys.properties
            // forterSDK.init(this, forterSiteId, mobileId)

            // Step 4: Register activity lifecycle callbacks (REQUIRED)
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            //     registerActivityLifecycleCallbacks(forterSDK.activityLifecycleCallbacks)
            // }

            // Step 5: Track app active (RECOMMENDED)
            // forterSDK.trackAction(com.forter.mobile.sdk.TrackType.APP_ACTIVE)

            // Step 6: Enable verbose logging in DEBUG builds
            // if (BuildConfig.DEBUG) {
            //     forterSDK.setLogLevel(com.forter.mobile.sdk.ForterLogLevel.VERBOSE)
            // }

            Log.i("ForterExample", "Forter SDK initialization placeholder - add dependency to enable")
        } catch (e: Exception) {
            // Handle initialization errors gracefully
            Log.i("ForterExample", "Forter SDK initialization skipped: ${e::class.simpleName}")
            // App can continue without 3DS support
        }
    }
}
