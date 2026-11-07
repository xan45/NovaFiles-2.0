package com.novafiles.core.common

/**
 * Functional result wrapper representing the state of an asynchronous or domain operation.
 */
sealed interface NovaResult<out T> {
    data class Success<out T>(val data: T) : NovaResult<T>
    data class Error(val throwable: Throwable, val message: String? = throwable.localizedMessage) : NovaResult<Nothing>
    data object Loading : NovaResult<Nothing>
}

inline fun <T, R> NovaResult<T>.map(transform: (T) -> R): NovaResult<R> = when (this) {
    is NovaResult.Success -> NovaResult.Success(transform(data))
    is NovaResult.Error -> this
    is NovaResult.Loading -> this
}

inline fun <T> NovaResult<T>.onSuccess(action: (T) -> Unit): NovaResult<T> {
    if (this is NovaResult.Success) action(data)
    return this
}

inline fun <T> NovaResult<T>.onError(action: (Throwable, String?) -> Unit): NovaResult<T> {
    if (this is NovaResult.Error) action(throwable, message)
    return this
}
