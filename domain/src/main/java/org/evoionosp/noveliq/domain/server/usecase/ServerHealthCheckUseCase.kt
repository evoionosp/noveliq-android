package org.evoionosp.noveliq.domain.server.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.repository.ServerRepository

class ServerHealthCheckUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
    ) {
        suspend operator fun invoke(baseUrl: String): ServerCheckResult<Boolean> = repository.healthCheck(baseUrl)
    }
