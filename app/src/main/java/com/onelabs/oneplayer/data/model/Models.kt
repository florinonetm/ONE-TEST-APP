package com.onelabs.oneplayer.data.model

enum class StreamKind {
    LIVE, VOD, SERIES
}

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logo: String = "",
    val category: String = "General",
    val kind: StreamKind = StreamKind.LIVE,
    val isFavorite: Boolean = false,
    val currentProgram: String? = null,
    val nextProgram: String? = null,
    val programProgress: Float = 0.45f
)

data class EpgProgram(
    val title: String,
    val startTime: String,
    val endTime: String,
    val description: String,
    val isCurrent: Boolean = false
)

data class VodItem(
    val id: String,
    val title: String,
    val streamUrl: String,
    val poster: String = "",
    val backdrop: String = "",
    val category: String = "Action",
    val rating: String = "8.4",
    val year: String = "2024",
    val duration: String = "1h 54m",
    val description: String = "",
    val cast: String = ""
)

data class SeriesItem(
    val id: String,
    val title: String,
    val poster: String = "",
    val category: String = "Drama",
    val rating: String = "8.8",
    val year: String = "2024",
    val seasonsCount: Int = 3,
    val description: String = "",
    val episodes: List<EpisodeItem> = emptyList()
)

data class EpisodeItem(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val duration: String,
    val streamUrl: String,
    val thumbnail: String = "",
    val synopsis: String = ""
)

data class PlaylistItem(
    val id: String,
    val name: String,
    val type: String, // "M3U" or "XTREAM"
    val url: String = "",
    val server: String = "",
    val username: String = "",
    val channelCount: Int = 0,
    val lastUpdated: String = "",
    val isActive: Boolean = true
)

data class UserProfile(
    val id: String,
    val name: String,
    val avatarColorHex: String,
    val isKids: Boolean = false,
    val pin: String? = null
)

data class LicenseStatus(
    val macAddress: String,
    val deviceKey: String,
    val isTrialActive: Boolean = true,
    val daysRemaining: Int = 31,
    val status: String = "Active (31-day Trial)",
    val isPairedWithCloud: Boolean = false,
    val cloudPlaylistCount: Int = 0,
    val portalUrl: String = "https://oneapk.ro"
)
