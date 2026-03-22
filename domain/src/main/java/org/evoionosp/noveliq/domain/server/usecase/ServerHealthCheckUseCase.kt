package org.evoionosp.noveliq.domain.server.usecase

import org.evoionosp.noveliq.domain.server.repository.ServerRepository
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import javax.inject.Inject

class ServerHealthCheckUseCase @Inject constructor(
    private val repository: ServerRepository
) {
    suspend operator fun invoke(baseUrl: String): ServerCheckResult<Boolean> {
        return repository.healthCheck(baseUrl)
    }
}
