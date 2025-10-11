package com.workoutapp.data.remote.strava

import retrofit2.Response
import retrofit2.http.*

/**
 * Strava API interface for OAuth and activity management
 */
interface StravaApi {

    /**
     * Exchange authorization code for access tokens
     * POST /oauth/token
     */
    @POST("oauth/token")
    @FormUrlEncoded
    suspend fun exchangeCodeForTokens(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code"
    ): Response<StravaTokenResponse>

    /**
     * Refresh access token using refresh token
     * POST /oauth/token
     */
    @POST("oauth/token")
    @FormUrlEncoded
    suspend fun refreshAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Response<StravaTokenResponse>

    /**
     * Deauthorize application (revoke access)
     * POST /oauth/deauthorize
     */
    @POST("oauth/deauthorize")
    suspend fun deauthorize(
        @Header("Authorization") accessToken: String
    ): Response<Unit>

    /**
     * Create a new activity
     * POST /api/v3/activities
     */
    @POST("api/v3/activities")
    @Headers("Content-Type: application/json")
    suspend fun createActivity(
        @Header("Authorization") accessToken: String,
        @Body activity: StravaActivityRequest
    ): Response<StravaActivityResponse>

    /**
     * Update an existing activity
     * PUT /api/v3/activities/{id}
     */
    @PUT("api/v3/activities/{id}")
    @Headers("Content-Type: application/json")
    suspend fun updateActivity(
        @Header("Authorization") accessToken: String,
        @Path("id") activityId: Long,
        @Body activity: StravaActivityUpdateRequest
    ): Response<StravaActivityResponse>

    /**
     * Get activity details
     * GET /api/v3/activities/{id}
     */
    @GET("api/v3/activities/{id}")
    suspend fun getActivity(
        @Header("Authorization") accessToken: String,
        @Path("id") activityId: Long
    ): Response<StravaActivityResponse>

    /**
     * Delete an activity
     * DELETE /api/v3/activities/{id}
     */
    @DELETE("api/v3/activities/{id}")
    suspend fun deleteActivity(
        @Header("Authorization") accessToken: String,
        @Path("id") activityId: Long
    ): Response<Unit>

    /**
     * Get authenticated athlete info
     * GET /api/v3/athlete
     */
    @GET("api/v3/athlete")
    suspend fun getAthlete(
        @Header("Authorization") accessToken: String
    ): Response<StravaAthlete>
}
