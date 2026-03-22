package org.evoionosp.noveliq.data.auth.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginResponseDto(
    @SerializedName(value = "access_token", alternate = ["accessToken", "token"])
    val accessToken: String? = null,
    @SerializedName(value = "refresh_token", alternate = ["refreshToken"])
    val refreshToken: String? = null,
    @SerializedName(value = "user_id", alternate = ["userId", "id"])
    val userId: String? = null
)
