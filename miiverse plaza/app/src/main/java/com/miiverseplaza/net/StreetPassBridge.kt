package com.miiverseplaza.net

import android.content.Context
import android.net.wifi.WifiManager
import com.miiverseplaza.model.Encounter
import com.miiverseplaza.model.PlazaProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class StreetPassBridge(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onEncounter: (Encounter) -> Unit
) {
    private var listenerJob: Job? = null
    private var beaconJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start(profile: PlazaProfile) {
        if (listenerJob?.isActive == true || beaconJob?.isActive == true) {
            return
        }

        acquireMulticastLock()

        listenerJob = scope.launch(Dispatchers.IO) {
            val socket = DatagramSocket(PlazaProtocol.PORT).apply {
                broadcast = true
                soTimeout = 0
            }

            try {
                val buffer = ByteArray(2048)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val json = packet.data.decodeToString(0, packet.length)
                    val remoteProfile = PlazaProtocol.decodeProfile(json) ?: continue
                    if (remoteProfile.deviceId == profile.deviceId) {
                        continue
                    }

                    onEncounter(
                        Encounter(
                            profile = remoteProfile,
                            timestamp = System.currentTimeMillis(),
                            hostAddress = packet.address.hostAddress ?: "unknown"
                        )
                    )
                }
            } finally {
                socket.close()
            }
        }

        beaconJob = scope.launch(Dispatchers.IO) {
            val socket = DatagramSocket().apply {
                broadcast = true
            }

            val beacon = PlazaProtocol.encodeBeacon(profile)
            val address = InetAddress.getByName("255.255.255.255")

            try {
                while (isActive) {
                    val packet = DatagramPacket(beacon, beacon.size, address, PlazaProtocol.PORT)
                    socket.send(packet)
                    delay(BEACON_INTERVAL_MS)
                }
            } finally {
                socket.close()
            }
        }
    }

    fun stop() {
        listenerJob?.cancel()
        beaconJob?.cancel()
        listenerJob = null
        beaconJob = null
        releaseMulticastLock()
    }

    fun sendSystemSync(targetHost: String, payload: String, onComplete: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val success = runCatching {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val address = InetAddress.getByName(targetHost)
                    val data = payload.toByteArray(Charsets.UTF_8)
                    val packet = DatagramPacket(data, data.size, address, PlazaProtocol.PORT)
                    socket.send(packet)
                }
                true
            }.getOrElse { false }

            scope.launch(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    private fun acquireMulticastLock() {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = wifiManager?.createMulticastLock("miiverse-plaza-lock")?.apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.takeIf { it.isHeld }?.release()
        multicastLock = null
    }

    companion object {
        private const val BEACON_INTERVAL_MS = 4_000L
    }
}
