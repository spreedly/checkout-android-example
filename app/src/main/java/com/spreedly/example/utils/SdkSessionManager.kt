package com.spreedly.example.utils

import android.content.Context
import com.spreedly.example.AuthService
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.SpreedlySDKInitOptions

/**
 * Single source of truth for SDK initialization flow.
 *
 * Fetches auth params from the backend and initializes the SDK.
 * Constructor-injected [AuthService] makes this fully testable with mocks.
 *
 * @param authService Backend API client for fetching auth params.
 */
class SdkSessionManager(
    private val authService: AuthService,
) {
    /**
     * Fetch remote auth params, build init options, and call [Spreedly.init].
     *
     * @param forterSiteId Optional Forter Site ID for 3DS authentication.
     * @return [Result.success] on successful init, [Result.failure] otherwise.
     */
    suspend fun initializeSdk(
        sdk: Spreedly,
        context: Context,
        environmentKey: String,
        forterSiteId: String? = null,
    ): kotlin.Result<Unit> {
        val authResult = authService.getAuthParams()

        return authResult.mapCatching { authParams ->
            val options = SpreedlySDKInitOptions(
                nonce = authParams.nonce,
                signature = authParams.signature,
                certificateToken = authParams.certificateToken,
                timestamp = authParams.timestamp.toString(),
                environmentKey = environmentKey,
                context = context,
                forterSiteId = forterSiteId,
            )
            sdk.init(options)
        }
    }
}
