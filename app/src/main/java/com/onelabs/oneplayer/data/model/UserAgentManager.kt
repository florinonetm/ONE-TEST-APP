package com.onelabs.oneplayer.data.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserAgentPreset(
    val id: String,
    val name: String,
    val userAgent: String,
    val description: String
)

class UserAgentManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("oneplayer_user_agents", Context.MODE_PRIVATE)

    val presets = listOf(
        UserAgentPreset(
            id = "vlc",
            name = "VLC Media Player (Recommended)",
            userAgent = "VLC/3.0.18 LibVLC/3.0.18",
            description = "Bypasses strict IPTV provider server restrictions and stream blocking"
        ),
        UserAgentPreset(
            id = "tivimate",
            name = "TiviMate 4.7",
            userAgent = "TiviMate/4.7.0 (Android TV)",
            description = "Widely recognized by IPTV servers for Smart TV & set-top box streams"
        ),
        UserAgentPreset(
            id = "smarters",
            name = "IPTV Smarters Pro",
            userAgent = "IPTV Smarters Pro/3.1.5.1",
            description = "Standard user agent for Xtream Codes and M3U services"
        ),
        UserAgentPreset(
            id = "chrome",
            name = "Google Chrome (Desktop Web)",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            description = "Simulates standard desktop web browser requests"
        ),
        UserAgentPreset(
            id = "oneplayer",
            name = "ONE Player Native",
            userAgent = "ONEPlayer/2.0 (Android; SDK 35; Exoplayer)",
            description = "Direct client user-agent with Media3 engine metadata"
        ),
        UserAgentPreset(
            id = "exoplayer",
            name = "Android ExoPlayer",
            userAgent = "ExoPlayerLib/2.19.1",
            description = "Standard Android Google ExoPlayer user agent"
        )
    )

    private val _currentUserAgent = MutableStateFlow(
        prefs.getString(KEY_USER_AGENT, presets.first().userAgent) ?: presets.first().userAgent
    )
    val currentUserAgent: StateFlow<String> = _currentUserAgent.asStateFlow()

    fun setUserAgent(ua: String) {
        val cleanUa = ua.trim().ifEmpty { presets.first().userAgent }
        _currentUserAgent.value = cleanUa
        prefs.edit().putString(KEY_USER_AGENT, cleanUa).apply()
    }

    companion object {
        private const val KEY_USER_AGENT = "active_user_agent_value"
    }
}
