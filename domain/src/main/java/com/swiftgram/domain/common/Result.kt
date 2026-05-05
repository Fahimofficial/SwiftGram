package com.swiftgram.domain.common

/**
 * Sealed class representing the result of an operation.
 * Can be either Success with data, or Failure with an exception.
 *
 * This provides a type-safe way to handle both successful and failed operations
 * without using exceptions for control flow.
 *
 * Usage:
 * ```
 * when (result) {
 *     is Result.Success -> {
 *         val data = result.data
 *         // handle success
 *     }
 *     is Result.Failure -> {
 *         val error = result.exception.message
 *         // handle error
 *     }
 * }
 * ```
 */
sealed class Result<out T> {
    
    /**
     * Operation succeeded with data.
     *
     * @param data The successful result
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Operation failed with an exception.
     *
     * @param exception The exception that caused the failure
     */
    data class Failure(val exception: Exception) : Result<Nothing>()
    
    /**
     * Get the data if successful, or null if failed.
     *
     * @return The data if Success, null if Failure
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
    
    /**
     * Get the exception if failed, or null if successful.
     *
     * @return The exception if Failure, null if Success
     */
    fun exceptionOrNull(): Exception? = when (this) {
        is Success -> null
        is Failure -> exception
    }
    
    /**
     * Transform the success data using the provided function.
     * If this is a Failure, returns the Failure unchanged.
     *
     * @param transform Function to transform the success data
     * @return Result with transformed data or unchanged Failure
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }
    
    /**
     * Apply a side effect for successful results.
     * If this is a Failure, does nothing.
     *
     * @param action Function to apply to the success data
     * @return This result unchanged
     */
    inline fun onEach(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }
    
    /**
     * Apply a side effect for failed results.
     * If this is a Success, does nothing.
     *
     * @param action Function to apply to the exception
     * @return This result unchanged
     */
    inline fun onError(action: (Exception) -> Unit): Result<T> {
        if (this is Failure) {
            action(exception)
        }
        return this
    }
    
    /**
     * Get the data or throw the exception if failed.
     *
     * @return The data if Success
     * @throws Exception if Failure
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw exception
    }
}
