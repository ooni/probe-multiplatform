package org.ooni.engine.models

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

sealed class Result<out S, out F> {
    inline infix fun <S2> map(mapper: (S) -> S2): Result<S2, F> =
        when (this) {
            is Success -> Success(mapper(value))
            is Failure -> Failure(reason)
        }

    inline infix fun <F2> mapError(mapper: (F) -> F2): Result<S, F2> =
        when (this) {
            is Success -> Success(value)
            is Failure -> Failure(mapper(reason))
        }

    inline infix fun <S2> flatMap(mapper: (S) -> Result<S2, @UnsafeVariance F>): Result<S2, F> =
        when (this) {
            is Success -> mapper(value)
            is Failure -> Failure(reason)
        }

    inline infix fun onSuccess(action: (S) -> Unit): Result<S, F> {
        if (this is Success) {
            action(value)
        }
        return this
    }

    inline infix fun onFailure(action: (F) -> Unit): Result<S, F> {
        if (this is Failure) {
            action(reason)
        }
        return this
    }

    fun get(): S? = (this as? Success)?.value

    fun getOrThrow(): S = get() ?: throw IllegalStateException("Result is not successful")

    fun getError(): F? = (this as? Failure)?.reason
}

data class Success<out S>(val value: S) : Result<S, Nothing>()

data class Failure<out F>(val reason: F) : Result<Nothing, F>()

suspend fun <S> resultOf(
    coroutineDispatcher: CoroutineDispatcher? = null,
    action: suspend () -> S,
): Result<S, Throwable> =
    withContext(coroutineDispatcher ?: coroutineContext) {
        try {
            Success(action())
        } catch (throwable: Throwable) {
            Failure(throwable)
        }
    }
