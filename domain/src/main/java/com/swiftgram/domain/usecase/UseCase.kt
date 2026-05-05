package com.swiftgram.domain.usecase

import com.swiftgram.domain.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base class for all use cases (interactors).
 * Provides a consistent pattern for executing domain logic.
 *
 * Use cases encapsulate business logic and are the primary way the UI layer
 * interacts with the domain layer. Each use case should handle a single,
 * well-defined operation.
 *
 * Usage:
 * ```
 * class MyUseCase @Inject constructor(
 *     private val repository: MyRepository
 * ) : UseCase<InputParams, OutputData>() {
 *     override suspend fun execute(params: InputParams): OutputData {
 *         return repository.doSomething(params)
 *     }
 * }
 *
 * // In ViewModel
 * val result = myUseCase(MyUseCase.Params(...))
 * when (result) {
 *     is Result.Success -> { /* handle success */ }
 *     is Result.Failure -> { /* handle error */ }
 * }
 * ```
 */
abstract class UseCase<in Input, out Output> {
    
    /**
     * Execute the use case with the given input.
     * Implement this in subclasses with the actual business logic.
     *
     * @param params Input parameters for the use case
     * @return The output of the use case
     * @throws Exception if the operation fails
     */
    protected abstract suspend fun execute(params: Input): Output
    
    /**
     * Invoke the use case, handling coroutine dispatching and error wrapping.
     * This is the primary way to execute a use case.
     *
     * Execution happens on Dispatchers.Default to avoid blocking the main thread.
     * Exceptions are caught and wrapped in Result.Failure.
     *
     * @param params Input parameters
     * @return Result containing the output or exception
     */
    suspend operator fun invoke(params: Input): Result<Output> = withContext(Dispatchers.Default) {
        try {
            Result.Success(execute(params))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

/**
 * Base class for use cases that don't require input parameters.
 *
 * Usage:
 * ```
 * class GetCurrentUserUseCase @Inject constructor(
 *     private val repository: UserRepository
 * ) : NoParamUseCase<User>() {
 *     override suspend fun execute(): User {
 *         return repository.getCurrentUser()
 *     }
 * }
 *
 * // In ViewModel
 * val result = getCurrentUserUseCase()
 * ```
 */
abstract class NoParamUseCase<out Output> {
    
    /**
     * Execute the use case without parameters.
     *
     * @return The output of the use case
     * @throws Exception if the operation fails
     */
    protected abstract suspend fun execute(): Output
    
    /**
     * Invoke the use case without parameters.
     *
     * @return Result containing the output or exception
     */
    suspend operator fun invoke(): Result<Output> = withContext(Dispatchers.Default) {
        try {
            Result.Success(execute())
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}
