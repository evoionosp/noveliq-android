package org.evoionosp.noveliq.data.server.remote.dto

import com.google.gson.annotations.SerializedName

data class ServerPingResponseDto(
    @SerializedName("success") val success: Boolean
)
