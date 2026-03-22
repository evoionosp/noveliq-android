package org.evoionosp.noveliq.domain.server.usecase

import org.evoionosp.noveliq.domain.server.ServerRepository
import org.evoionosp.noveliq.domain.server.model.ServerResult
import org.evoionosp.noveliq.domain.server.model.ServerStatus

class CheckServerUseCase(
    private val repository: ServerRepository
) {
    suspend operator fun invoke(baseUrl: String): ServerResult<ServerStatus> {
        return when (val pingResult = repository.ping(baseUrl)) {
            is ServerResult.Failure -> pingResult
            is ServerResult.Success -> {
                if (!pingResult.data) {
                    ServerResult.Failure("Ping failed. Please check the server URL.")
                } else {
                    repository.getStatus(baseUrl)
                }
            }
        }
    }
}
