package org.evoionosp.noveliq.domain.server.usecase

import org.evoionosp.noveliq.domain.server.repository.ServerRepository
import org.evoionosp.noveliq.domain.server.model.ServerError
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.model.ServerStatus
import javax.inject.Inject

class ServerPingUseCase @Inject constructor(
    private val repository: ServerRepository
) {
    suspend operator fun invoke(baseUrl: String): ServerCheckResult<ServerStatus> {
        return when (val pingResult = repository.ping(baseUrl)) {
            is ServerCheckResult.Failure -> pingResult
            is ServerCheckResult.Success -> {
                if (!pingResult.data) {
                    ServerCheckResult.Failure(ServerError.PING_FAILED)
                } else {
                    repository.getStatus(baseUrl)
                }
            }
        }
    }
}
