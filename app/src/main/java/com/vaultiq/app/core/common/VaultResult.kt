
package com.vaultiq.app.core.common

sealed interface VaultResult<out T> {

    data class Success<T>(
        val data: T
    ) : VaultResult<T>

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : VaultResult<Nothing>

    data object Loading : VaultResult<Nothing>
}
