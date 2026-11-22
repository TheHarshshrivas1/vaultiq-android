
package com.vaultiq.app.core.utils

import com.vaultiq.app.core.common.VaultResult
import java.net.URI

object SharedUrlParser {

    private val urlPattern = Regex(
        """https?://\S+""",
        RegexOption.IGNORE_CASE
    )

    fun extractUrl(sharedText: String?): VaultResult<URI> {

        if (sharedText.isNullOrBlank()) {
            return VaultResult.Error(
                "No shared link was received."
            )
        }

        val candidate = urlPattern
            .find(sharedText)
            ?.value
            ?.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '}')
            ?: return VaultResult.Error(
                "No HTTP or HTTPS link found in shared content."
            )

        return UrlValidator.validate(candidate)
    }
}
