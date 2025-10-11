package com.workoutapp.util

import com.workoutapp.BuildConfig

/**
 * Strava API configuration and constants
 * Credentials are loaded from BuildConfig (which reads from local.properties)
 */
object StravaConfig {

    /**
     * Strava OAuth Client ID
     */
    val CLIENT_ID: String
        get() = BuildConfig.STRAVA_CLIENT_ID

    /**
     * Strava OAuth Client Secret
     * NEVER log this in production!
     */
    val CLIENT_SECRET: String
        get() = BuildConfig.STRAVA_CLIENT_SECRET

    /**
     * OAuth redirect URI (using localhost as required by Strava)
     * The actual deep link is handled in AndroidManifest
     */
    const val REDIRECT_URI = "http://localhost/strava-oauth"

    /**
     * OAuth scopes required
     */
    const val OAUTH_SCOPE = "activity:write"

    /**
     * Strava API base URL
     */
    const val BASE_URL = "https://www.strava.com/"

    /**
     * Builds the Strava OAuth authorization URL
     */
    fun buildAuthUrl(): String {
        return "${BASE_URL}oauth/authorize?" +
                "client_id=$CLIENT_ID&" +
                "redirect_uri=$REDIRECT_URI&" +
                "response_type=code&" +
                "scope=$OAUTH_SCOPE"
    }

    /**
     * Verifies that credentials are configured
     */
    fun isConfigured(): Boolean {
        return CLIENT_ID.isNotBlank() && CLIENT_SECRET.isNotBlank()
    }
}
