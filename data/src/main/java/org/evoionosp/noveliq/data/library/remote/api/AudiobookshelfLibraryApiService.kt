package org.evoionosp.noveliq.data.library.remote.api

import org.evoionosp.noveliq.data.library.remote.dto.LibraryItemsResponseDto
import org.evoionosp.noveliq.data.library.remote.dto.LibrariesResponseDto
import org.evoionosp.noveliq.data.library.remote.dto.MediaProgressDto
import org.evoionosp.noveliq.data.library.remote.dto.PersonalizedShelfDto
import org.evoionosp.noveliq.data.library.remote.dto.UpdateProgressRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface AudiobookshelfLibraryApiService {
    @GET("api/libraries")
    suspend fun libraries(
        @Header("Authorization") authorization: String
    ): LibrariesResponseDto

    @GET("api/libraries/{libraryId}/items")
    suspend fun libraryItems(
        @Header("Authorization") authorization: String,
        @Path("libraryId") libraryId: String,
        @Query("limit") limit: Int = 0,
        @Query("page") page: Int = 0,
        @Query("minified") minified: Int = 1,
        @Query("collapseseries") collapseSeries: Int = 0
    ): LibraryItemsResponseDto

    @GET("api/items/{itemId}")
    suspend fun item(
        @Header("Authorization") authorization: String,
        @Path("itemId") itemId: String,
        @Query("expanded") expanded: Int = 1
    ): org.evoionosp.noveliq.data.library.remote.dto.LibraryItemDto

    @GET("api/libraries/{libraryId}/personalized")
    suspend fun personalized(
        @Header("Authorization") authorization: String,
        @Path("libraryId") libraryId: String
    ): List<PersonalizedShelfDto>

    @GET("api/me/items-in-progress")
    suspend fun itemsInProgress(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 50
    ): LibraryItemsResponseDto

    @GET("api/me/progress/{itemId}")
    suspend fun mediaProgress(
        @Header("Authorization") authorization: String,
        @Path("itemId") itemId: String
    ): MediaProgressDto

    @PATCH("api/me/progress/{itemId}")
    suspend fun updateMediaProgress(
        @Header("Authorization") authorization: String,
        @Path("itemId") itemId: String,
        @Body body: UpdateProgressRequestDto
    ): Response<Unit>
}
