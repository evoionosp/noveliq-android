package org.evoionosp.noveliq.data.server.remote.dto

import com.google.gson.annotations.SerializedName

data class PingResponseDto(
    @SerializedName("success") val success: Boolean
)
