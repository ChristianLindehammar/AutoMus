package com.lindehammarkonsult.automus.shared.utils

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resumeWithException
import com.google.common.util.concurrent.MoreExecutors

/**
 * Converts a ListenableFuture to a Kotlin coroutine suspend function.
 * This is a replacement for kotlinx-coroutines-guava await() function.
 */
suspend fun <T> ListenableFuture<T>.await(): T {
    // Fast path: if the future is already done, return the result or throw the exception
    if (isDone) {
        try {
            return get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }
    
    return suspendCancellableCoroutine { cont ->
        addListener({
            try {
                cont.resume(get()) { /* cancelled */ }
            } catch (e: ExecutionException) {
                cont.resumeWithException(e.cause ?: e)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }, MoreExecutors.directExecutor())
        
        cont.invokeOnCancellation {
            cancel(false)
        }
    }
}
