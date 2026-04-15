package org.evoionosp.noveliq.data.library.remote.dto

import com.google.gson.annotations.SerializedName

data class PersonalizedShelfDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("entities")
    val entities: List<LibraryItemDto>? = null
)
