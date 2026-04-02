package org.evoionosp.noveliq.data.library.remote.dto

import com.google.gson.annotations.SerializedName

data class LibraryItemsResponseDto(
    @SerializedName(value = "results", alternate = ["libraryItems", "items"])
    val results: List<LibraryItemDto>? = null
)

data class LibraryItemDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("libraryId")
    val libraryId: String? = null,
    @SerializedName("mediaType")
    val mediaType: String? = null,
    @SerializedName("media")
    val media: LibraryItemMediaDto? = null
)

data class LibraryItemMediaDto(
    @SerializedName("duration")
    val durationInSeconds: Float? = null,
    @SerializedName("chapters")
    val chapters: List<ChapterDto>? = null,
    @SerializedName("metadata")
    val metadata: LibraryItemMetadataDto? = null
)

data class LibraryItemMetadataDto(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName(value = "authorName", alternate = ["author", "authorNameLF"])
    val authorName: String? = null,
    @SerializedName("series")
    val series: List<SeriesDto>? = null
)

data class SeriesDto(
    @SerializedName("name")
    val name: String? = null
)

data class ChapterDto(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName(value = "start", alternate = ["startTime", "startOffset"])
    val startInSeconds: Float? = null,
    @SerializedName(value = "end", alternate = ["endTime", "endOffset"])
    val endInSeconds: Float? = null
)
