package com.vaultiq.app.core.utils

data class PlatformLinkGuidance(
    val platform: String,
    val message: String,
    val officialUrl: String,
    val requiresManualImport: Boolean = true
)

object PlatformLinkGuidanceProvider {

    fun get(url: String): PlatformLinkGuidance? {
        val result = MediaSourceDetector.detect(url)

        val source = when (result) {
            is com.vaultiq.app.core.common.VaultResult.Success ->
                result.data.source

            else -> return null
        }

        return when (source) {
            MediaSource.YOUTUBE -> PlatformLinkGuidance(
                platform = "YouTube",
                message = "YouTube link detected. Use YouTube's " +
                    "official download options where available. " +
                    "For your own exported videos, import the saved file into VAULTIQ.",
                officialUrl = "https://www.youtube.com/"
            )

            MediaSource.INSTAGRAM -> PlatformLinkGuidance(
                platform = "Instagram",
                message = "Instagram link detected. Use Instagram's " +
                    "official content download or account export options. " +
                    "You can then import your saved media into VAULTIQ.",
                officialUrl = "https://www.instagram.com/"
            )

            else -> null
        }
    }
}
