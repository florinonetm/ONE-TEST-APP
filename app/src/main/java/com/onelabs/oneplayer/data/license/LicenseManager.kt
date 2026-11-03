package com.onelabs.oneplayer.data.license

import android.content.Context
import android.content.SharedPreferences
import com.onelabs.oneplayer.data.model.LicenseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

class LicenseManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("oneplayer_license_prefs", Context.MODE_PRIVATE)

    private val _licenseStatus = MutableStateFlow(loadCurrentStatus())
    val licenseStatus: StateFlow<LicenseStatus> = _licenseStatus.asStateFlow()

    init {
        // Ensure MAC and key exist
        if (!prefs.contains(KEY_MAC)) {
            val mac = generateRealisticMac()
            val key = String.format("%06d", Random().nextInt(900000) + 100000)
            val installTimestamp = System.currentTimeMillis()

            prefs.edit()
                .putString(KEY_MAC, mac)
                .putString(KEY_DEVICE_KEY, key)
                .putLong(KEY_INSTALL_TIME, installTimestamp)
                .apply()
        }
        _licenseStatus.value = loadCurrentStatus()
    }

    private fun loadCurrentStatus(): LicenseStatus {
        val mac = prefs.getString(KEY_MAC, "") ?: generateRealisticMac()
        val key = prefs.getString(KEY_DEVICE_KEY, "") ?: "839201"
        val installTime = prefs.getLong(KEY_INSTALL_TIME, System.currentTimeMillis())
        val isCloudPaired = prefs.getBoolean(KEY_CLOUD_PAIRED, false)
        val cloudCount = prefs.getInt(KEY_CLOUD_COUNT, 0)

        val elapsedMillis = System.currentTimeMillis() - installTime
        val elapsedDays = (elapsedMillis / (1000L * 60 * 60 * 24)).toInt()
        val daysRemaining = maxOf(0, 31 - elapsedDays)
        val isTrialActive = daysRemaining > 0 || isCloudPaired

        val statusText = when {
            isCloudPaired -> "Activated via oneapk.ro"
            daysRemaining > 0 -> "Active (31-day Trial · $daysRemaining days left)"
            else -> "Trial Expired (Pair at oneapk.ro)"
        }

        return LicenseStatus(
            macAddress = mac,
            deviceKey = key,
            isTrialActive = isTrialActive,
            daysRemaining = daysRemaining,
            status = statusText,
            isPairedWithCloud = isCloudPaired,
            cloudPlaylistCount = cloudCount
        )
    }

    suspend fun checkLicense(): LicenseStatus = withContext(Dispatchers.IO) {
        val current = _licenseStatus.value
        try {
            val url = URL("https://oneapk.ro/pair.php?mac=${current.macAddress}&key=${current.deviceKey}")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "ONEPlayer/1.02 (Android; SDK 35)")

            if (conn.responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(response)
                val active = json.optBoolean("active", false) || json.optString("status") == "active"
                if (active) {
                    prefs.edit().putBoolean(KEY_CLOUD_PAIRED, true).apply()
                }
            }
        } catch (_: Exception) {
            // Keep current offline trial status
        }

        val updated = loadCurrentStatus()
        _licenseStatus.value = updated
        updated
    }

    suspend fun syncCloudPlaylists(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val current = _licenseStatus.value
        try {
            val url = URL("https://oneapk.ro/api/v1/playlists.php?mac=${current.macAddress}")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "ONEPlayer/1.02 (Android; SDK 35)")

            if (conn.responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(response)
                val playlistsArray = json.optJSONArray("playlists")
                val count = playlistsArray?.length() ?: 0

                prefs.edit()
                    .putBoolean(KEY_CLOUD_PAIRED, true)
                    .putInt(KEY_CLOUD_COUNT, count)
                    .apply()

                _licenseStatus.value = loadCurrentStatus()
                Pair(true, "Synced $count playlist(s) from oneapk.ro cloud")
            } else {
                Pair(false, "Cloud response: HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, "Device connected to oneapk.ro pairing portal. Add playlists at https://oneapk.ro")
        }
    }

    private fun generateRealisticMac(): String {
        val r = Random()
        val b = ByteArray(6)
        r.nextBytes(b)
        b[0] = (b[0].toInt() and 254 or 2).toByte() // Unicast, locally administered
        return b.joinToString(":") { String.format("%02X", it) }
    }

    companion object {
        private const val KEY_MAC = "device_mac_address"
        private const val KEY_DEVICE_KEY = "device_pairing_key"
        private const val KEY_INSTALL_TIME = "install_timestamp"
        private const val KEY_CLOUD_PAIRED = "is_cloud_paired"
        private const val KEY_CLOUD_COUNT = "cloud_playlist_count"
    }
}
