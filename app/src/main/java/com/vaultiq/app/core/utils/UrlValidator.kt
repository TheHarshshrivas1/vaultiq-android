
package com.vaultiq.app.core.utils

import com.vaultiq.app.core.common.VaultResult
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
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

        val host = uri.host
            ?.lowercase(Locale.ROOT)
            ?.removeSurrounding("[", "]")
            ?: return VaultResult.Error(
                "URL must contain a valid host."
            )

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

        if (isBlockedHost(host)) {
            return VaultResult.Error(
                "Local and private network addresses are not supported."
            )
        }

        return VaultResult.Success(uri)
    }

    private fun isBlockedHost(host: String): Boolean {

        if (
            host == "localhost" ||
            host.endsWith(".localhost") ||
            host.endsWith(".local") ||
            host.endsWith(".internal") ||
            host.endsWith(".test")
        ) {
            return true
        }

        if (host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) {
            return isBlockedIpv4(host)
        }

        if (host.contains(":")) {
            return isBlockedIpv6(host)
        }

        return false
    }

    private fun isBlockedIpv4(host: String): Boolean {

        val parts = host.split(".").mapNotNull {
            it.toIntOrNull()
        }

        if (
            parts.size != 4 ||
            parts.any { it !in 0..255 }
        ) {
            return true
        }

        val a = parts[0]
        val b = parts[1]

        return when {
            a == 0 -> true
            a == 10 -> true
            a == 127 -> true
            a >= 224 -> true
            a == 100 && b in 64..127 -> true
            a == 169 && b == 254 -> true
            a == 172 && b in 16..31 -> true
            a == 192 && b == 168 -> true
            a == 192 && b == 0 -> true
            a == 198 && b in 18..19 -> true
            a == 255 && b == 255 -> true
            else -> false
        }
    }

    private fun isBlockedIpv6(host: String): Boolean {

        return try {
            val address = InetAddress.getByName(host)

            if (address !is Inet6Address) {
                true
            } else {
                address.isAnyLocalAddress ||
                    address.isLoopbackAddress ||
                    address.isLinkLocalAddress ||
                    address.isSiteLocalAddress ||
                    address.isMulticastAddress ||
                    isIpv4MappedPrivate(address)
            }
        } catch (exception: Exception) {
            true
        }
    }

    private fun isIpv4MappedPrivate(
        address: Inet6Address
    ): Boolean {

        val bytes = address.address

        val isMapped = bytes.take(10).all { it == 0.toByte() } &&
            bytes[10] == 0xFF.toByte() &&
            bytes[11] == 0xFF.toByte()

        if (!isMapped) return false

        val ipv4 = bytes.takeLast(4).joinToString(".") {
            (it.toInt() and 0xFF).toString()
        }

        return isBlockedIpv4(ipv4)
    }
}
