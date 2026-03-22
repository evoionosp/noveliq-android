package org.evoionosp.noveliq.domain.server.model

sealed interface ServerResult<out T> {
    data class Success<T>(val data: T) : ServerResult<T>
    data class Failure(val message: String, val code: Int? = null) : ServerResult<Nothing>
}
