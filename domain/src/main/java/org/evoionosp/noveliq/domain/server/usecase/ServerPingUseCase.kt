package org.evoionosp.noveliq.domain.server.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.model.ServerError
import org.evoionosp.noveliq.domain.server.model.ServerStatus
import org.evoionosp.noveliq.domain.server.repository.ServerRepository

class ServerPingUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
    ) {
        suspend operator fun invoke(baseUrl: String): ServerCheckResult<ServerStatus> =
            when (val pingResult = repository.ping(baseUrl)) {
                is ServerCheckResult.Failure -> {
                    pingResult
                }

                is ServerCheckResult.Success -> {
                    if (!pingResult.data) {
                        ServerCheckResult.Failure(ServerError.PING_FAILED)
                    } else {
                        repository.getStatus(baseUrl)
                    }
                }
            }
    }
