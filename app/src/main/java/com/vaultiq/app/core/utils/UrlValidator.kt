
package com.vaultiq.app.core.utils

import com.vaultiq.app.core.common.VaultResult
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

object UrlValidator {

    fun validate(rawUrl: String): VaultResult<URI> {

        val input = rawUrl.trim()

        if (input.isBlank()) {
            return VaultResult.Error("URL cannot be empty.")
        }

        if (input.any { it.isWhitespace() }) {
            return VaultResult.Error(
                "URL must not contain whitespace."
            )
        }

        val uri = try {
            URI(input)
        } catch (exception: URISyntaxException) {
            return VaultResult.Error(
                "Invalid URL format.",
                exception
            )
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)

        if (scheme != "http" && scheme != "https") {
            return VaultResult.Error(
                "Only HTTP and HTTPS URLs are supported."
            )
        }

        if (uri.host.isNullOrBlank()) {
            return VaultResult.Error(
                "URL must contain a valid host."
            )
        }

        if (uri.rawUserInfo != null) {
            return VaultResult.Error(
                "URLs containing embedded credentials are not supported."
            )
        }

        if (uri.port !in -1..65535) {
            return VaultResult.Error(
                "Invalid URL port."
            )
        }

        return VaultResult.Success(uri)
    }
}
