package org.evoionosp.noveliq.domain.server.model

sealed interface ServerCheckResult<out T> {
    data class Success<T>(val data: T) : ServerCheckResult<T>
    data class Failure(val error: ServerError, val code: Int? = null) : ServerCheckResult<Nothing>
}
