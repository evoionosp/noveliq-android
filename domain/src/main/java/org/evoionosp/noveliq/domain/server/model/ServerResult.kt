package org.evoionosp.noveliq.domain.server.model

sealed interface ServerResult<out T> {
    data class Success<T>(val data: T) : ServerResult<T>
    data class Failure(val error: ServerError, val code: Int? = null) : ServerResult<Nothing>
}
