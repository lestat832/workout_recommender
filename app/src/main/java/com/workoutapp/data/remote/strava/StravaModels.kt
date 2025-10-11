package com.workoutapp.data.remote.strava

import com.google.gson.annotations.SerializedName

/**
 * OAuth Token Exchange Request
 */
data class StravaTokenRequest(
    @SerializedName("client_id")
    val clientId: String,

    @SerializedName("client_secret")
    val clientSecret: String,

    @SerializedName("code")
    val code: String,

    @SerializedName("grant_type")
    val grantType: String = "authorization_code"
)

/**
 * OAuth Token Refresh Request
 */
data class StravaTokenRefreshRequest(
    @SerializedName("client_id")
    val clientId: String,

    @SerializedName("client_secret")
    val clientSecret: String,

    @SerializedName("refresh_token")
    val refreshToken: String,

    @SerializedName("grant_type")
    val grantType: String = "refresh_token"
)

/**
 * OAuth Token Response
 */
data class StravaTokenResponse(
    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("expires_at")
    val expiresAt: Long,

    @SerializedName("expires_in")
    val expiresIn: Int,

    @SerializedName("refresh_token")
    val refreshToken: String,

    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("athlete")
    val athlete: StravaAthlete
)

/**
 * Strava Athlete Info
 */
data class StravaAthlete(
    @SerializedName("id")
    val id: Long,

    @SerializedName("username")
    val username: String?,

    @SerializedName("firstname")
    val firstName: String?,

    @SerializedName("lastname")
    val lastName: String?,

    @SerializedName("profile")
    val profileImageUrl: String?
)

/**
 * Create Activity Request
 */
data class StravaActivityRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("type")
    val type: String = "WeightTraining",

    @SerializedName("sport_type")
    val sportType: String = "WeightTraining",

    @SerializedName("start_date_local")
    val startDateLocal: String,

    @SerializedName("elapsed_time")
    val elapsedTime: Int, // in seconds

    @SerializedName("description")
    val description: String,

    @SerializedName("trainer")
    val trainer: Boolean = false,

    @SerializedName("commute")
    val commute: Boolean = false
)

/**
 * Update Activity Request
 */
data class StravaActivityUpdateRequest(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("sport_type")
    val sportType: String? = null,

    @SerializedName("description")
    val description: String? = null
)

/**
 * Strava Activity Response
 */
data class StravaActivityResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("sport_type")
    val sportType: String,

    @SerializedName("start_date_local")
    val startDateLocal: String,

    @SerializedName("elapsed_time")
    val elapsedTime: Int,

    @SerializedName("description")
    val description: String?,

    @SerializedName("athlete")
    val athlete: StravaAthlete
)

/**
 * Strava Error Response
 */
data class StravaErrorResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("errors")
    val errors: List<StravaError>?
)

data class StravaError(
    @SerializedName("resource")
    val resource: String,

    @SerializedName("field")
    val field: String,

    @SerializedName("code")
    val code: String
)
