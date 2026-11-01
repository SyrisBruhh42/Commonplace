package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.TailscaleConnectionStatus
import java.net.NetworkInterface
import java.util.Collections

/**
 * NetworkHealthService checks for active Tailscale interfaces (`tailscale*`, `tun*`, `utun*`, `wg*`),
 * Tailscale CGNAT (`100.64.0.0/10`) / ULA (`fd7a:115c:a1e0::/48`) addresses, or specific private VPN
 * profiles via [ConnectivityManager] (`TRANSPORT_VPN`) to detect if the user is in a private network.
 *
 * When a valid private network is detected, the UI conditionally displays and wires the
 * LFS Oasis Server configuration card in `FamilyCircleScreen`.
 */
class NetworkHealthService(
    private val context: Context? = null
) {
    companion object {
        private const val TAILSCALE_IPV6_PREFIX = "fd7a:115c:a1e0:"
        val RECOGNIZED_VPN_INTERFACE_PREFIXES = listOf(
            "tailscale",
            "ts",
            "tun",
            "utun",
            "wg",
            "oasis",
            "lfs"
        )
        val RECOGNIZED_PRIVATE_DNS_DOMAINS = listOf(
            "ts.net",
            "tailscale.net",
            "lewisfamilysystems.com"
        )

        /**
         * Returns true if [ipAddress] belongs to the Tailscale IPv4 CGNAT subnet `100.64.0.0/10`
         * (`100.64.0.0` through `100.127.255.255`) or the Tailscale IPv6 ULA prefix `fd7a:115c:a1e0::/48`.
         */
        fun isTailscaleIpAddress(ipAddress: String): Boolean {
            val clean = ipAddress.trim().substringBefore('%').lowercase()
            if (clean.startsWith(TAILSCALE_IPV6_PREFIX)) {
                return true
            }
            val parts = clean.split('.')
            if (parts.size == 4) {
                val octets = parts.map { it.toIntOrNull() ?: return false }
                if (octets.any { it !in 0..255 }) return false
                val first = octets[0]
                val second = octets[1]
                return first == 100 && second in 64..127
            }
            return false
        }

        /**
         * Returns true if [vpnProfileOrDomain] matches a recognized Tailscale or LFS Oasis VPN profile.
         */
        fun isRecognizedPrivateVpnProfile(vpnProfileOrDomain: String): Boolean {
            val clean = vpnProfileOrDomain.trim().lowercase()
            if (clean.isBlank()) return false
            return clean.contains("tailscale") ||
                clean.contains("oasis") ||
                clean.contains("lfs") ||
                clean.contains("wireguard") ||
                RECOGNIZED_PRIVATE_DNS_DOMAINS.any { clean.contains(it) }
        }

        /**
         * Inspects local network interfaces and optional VPN profile metadata to determine
         * whether the device is connected to a valid Tailscale mesh or private LFS VPN network.
         */
        fun detectPrivateNetwork(
            simulatedInterfaces: List<Pair<String, List<String>>>? = null,
            simulatedVpnProfile: String? = null,
            isVpnTransportActive: Boolean = false
        ): TailscaleConnectionStatus {
            val candidates: List<Pair<String, List<String>>> = simulatedInterfaces ?: try {
                val netIfs = NetworkInterface.getNetworkInterfaces()
                if (netIfs == null) {
                    emptyList()
                } else {
                    Collections.list(netIfs)
                        .filter { it.isUp && !it.isLoopback }
                        .map { nif ->
                            val addrs = Collections.list(nif.inetAddresses)
                                .mapNotNull { it.hostAddress }
                            nif.name to addrs
                        }
                }
            } catch (_: Exception) {
                emptyList()
            }

            // Priority 1: Explicit tailscale/tun/utun/wg interface carrying a Tailscale IP
            for ((ifName, addrs) in candidates) {
                val lowerName = ifName.lowercase()
                val isTunnelIf = RECOGNIZED_VPN_INTERFACE_PREFIXES.any { lowerName.startsWith(it) }
                val tsIp = addrs.firstOrNull { isTailscaleIpAddress(it) }
                if (isTunnelIf && tsIp != null) {
                    return TailscaleConnectionStatus(
                        isDetected = true,
                        tailnetIp = tsIp.substringBefore('%'),
                        interfaceName = ifName,
                        vpnProfileName = simulatedVpnProfile?.ifBlank { "Tailscale Mesh VPN" } ?: "Tailscale Mesh VPN",
                        isVpnTransportActive = true,
                        magicDnsActive = true
                    )
                }
            }

            // Priority 2: Any active interface carrying a verified Tailscale IP (100.64.0.0/10 or fd7a:115c:a1e0::/48)
            for ((ifName, addrs) in candidates) {
                val tsIp = addrs.firstOrNull { isTailscaleIpAddress(it) }
                if (tsIp != null) {
                    return TailscaleConnectionStatus(
                        isDetected = true,
                        tailnetIp = tsIp.substringBefore('%'),
                        interfaceName = ifName,
                        vpnProfileName = simulatedVpnProfile?.ifBlank { "Tailscale CGNAT" } ?: "Tailscale CGNAT",
                        isVpnTransportActive = isVpnTransportActive,
                        magicDnsActive = true
                    )
                }
            }

            // Priority 3: Recognized private VPN profile (e.g., Tailscale / Oasis / WireGuard profile)
            if (!simulatedVpnProfile.isNullOrBlank() && isRecognizedPrivateVpnProfile(simulatedVpnProfile)) {
                val tunnelIf = candidates.firstOrNull()?.first ?: "tun0"
                val tunnelIp = candidates.firstOrNull()?.second?.firstOrNull()?.substringBefore('%') ?: "100.64.0.1"
                return TailscaleConnectionStatus(
                    isDetected = true,
                    tailnetIp = tunnelIp,
                    interfaceName = tunnelIf,
                    vpnProfileName = simulatedVpnProfile,
                    isVpnTransportActive = true,
                    magicDnsActive = true
                )
            }

            return TailscaleConnectionStatus(
                isDetected = false,
                isVpnTransportActive = isVpnTransportActive
            )
        }
    }

    /**
     * Checks both network interfaces and Android's [ConnectivityManager] active VPN capabilities
     * (`TRANSPORT_VPN` and DNS search domains) to detect if the user is in a private Tailscale/VPN network.
     */
    fun checkPrivateNetworkHealth(
        simulatedInterfaces: List<Pair<String, List<String>>>? = null,
        simulatedVpnProfile: String? = null
    ): TailscaleConnectionStatus {
        var vpnTransportActive = false
        var detectedProfileName = simulatedVpnProfile.orEmpty()

        if (simulatedInterfaces == null && context != null) {
            runCatching {
                val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNet = cm?.activeNetwork
                val caps = if (activeNet != null) cm.getNetworkCapabilities(activeNet) else null
                val linkProps = if (activeNet != null) cm.getLinkProperties(activeNet) else null

                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    vpnTransportActive = true
                    val domains = linkProps?.domains.orEmpty()
                    val ifName = linkProps?.interfaceName.orEmpty()
                    if (detectedProfileName.isBlank()) {
                        if (isRecognizedPrivateVpnProfile(domains)) {
                            detectedProfileName = "Tailscale MagicDNS ($domains)"
                        } else if (ifName.lowercase().startsWith("tailscale") || ifName.lowercase().startsWith("tun")) {
                            // Also check if linkAddresses on the VPN link contain a Tailscale IP
                            val linkIps = linkProps?.linkAddresses?.mapNotNull { it.address?.hostAddress }.orEmpty()
                            if (linkIps.any { isTailscaleIpAddress(it) }) {
                                detectedProfileName = "Tailscale VPN ($ifName)"
                            }
                        }
                    }
                }
            }
        }

        return detectPrivateNetwork(
            simulatedInterfaces = simulatedInterfaces,
            simulatedVpnProfile = detectedProfileName.ifBlank { null },
            isVpnTransportActive = vpnTransportActive
        )
    }
}
