package com.swiftgram.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**\n * Hilt module for providing use cases.\n * Use cases are provided as singletons since they're stateless.\n *\n * Note: Individual use cases are automatically provided by Hilt\n * when they have @Inject constructors. This module is a placeholder\n * for any future use case configuration or bindings.\n *\n * Usage:\n * ```\n * @Inject\n * lateinit var sendPhoneNumberUseCase: SendPhoneNumberUseCase\n * ```\n */\n@Module\n@InstallIn(SingletonComponent::class)\nobject UseCaseModule {\n    // Use cases are automatically provided via their @Inject constructors\n    // No explicit bindings needed at this time\n}\n
