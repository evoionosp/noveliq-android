package org.evoionosp.noveliq.domain.server.usecase

import org.evoionosp.noveliq.domain.server.ServerRepository
import org.evoionosp.noveliq.domain.server.model.ServerResult
import javax.inject.Inject

class HealthCheckUseCase @Inject constructor(
    private val repository: ServerRepository
) {
    suspend operator fun invoke(baseUrl: String): ServerResult<Boolean> {
        return repository.healthCheck(baseUrl)
    }
}
