package org.evoionosp.noveliq.data.library.remote.api

import org.evoionosp.noveliq.data.library.remote.dto.LibraryItemsResponseDto
import org.evoionosp.noveliq.data.library.remote.dto.LibrariesResponseDto
import retrofit2.http.GET
import retrofit2.http.Header
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
}
