package com.example.lockin.util

object PasswordManager {
    private const val HARD_CODED_PASSWORD = "X7pL9qW3zT2mK8nJ4vR6yF5bH1cD0eG2aJ8kM3nP7qW9rT4vY6zB1"

    fun verifyPassword(input: String): Boolean {
        return input == HARD_CODED_PASSWORD
    }
}