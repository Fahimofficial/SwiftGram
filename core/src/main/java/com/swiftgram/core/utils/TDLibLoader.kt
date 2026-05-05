package com.swiftgram.core.utils

import android.content.Context
import timber.log.Timber

/**
 * Utility for loading TDLib native libraries.
 * Handles the loading of libtdjson.so and manages JNI initialization.
 *
 * Usage:
 * ```
 * try {
 *     TDLibLoader.loadLibraries(context)
 * } catch (e: Exception) {
 *     Timber.e(e, "Failed to load TDLib")
 * }
 * ```
 */
object TDLibLoader {
    private const val TAG = "TDLibLoader"
    private var isLoaded = false

    /**
     * Load TDLib native libraries.
     * Must be called once during app initialization, preferably in Application.onCreate() or MainActivity.onCreate().
     *
     * @param context Android context (used for logging purposes)
     * @throws UnsatisfiedLinkError if native libraries cannot be found or loaded
     * @throws RuntimeException if an unexpected error occurs during loading
     */
    @Synchronized
    fun loadLibraries(context: Context) {
        if (isLoaded) {
            Timber.d(TAG, "TDLib libraries already loaded")
            return
        }

        try {
            // Load the C++ runtime library first (required dependency)
            System.loadLibrary("c++_shared")
            Timber.d(TAG, "Loaded libc++_shared.so")

            // Load the main TDLib library
            System.loadLibrary("tdjson")
            Timber.d(TAG, "Loaded libtdjson.so")

            isLoaded = true
            Timber.i(TAG, "TDLib native libraries loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Timber.e(TAG, "Failed to load TDLib native libraries: ${e.message}")
            throw RuntimeException("Failed to load TDLib: ${e.message}", e)
        } catch (e: Exception) {
            Timber.e(TAG, "Unexpected error loading TDLib: ${e.message}")
            throw RuntimeException("Unexpected error loading TDLib: ${e.message}", e)
        }
    }

    /**
     * Check if TDLib libraries are already loaded.
     *
     * @return true if libraries are loaded, false otherwise
     */
    fun isLibrariesLoaded(): Boolean = isLoaded
}
