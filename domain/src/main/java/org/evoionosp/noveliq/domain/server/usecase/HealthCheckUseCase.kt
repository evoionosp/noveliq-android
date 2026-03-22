package org.evoionosp.noveliq.domain.server.usecase

import org.evoionosp.noveliq.domain.server.ServerRepository
import org.evoionosp.noveliq.domain.server.model.ServerResult

class HealthCheckUseCase(
    private val repository: ServerRepository
) {
    suspend operator fun invoke(baseUrl: String): ServerResult<Boolean> {
        return repository.healthCheck(baseUrl)
    }
}
