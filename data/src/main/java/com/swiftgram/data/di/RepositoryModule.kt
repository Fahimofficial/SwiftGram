package com.swiftgram.data.di

import com.swiftgram.data.repository.TelegramRepositoryImpl
import com.swiftgram.domain.repository.TelegramRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations.
 * This module binds the TelegramRepository interface to its implementation.
 *
 * Usage:
 * ```
 * @Inject
 * lateinit var telegramRepository: TelegramRepository
 * ```
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Bind TelegramRepository interface to TelegramRepositoryImpl.
     * Hilt will automatically inject TelegramRepositoryImpl wherever TelegramRepository is needed.
     *
     * @param impl The implementation
     * @return The interface type for injection
     */
    @Binds
    @Singleton
    abstract fun bindTelegramRepository(
        impl: TelegramRepositoryImpl
    ): TelegramRepository
}
