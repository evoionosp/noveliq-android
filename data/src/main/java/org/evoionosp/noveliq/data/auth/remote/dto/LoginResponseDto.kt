package org.evoionosp.noveliq.data.auth.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginResponseDto(
    @SerializedName(value = "access_token", alternate = ["accessToken", "token"])
    val accessToken: String? = null,
    @SerializedName(value = "refresh_token", alternate = ["refreshToken"])
    val refreshToken: String? = null,
    @SerializedName(value = "user_id", alternate = ["userId", "id"])
    val userId: String? = null,
    @SerializedName("user")
    val user: UserDto? = null
) {
    data class UserDto(
        @SerializedName(value = "token", alternate = ["accessToken", "access_token"])
        val token: String? = null,
        @SerializedName(value = "id", alternate = ["userId"])
        val id: String? = null,
        @SerializedName(value = "username", alternate = ["userName"])
        val username: String? = null
    )
}
