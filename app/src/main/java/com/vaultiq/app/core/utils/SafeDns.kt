
package com.vaultiq.app.core.utils

import okhttp3.Dns
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

object SafeDns : Dns {

    override fun lookup(hostname: String): List<InetAddress> {

        val addresses = Dns.SYSTEM.lookup(hostname)

        if (addresses.isEmpty()) {
            throw UnknownHostException(
                "No IP addresses found for this host."
            )
        }

        if (addresses.any { isBlockedAddress(it) }) {
            throw UnknownHostException(
                "Connection to local or reserved network addresses is blocked."
            )
        }

        return addresses
    }

    private fun isBlockedAddress(
        address: InetAddress
    ): Boolean {

        if (
            address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) {
            return true
        }

        val bytes = address.address

        if (address is Inet6Address) {

            // Block IPv6 unique-local addresses (fc00::/7).
            if (
                (bytes[0].toInt() and 0xFE) ==
                0xFC
            ) {
                return true
            }

            // Check IPv4-mapped IPv6 addresses.
            val isMapped =
                bytes.take(10).all { it == 0.toByte() } &&
                bytes[10] == 0xFF.toByte() &&
                bytes[11] == 0xFF.toByte()

            if (isMapped) {
                return isBlockedIpv4(bytes.takeLast(4))
            }

            return false
        }

        if (address is Inet4Address) {
            return isBlockedIpv4(bytes.toList())
        }

        return true
    }

    private fun isBlockedIpv4(
        bytes: List<Byte>
    ): Boolean {

        if (bytes.size != 4) return true

        val parts = bytes.map {
            it.toInt() and 0xFF
        }

        val a = parts[0]
        val b = parts[1]
        val c = parts[2]

        return when {
            a == 0 -> true
            a == 10 -> true
            a == 127 -> true
            a >= 224 -> true

            a == 100 && b in 64..127 -> true

            a == 169 && b == 254 -> true

            a == 172 && b in 16..31 -> true

            a == 192 && b == 0 -> true
            a == 192 && b == 168 -> true
            a == 192 && b == 0 && c == 2 -> true

            a == 198 && b in 18..19 -> true
            a == 198 && b == 51 && c == 100 -> true

            a == 203 && b == 0 && c == 113 -> true

            a == 192 && b == 88 && c == 99 -> true

            else -> false
        }
    }
}
