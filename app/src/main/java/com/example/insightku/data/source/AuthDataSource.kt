
package com.example.insightku.data.source

import kotlinx.coroutines.delay

/**
 * Simulates a remote data source for authentication.
 */
object AuthDataSource {

    suspend fun login(email: String, password: String): Result<Unit> {
        delay(1500) // Simulate network delay
        return if (email == "test@test.com" && password == "123456") {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid email or password. Please try again."))
        }
    }
}
