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
                            password = obj.optString("password"),
                            channelCount = obj.optInt("channelCount"),
                            breakdown = obj.optString("breakdown"),
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
                        id = "ep_1_1_1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Ignition of Stellar Nurseries",
                        duration = "45m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=400",
                        synopsis = "How massive nebulae collapse to spark the birth of planetary systems."
                    ),
                    EpisodeItem(
                        id = "ep_1_1_2",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Singularity Event",
                        duration = "48m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400",
                        synopsis = "Telescopes capture real-time time-dilation ripples near an active galactic nucleus."
                    ),
                    EpisodeItem(
                        id = "ep_1_1_3",
                        seasonNumber = 1,
                        episodeNumber = 3,
                        title = "The Quantum Jump",
                        duration = "50m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400",
                        synopsis = "Experimental warp mechanics tested in deep space outer Lagrangian orbit."
                    ),
                    EpisodeItem(
                        id = "ep_1_2_1",
                        seasonNumber = 2,
                        episodeNumber = 1,
                        title = "Echoes of the Void",
                        duration = "46m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=400",
                        synopsis = "The crew decodes an ancient synthetic repeating beacon from Andromeda."
                    ),
                    EpisodeItem(
                        id = "ep_1_2_2",
                        seasonNumber = 2,
                        episodeNumber = 2,
                        title = "Event Horizon Colony",
                        duration = "52m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400",
                        synopsis = "Life and politics inside the humanity's first relativistic ring station."
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
                        id = "ep_2_1_1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Frozen Taiga Survival",
                        duration = "52m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400",
                        synopsis = "Wolves and snow leopards adapt to prolonged sub-zero conditions."
                    ),
                    EpisodeItem(
                        id = "ep_2_1_2",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Amazonian Canopy Secrets",
                        duration = "49m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400",
                        synopsis = "Rare nocturnal bioluminescent creatures revealed in unmapped rainforests."
                    ),
                    EpisodeItem(
                        id = "ep_2_2_1",
                        seasonNumber = 2,
                        episodeNumber = 1,
                        title = "The Abyssal Trench",
                        duration = "54m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=400",
                        synopsis = "Submersible probes dive into 11,000 meters of complete oceanic darkness."
                    )
                )
            ),
            SeriesItem(
                id = "ser_3",
                title = "Cyberpunk: Neon City",
                poster = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500",
                category = "Action & Thriller",
                rating = "98% Match",
                year = "2024",
                seasonsCount = 2,
                description = "In a hyper-connected mega-metropolis ruled by tech conglomerates, an augmented mercenary uncovers a rogue AI conspiracy.",
                episodes = listOf(
                    EpisodeItem(
                        id = "ep_3_1_1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Grid Protocol Reboot",
                        duration = "44m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=400",
                        synopsis = "A memory heist goes wrong in the subterranean lower sectors."
                    ),
                    EpisodeItem(
                        id = "ep_3_1_2",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Chrome & Shadows",
                        duration = "47m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=400",
                        synopsis = "An underground syndicate delivers an ultimatum to the defense grid."
                    )
                )
            ),
            SeriesItem(
                id = "ser_4",
                title = "Shadows of Bucharest",
                poster = "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?w=500",
                category = "Crime & Mystery",
                rating = "96% Match",
                year = "2023",
                seasonsCount = 3,
                description = "A seasoned detective and a cyber forensic investigator unravel interconnected corruption and clandestine art heists.",
                episodes = listOf(
                    EpisodeItem(
                        id = "ep_4_1_1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Midnight at Lipscani",
                        duration = "50m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?w=400",
                        synopsis = "A priceless museum artifact vanishes during a blackout in the Old Town."
                    ),
                    EpisodeItem(
                        id = "ep_4_1_2",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "The Archival Cipher",
                        duration = "53m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=400",
                        synopsis = "Encrypted ledger documents reveal an international trafficking ring."
                    )
                )
            ),
            SeriesItem(
                id = "ser_5",
                title = "The Last Frontier",
                poster = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=500",
                category = "Adventure",
                rating = "95% Match",
                year = "2024",
                seasonsCount = 2,
                description = "Isolated mountaineering rescue teams risk everything traversing treacherous peaks in the world's most unforgiving alpine ranges.",
                episodes = listOf(
                    EpisodeItem(
                        id = "ep_5_1_1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Whiteout Avalanche",
                        duration = "48m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=400",
                        synopsis = "A sudden blizzard traps an expedition near the K2 summit ridge."
                    ),
                    EpisodeItem(
                        id = "ep_5_1_2",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Glacier Chasm Rescue",
                        duration = "51m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=400",
                        synopsis = "High-altitude helicopters attempt an impossible crevasse extraction."
                    )
                )
            ),
            SeriesItem(
                id = "ser_6",
                title = "Silicon Valley Chronicles",
                poster = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=500",
                category = "Comedy & Tech",
                rating = "94% Match",
                year = "2024",
                seasonsCount = 3,
                description = "A sharp, witty satire following five quirky engineers building an open-source decentralized computing protocol from a garage.",
                episodes = listOf(
                    EpisodeItem(
                        id = "ep_6_1_1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Hello, World! (And Goodbye Sleep)",
                        duration = "32m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400",
                        synopsis = "The team burns through 48 hours of energy drinks pitching to seed investors."
                    ),
                    EpisodeItem(
                        id = "ep_6_1_2",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Merge Conflict Chaos",
                        duration = "34m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=400",
                        synopsis = "An accidental force-push deletes the production database three hours before demo day."
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
        _parsingProgress.value = "Connecting to server (Timeout: 300s)..."

        try {
            XtreamClient.openHttpStream(
                urlStr = url,
                userAgent = currentUa,
                connectTimeoutMs = 60000,
                readTimeoutMs = 300000
            ).use { resp ->
                _parsingProgress.value = "Streaming & parsing channels (up to 250,000)..."
                val result = M3uParser.parseFromInputStream(
                    inputStream = resp.stream,
                    isGzip = resp.isGzip,
                    sourceUrl = url,
                    maxLimit = 250000,
                    onProgress = { count ->
                        _parsingProgress.value = "Parsed $count streams..."
                    }
                )

                if (result.channels.isEmpty()) {
                    _parsingProgress.value = null
                    return@withContext Pair(false, "No valid streams found in playlist. Check URL or User-Agent.")
                }

                val newChannels = result.channels.mapIndexed { idx, it ->
                    Channel(
                        id = "m3u_${it.url.hashCode()}_$idx",
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

                val liveChannels = newChannels.filter { it.kind == StreamKind.LIVE }
                _channels.value = _channels.value + (if (liveChannels.isNotEmpty()) liveChannels else newChannels)

                val parsedVod = extractVodFromChannels(newChannels)
                if (parsedVod.isNotEmpty()) {
                    _vodItems.value = _vodItems.value + parsedVod
                }

                val parsedSeries = extractSeriesFromChannels(newChannels)
                if (parsedSeries.isNotEmpty()) {
                    _seriesItems.value = _seriesItems.value + parsedSeries
                }

                val liveCount = liveChannels.size
                val vodCount = parsedVod.size
                val seriesCount = parsedSeries.size
                val breakdown = "$liveCount Live · $vodCount Movies · $seriesCount Series"

                val newPlaylist = PlaylistItem(
                    id = UUID.randomUUID().toString(),
                    name = name.ifEmpty { "M3U Playlist" },
                    type = "M3U",
                    url = url,
                    channelCount = newChannels.size,
                    breakdown = breakdown,
                    lastUpdated = "Just now"
                )

                val updatedPlaylists = _playlists.value + newPlaylist
                _playlists.value = updatedPlaylists
                savePlaylistsToDisk(updatedPlaylists)

                _parsingProgress.value = null
                Pair(true, "Successfully loaded all ${newChannels.size} items ($breakdown)")
            }
        } catch (e: Exception) {
            _parsingProgress.value = null
            Pair(false, "Connection error: ${e.localizedMessage ?: "Failed to download playlist"}")
        }
    }

    suspend fun addXtreamAccount(name: String, server: String, u: String, p: String): Pair<Boolean, String> {
        val currentUa = userAgentManager.currentUserAgent.value
        _parsingProgress.value = "Connecting to Xtream Codes server (Catalog up to 250,000)..."

        val importResult = XtreamClient.fetchAccount(
            account = XtreamClient.Account(server, u, p),
            userAgent = currentUa,
            maxLimit = 250000,
            onProgress = { count ->
                _parsingProgress.value = "Loaded $count streams..."
            }
        )

        _parsingProgress.value = null
        if (!importResult.success) {
            return Pair(false, importResult.message)
        }

        val liveChannels = importResult.channels.filter { it.kind == StreamKind.LIVE }
        _channels.value = _channels.value + (if (liveChannels.isNotEmpty()) liveChannels else importResult.channels)

        val parsedVod = extractVodFromChannels(importResult.channels)
        if (parsedVod.isNotEmpty()) {
            _vodItems.value = _vodItems.value + parsedVod
        }

        val parsedSeries = extractSeriesFromChannels(importResult.channels)
        if (parsedSeries.isNotEmpty()) {
            _seriesItems.value = _seriesItems.value + parsedSeries
        }

        val breakdown = "${importResult.liveCount} Live · ${importResult.vodCount} Movies · ${importResult.seriesCount} Series"

        val newPlaylist = PlaylistItem(
            id = UUID.randomUUID().toString(),
            name = name.ifEmpty { "Xtream (${XtreamClient.normalizeServer(server)})" },
            type = "XTREAM",
            server = server,
            username = u,
            password = p,
            channelCount = importResult.channels.size,
            breakdown = breakdown,
            lastUpdated = "Just now"
        )

        val updatedPlaylists = _playlists.value + newPlaylist
        _playlists.value = updatedPlaylists
        savePlaylistsToDisk(updatedPlaylists)

        return Pair(true, importResult.message)
    }

    suspend fun reloadPlaylist(playlist: PlaylistItem): Pair<Boolean, String> {
        return if (playlist.type == "XTREAM") {
            addXtreamAccount(playlist.name, playlist.server, playlist.username, playlist.password)
        } else {
            addM3uPlaylist(playlist.name, playlist.url)
        }
    }

    suspend fun deletePlaylist(playlistId: String): Boolean = withContext(Dispatchers.IO) {
        val remaining = _playlists.value.filter { it.id != playlistId }
        _playlists.value = remaining
        savePlaylistsToDisk(remaining)

        if (remaining.isEmpty()) {
            _channels.value = emptyList()
            _vodItems.value = emptyList()
            _seriesItems.value = emptyList()
        } else {
            val nextCustom = remaining.firstOrNull { it.id != "pl_default" }
            if (nextCustom != null) {
                reloadPlaylist(nextCustom)
            } else {
                seedShowcaseData()
            }
        }
        true
    }

    fun deletePlaylistSync(playlistId: String) {
        val remaining = _playlists.value.filter { it.id != playlistId }
        _playlists.value = remaining
        savePlaylistsToDisk(remaining)

        if (remaining.isEmpty()) {
            _channels.value = emptyList()
            _vodItems.value = emptyList()
            _seriesItems.value = emptyList()
        } else {
            val nextCustom = remaining.firstOrNull { it.id != "pl_default" }
            if (nextCustom == null) {
                seedShowcaseData()
            }
        }
    }

    fun resetToShowcaseData() {
        prefs.edit().remove("playlists_json").apply()
        _playlists.value = emptyList()
        seedShowcaseData()
    }

    private fun extractVodFromChannels(channels: List<Channel>): List<VodItem> {
        val vodChannels = channels.filter { it.kind == StreamKind.VOD }
        if (vodChannels.isEmpty()) return emptyList()

        return vodChannels.mapIndexed { idx, ch ->
            VodItem(
                id = if (ch.id.isNotEmpty()) ch.id else "vod_${ch.streamUrl.hashCode()}_$idx",
                title = ch.name,
                poster = ch.logo.ifEmpty { "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500" },
                backdrop = ch.logo.ifEmpty { "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1000" },
                category = ch.category.ifEmpty { "Movies" },
                rating = "96% Match",
                year = "2024",
                duration = "Movie",
                streamUrl = ch.streamUrl,
                description = ch.currentProgram ?: "Stream Movie: ${ch.name}"
            )
        }
    }

    private fun extractSeriesFromChannels(channels: List<Channel>): List<SeriesItem> {
        val seriesChannels = channels.filter { it.kind == StreamKind.SERIES }
        if (seriesChannels.isEmpty()) return emptyList()

        val seriesMap = mutableMapOf<String, MutableList<EpisodeItem>>()
        val seriesMeta = mutableMapOf<String, Triple<String, String, String>>()
        val seriesRegex = Regex("""(?i)(.*?)(?:\s*[-_]?\s*)[sS](\d+)\s*[eE](\d+)(?:[-:\s]*(.*))?""")

        for (ch in seriesChannels) {
            val match = seriesRegex.find(ch.name)
            if (match != null) {
                val seriesTitle = match.groupValues[1].trim().ifEmpty { ch.category }
                val seasonNum = match.groupValues[2].toIntOrNull() ?: 1
                val epNum = match.groupValues[3].toIntOrNull() ?: 1
                val epTitle = match.groupValues[4].trim().ifEmpty { "Episode $epNum" }

                val list = seriesMap.getOrPut(seriesTitle) { mutableListOf() }
                list.add(
                    EpisodeItem(
                        id = ch.id,
                        seasonNumber = seasonNum,
                        episodeNumber = epNum,
                        title = epTitle,
                        duration = "45m",
                        streamUrl = ch.streamUrl,
                        thumbnail = ch.logo,
                        synopsis = ch.currentProgram ?: "Episode $epNum of $seriesTitle"
                    )
                )
                if (!seriesMeta.containsKey(seriesTitle)) {
                    seriesMeta[seriesTitle] = Triple(ch.logo, ch.category, "Streamed series from playlist")
                }
            } else {
                val seriesTitle = ch.name.substringBefore(" S0").substringBefore(" S1").trim()
                val list = seriesMap.getOrPut(seriesTitle) { mutableListOf() }
                list.add(
                    EpisodeItem(
                        id = ch.id,
                        seasonNumber = 1,
                        episodeNumber = list.size + 1,
                        title = ch.name,
                        duration = "45m",
                        streamUrl = ch.streamUrl,
                        thumbnail = ch.logo,
                        synopsis = ch.currentProgram ?: "Stream episode"
                    )
                )
                if (!seriesMeta.containsKey(seriesTitle)) {
                    seriesMeta[seriesTitle] = Triple(ch.logo, ch.category, "Streamed series from playlist")
                }
            }
        }

        return seriesMap.map { (title, episodes) ->
            val meta = seriesMeta[title]
            val maxSeason = episodes.maxOfOrNull { it.seasonNumber } ?: 1
            SeriesItem(
                id = "parsed_ser_${title.hashCode()}",
                title = title,
                poster = meta?.first?.ifEmpty { "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=500" } ?: "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=500",
                category = meta?.second ?: "Series",
                rating = "95% Match",
                year = "2024",
                seasonsCount = maxSeason,
                description = meta?.third ?: "Full series catalog",
                episodes = episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
            )
        }
    }

    fun getSeriesSeasons(series: SeriesItem): List<Int> {
        val fromEpisodes = series.episodes.map { it.seasonNumber }.distinct().sorted()
        return if (fromEpisodes.isNotEmpty()) fromEpisodes else (1..series.seasonsCount.coerceAtLeast(1)).toList()
    }

    fun getEpisodesForSeason(series: SeriesItem, seasonNumber: Int): List<EpisodeItem> {
        val filtered = series.episodes.filter { it.seasonNumber == seasonNumber }.sortedBy { it.episodeNumber }
        return if (filtered.isNotEmpty()) filtered else series.episodes
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
            obj.put("password", pl.password)
            obj.put("channelCount", pl.channelCount)
            obj.put("breakdown", pl.breakdown)
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
