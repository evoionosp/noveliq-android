package org.evoionosp.noveliq.data.server.remote.dto

import com.google.gson.annotations.SerializedName

data class StatusResponseDto(
    @SerializedName("app") val app: String? = null,
    @SerializedName("serverVersion") val serverVersion: String? = null,
    @SerializedName("isInit") val isInit: Boolean? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("authMethods") val authMethods: List<String>? = null,
    @SerializedName("authFormData") val authFormData: AuthFormDataDto? = null
)

data class AuthFormDataDto(
    @SerializedName("authLoginCustomMessage") val authLoginCustomMessage: String? = null
)
