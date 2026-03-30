package org.evoionosp.noveliq.data.library.remote.dto

import com.google.gson.annotations.SerializedName

data class LibrariesResponseDto(
    @SerializedName("libraries")
    val libraries: List<LibraryDto>? = null
)

data class LibraryDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("displayOrder")
    val displayOrder: Int? = null,
    @SerializedName("mediaType")
    val mediaType: String? = null
)
