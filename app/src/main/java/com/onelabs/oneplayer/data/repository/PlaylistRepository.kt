package com.onelabs.oneplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.onelabs.oneplayer.data.model.*
import com.onelabs.oneplayer.data.network.XtreamClient
import com.onelabs.oneplayer.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ContinueWatchingItem(
    val id: String,
    val title: String,
    val streamUrl: String,
    val poster: String,
    val progressFraction: Float,
    val lastPositionMs: Long,
    val isLive: Boolean = false
)

class PlaylistRepository(
    private val context: Context,
    val userAgentManager: UserAgentManager
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("oneplayer_data_prefs", Context.MODE_PRIVATE)

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _vodItems = MutableStateFlow<List<VodItem>>(emptyList())
    val vodItems: StateFlow<List<VodItem>> = _vodItems.asStateFlow()

    private val _seriesItems = MutableStateFlow<List<SeriesItem>>(emptyList())
    val seriesItems: StateFlow<List<SeriesItem>> = _seriesItems.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlists: StateFlow<List<PlaylistItem>> = _playlists.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _continueWatching = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val continueWatching: StateFlow<List<ContinueWatchingItem>> = _continueWatching.asStateFlow()

    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<UserProfile?>(null)
    val activeProfile: StateFlow<UserProfile?> = _activeProfile.asStateFlow()

    private val _parsingProgress = MutableStateFlow<String?>(null)
    val parsingProgress: StateFlow<String?> = _parsingProgress.asStateFlow()

    init {
        loadSavedFavorites()
        loadSavedProfiles()
        loadSavedPlaylists()
        seedShowcaseData()
    }

    private fun loadSavedFavorites() {
        val favSet = prefs.getStringSet("favorite_channels", emptySet()) ?: emptySet()
        _favorites.value = favSet
    }

    private fun loadSavedProfiles() {
        val defaultProfiles = listOf(
            UserProfile("p1", "Florin", "#E50914", isKids = false),
            UserProfile("p2", "Family", "#0071EB", isKids = false),
            UserProfile("p3", "Kids", "#46D369", isKids = true, pin = "1234")
        )
        _profiles.value = defaultProfiles
        _activeProfile.value = defaultProfiles.first()
    }

    private fun loadSavedPlaylists() {
        val saved = prefs.getString("playlists_json", null)
        if (saved != null) {
            try {
                val arr = JSONArray(saved)
                val list = mutableListOf<PlaylistItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        PlaylistItem(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            type = obj.getString("type"),
                            url = obj.optString("url"),
                            server = obj.optString("server"),
                            username = obj.optString("username"),
                            channelCount = obj.optInt("channelCount"),
                            lastUpdated = obj.optString("lastUpdated")
                        )
                    )
                }
                _playlists.value = list
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    private fun seedShowcaseData() {
        val defaultChannels = listOf(
            Channel(
                id = "c_nasa",
                name = "NASA TV 4K",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                logo = "https://images.unsplash.com/photo-1614728894747-a83421e2b9c9?w=300",
                category = "Documentaries",
                kind = StreamKind.LIVE,
                currentProgram = "ISS Live: Orbiting the Pacific",
                nextProgram = "Deep Space Discoveries",
                programProgress = 0.65f
            ),
            Channel(
                id = "c_redbull",
                name = "Red Bull TV HD",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                logo = "https://images.unsplash.com/photo-1551698618-1dfe5d97d256?w=300",
                category = "Sports",
                kind = StreamKind.LIVE,
                currentProgram = "Rampage Extreme Mountain Biking",
                nextProgram = "Cliff Diving World Tour",
                programProgress = 0.55f
            ),
            Channel(
                id = "c_nature",
                name = "Wild Planet 4K",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                logo = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300",
                category = "Documentaries",
                kind = StreamKind.LIVE,
                currentProgram = "Apex Predators of Serengeti",
                nextProgram = "Oceans Unknown",
                programProgress = 0.75f
            ),
            Channel(
                id = "c_bloomberg",
                name = "Bloomberg Quicktake",
                streamUrl = "https://bloomberg.com/mediafeeds/quicktake/live/playlist.m3u8",
                logo = "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=300",
                category = "News",
                kind = StreamKind.LIVE,
                currentProgram = "Global Tech & Market Close",
                nextProgram = "The Future of AI",
                programProgress = 0.30f
            ),
            Channel(
                id = "c_dw",
                name = "DW News HD",
                streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
                logo = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=300",
                category = "News",
                kind = StreamKind.LIVE,
                currentProgram = "Prime European Report",
                nextProgram = "Conflict Zone",
                programProgress = 0.80f
            ),
            Channel(
                id = "c_france24",
                name = "France 24 HD",
                streamUrl = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8",
                logo = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=300",
                category = "News",
                kind = StreamKind.LIVE,
                currentProgram = "World Panorama",
                nextProgram = "Inside the Culture",
                programProgress = 0.40f
            ),
            Channel(
                id = "c_euronews",
                name = "Euronews Now",
                streamUrl = "https://euronews-euronews-world-1-eu.plex.tv/live/master.m3u8",
                logo = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=300",
                category = "News",
                kind = StreamKind.LIVE,
                currentProgram = "Brussels Dispatch",
                nextProgram = "Global Business",
                programProgress = 0.20f
            )
        )

        val defaultVod = listOf(
            VodItem(
                id = "vod_1",
                title = "Big Buck Bunny (4K Ultra HD)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                poster = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=500",
                backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1200",
                category = "Action & Adventure",
                rating = "98% Match",
                year = "2024",
                duration = "10m",
                description = "When a lovable giant rabbit is pushed to his limits by mischievous woodland bullies, he devises an ingenious forest defense mechanism.",
                cast = "Blender Open Creative Studio"
            ),
            VodItem(
                id = "vod_2",
                title = "Tears of Steel",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                poster = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500",
                backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200",
                category = "Sci-Fi & Cyberpunk",
                rating = "95% Match",
                year = "2024",
                duration = "12m",
                description = "In a dystopian cyberpunk Amsterdam, scientists stage an emotional reconnection in an old rocket launcher to avert an alien robot cataclysm.",
                cast = "Derek de Lint, Sergio Hasselbaink, Denise Rebergen"
            ),
            VodItem(
                id = "vod_3",
                title = "Sintel: Dragon's Dawn",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500",
                backdrop = "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=1200",
                category = "Fantasy",
                rating = "99% Match",
                year = "2024",
                duration = "15m",
                description = "A determined lone warrior journeys across scorching desert wastelands and frozen mountain peaks to rescue her baby dragon companion.",
                cast = "Halina Reijn, Thom Hoffman"
            ),
            VodItem(
                id = "vod_4",
                title = "Elephants Dream",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                poster = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=500",
                backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200",
                category = "Sci-Fi & Cyberpunk",
                rating = "91% Match",
                year = "2023",
                duration = "11m",
                description = "Two wanderers venture through the endless recursive chambers of a gargantuan psychic biomechanical labyrinth.",
                cast = "Tygo Gernandt, Cas Jansen"
            ),
            VodItem(
                id = "vod_5",
                title = "Cosmos Laundromat",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                poster = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=500",
                backdrop = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200",
                category = "Dramas",
                rating = "93% Match",
                year = "2024",
                duration = "13m",
                description = "On a windswept desolate cliffside, an existential sheep is given a mysterious token that grants him parallel lives across space and time.",
                cast = "Pierre Bokma, Reinout Scholten"
            )
        )

        val defaultSeries = listOf(
            SeriesItem(
                id = "ser_1",
                title = "Cosmic Odyssey",
                poster = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500",
                category = "Sci-Fi & Fantasy",
                rating = "99% Match",
                year = "2024",
                seasonsCount = 3,
                description = "An exhilarating deep dive into gravitational waves, supermassive black holes, and the future of human exploration beyond the solar system.",
                episodes = listOf(
                    EpisodeItem(
                        id = "ep_1_1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Ignition of Stellar Nurseries",
                        duration = "45m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=400",
                        synopsis = "How massive nebulae collapse to spark the birth of planetary systems."
                    ),
                    EpisodeItem(
                        id = "ep_1_2",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Singularity Event",
                        duration = "48m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400",
                        synopsis = "Telescopes capture real-time time-dilation ripples near an active galactic nucleus."
                    )
                )
            ),
            SeriesItem(
                id = "ser_2",
                title = "Earth: Wild Expeditions",
                poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500",
                category = "Documentaries",
                rating = "97% Match",
                year = "2024",
                seasonsCount = 2,
                description = "Unprecedented high-speed 4K cinematography chronicling earth's most resilient predator and prey ecosystems.",
                episodes = listOf(
                    EpisodeItem(
                        id = "ep_2_1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Frozen Taiga Survival",
                        duration = "52m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400",
                        synopsis = "Wolves and snow leopards adapt to prolonged sub-zero conditions."
                    )
                )
            )
        )

        val defaultContinue = listOf(
            ContinueWatchingItem(
                id = "cw_1",
                title = "Big Buck Bunny (4K)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                poster = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=400",
                progressFraction = 0.62f,
                lastPositionMs = 380000L
            ),
            ContinueWatchingItem(
                id = "cw_2",
                title = "NASA TV 4K",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                poster = "https://images.unsplash.com/photo-1614728894747-a83421e2b9c9?w=400",
                progressFraction = 0.88f,
                lastPositionMs = 0L,
                isLive = true
            ),
            ContinueWatchingItem(
                id = "cw_3",
                title = "Tears of Steel",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                poster = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=400",
                progressFraction = 0.35f,
                lastPositionMs = 240000L
            )
        )

        _channels.value = defaultChannels.map {
            it.copy(isFavorite = _favorites.value.contains(it.id))
        }
        _vodItems.value = defaultVod
        _seriesItems.value = defaultSeries
        _continueWatching.value = defaultContinue

        if (_playlists.value.isEmpty()) {
            _playlists.value = listOf(
                PlaylistItem(
                    id = "pl_default",
                    name = "ONE Demo Global Streams",
                    type = "M3U",
                    url = "Built-in Showcase",
                    channelCount = defaultChannels.size,
                    lastUpdated = "Today"
                )
            )
        }
    }

    fun toggleFavorite(channelId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(channelId)) {
            current.remove(channelId)
        } else {
            current.add(channelId)
        }
        _favorites.value = current
        prefs.edit().putStringSet("favorite_channels", current).apply()

        _channels.value = _channels.value.map {
            if (it.id == channelId) it.copy(isFavorite = current.contains(channelId)) else it
        }
    }

    fun updateContinueWatching(title: String, url: String, poster: String, progress: Float, positionMs: Long, isLive: Boolean) {
        val current = _continueWatching.value.toMutableList()
        current.removeAll { it.streamUrl == url }
        current.add(
            0,
            ContinueWatchingItem(
                id = UUID.randomUUID().toString(),
                title = title,
                streamUrl = url,
                poster = poster.ifEmpty { "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=400" },
                progressFraction = progress.coerceIn(0.05f, 0.98f),
                lastPositionMs = positionMs,
                isLive = isLive
            )
        )
        _continueWatching.value = current.take(10)
    }

    suspend fun addM3uPlaylist(name: String, url: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val currentUa = userAgentManager.currentUserAgent.value
        _parsingProgress.value = "Connecting to server (Timeout: 180s)..."

        try {
            // Extended 60s connect / 180s read timeout
            val content = XtreamClient.httpGet(
                urlStr = url,
                userAgent = currentUa,
                connectTimeoutMs = 60000,
                readTimeoutMs = 180000
            )

            _parsingProgress.value = "Parsing channels & streams..."
            val result = M3uParser.parse(
                content = content,
                sourceUrl = url,
                maxLimit = 100000,
                onProgress = { count ->
                    _parsingProgress.value = "Parsed $count channels..."
                }
            )

            if (result.channels.isEmpty()) {
                _parsingProgress.value = null
                return@withContext Pair(false, "No valid streams found in playlist. Check URL or User-Agent.")
            }

            val newChannels = result.channels.map {
                Channel(
                    id = UUID.randomUUID().toString(),
                    name = it.name,
                    streamUrl = it.url,
                    logo = it.logo,
                    category = it.group,
                    kind = when (it.kind) {
                        M3uParser.Kind.LIVE -> StreamKind.LIVE
                        M3uParser.Kind.VOD -> StreamKind.VOD
                        M3uParser.Kind.SERIES -> StreamKind.SERIES
                    }
                )
            }

            _channels.value = _channels.value + newChannels

            val newPlaylist = PlaylistItem(
                id = UUID.randomUUID().toString(),
                name = name.ifEmpty { "M3U Playlist" },
                type = "M3U",
                url = url,
                channelCount = newChannels.size,
                lastUpdated = "Just now"
            )

            val updatedPlaylists = _playlists.value + newPlaylist
            _playlists.value = updatedPlaylists
            savePlaylistsToDisk(updatedPlaylists)

            _parsingProgress.value = null
            Pair(true, "Successfully loaded all ${newChannels.size} streams!")
        } catch (e: Exception) {
            _parsingProgress.value = null
            Pair(false, "Connection error: ${e.localizedMessage ?: "Failed to download playlist"}")
        }
    }

    suspend fun addXtreamAccount(name: String, server: String, u: String, p: String): Pair<Boolean, String> {
        val currentUa = userAgentManager.currentUserAgent.value
        _parsingProgress.value = "Authenticating with Xtream Codes API..."

        val importResult = XtreamClient.fetchAccount(
            account = XtreamClient.Account(server, u, p),
            userAgent = currentUa,
            maxLive = 50000,
            maxVod = 25000,
            maxSeries = 10000,
            onProgress = { count ->
                _parsingProgress.value = "Loaded $count streams..."
            }
        )

        _parsingProgress.value = null
        if (!importResult.success) {
            return Pair(false, importResult.message)
        }

        _channels.value = _channels.value + importResult.channels

        val newPlaylist = PlaylistItem(
            id = UUID.randomUUID().toString(),
            name = name.ifEmpty { "Xtream ($server)" },
            type = "XTREAM",
            server = server,
            username = u,
            channelCount = importResult.channels.size,
            lastUpdated = "Just now"
        )

        val updatedPlaylists = _playlists.value + newPlaylist
        _playlists.value = updatedPlaylists
        savePlaylistsToDisk(updatedPlaylists)

        return Pair(true, importResult.message)
    }

    private fun savePlaylistsToDisk(list: List<PlaylistItem>) {
        val arr = JSONArray()
        for (pl in list) {
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            obj.put("type", pl.type)
            obj.put("url", pl.url)
            obj.put("server", pl.server)
            obj.put("username", pl.username)
            obj.put("channelCount", pl.channelCount)
            obj.put("lastUpdated", pl.lastUpdated)
            arr.put(obj)
        }
        prefs.edit().putString("playlists_json", arr.toString()).apply()
    }

    fun setActiveProfile(profile: UserProfile) {
        _activeProfile.value = profile
    }

    fun getEpgForChannel(channel: Channel): List<EpgProgram> {
        return listOf(
            EpgProgram(
                title = channel.currentProgram ?: "${channel.name} Prime Broadcast",
                startTime = "18:00",
                endTime = "19:30",
                description = "Current active program stream with live coverage and expert commentary.",
                isCurrent = true
            ),
            EpgProgram(
                title = channel.nextProgram ?: "${channel.name} Special Edition",
                startTime = "19:30",
                endTime = "21:00",
                description = "Upcoming featured evening broadcast and in-depth report.",
                isCurrent = false
            ),
            EpgProgram(
                title = "${channel.name} Late Night Special",
                startTime = "21:00",
                endTime = "23:00",
                description = "Nightly summary, international highlights, and analysis.",
                isCurrent = false
            )
        )
    }
}
