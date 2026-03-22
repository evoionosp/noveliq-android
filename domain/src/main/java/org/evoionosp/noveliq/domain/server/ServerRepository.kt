package org.evoionosp.noveliq.domain.server

import org.evoionosp.noveliq.domain.server.model.ServerResult
import org.evoionosp.noveliq.domain.server.model.ServerStatus

interface ServerRepository {
    suspend fun ping(baseUrl: String): ServerResult<Boolean>

    suspend fun getStatus(baseUrl: String): ServerResult<ServerStatus>

    suspend fun healthCheck(baseUrl: String): ServerResult<Boolean>
}
