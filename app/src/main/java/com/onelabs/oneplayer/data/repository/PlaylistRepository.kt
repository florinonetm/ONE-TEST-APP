package com.onelabs.oneplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.onelabs.oneplayer.data.model.Channel
import com.onelabs.oneplayer.data.model.EpgProgram
import com.onelabs.oneplayer.data.model.EpisodeItem
import com.onelabs.oneplayer.data.model.PlaylistItem
import com.onelabs.oneplayer.data.model.SeriesItem
import com.onelabs.oneplayer.data.model.StreamKind
import com.onelabs.oneplayer.data.model.UserProfile
import com.onelabs.oneplayer.data.model.VodItem
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

class PlaylistRepository(private val context: Context) {

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

    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<UserProfile?>(null)
    val activeProfile: StateFlow<UserProfile?> = _activeProfile.asStateFlow()

    init {
        loadSavedFavorites()
        loadSavedProfiles()
        loadSavedPlaylists()
        seedDefaultData()
    }

    private fun loadSavedFavorites() {
        val favSet = prefs.getStringSet("favorite_channels", emptySet()) ?: emptySet()
        _favorites.value = favSet
    }

    private fun loadSavedProfiles() {
        val defaultProfiles = listOf(
            UserProfile("p1", "Main User", "#00E5FF", isKids = false),
            UserProfile("p2", "Family / Cinema", "#FFB400", isKids = false),
            UserProfile("p3", "Kids Zone", "#76FF03", isKids = true, pin = "1234")
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

    private fun seedDefaultData() {
        val defaultChannels = listOf(
            Channel(
                id = "c_nasa",
                name = "NASA TV HD",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                logo = "https://images.unsplash.com/photo-1614728894747-a83421e2b9c9?w=200",
                category = "Science & Space",
                kind = StreamKind.LIVE,
                currentProgram = "ISS Live Stream & Earth Views",
                nextProgram = "Artemis Mission Briefing",
                programProgress = 0.65f
            ),
            Channel(
                id = "c_bloomberg",
                name = "Bloomberg Quicktake",
                streamUrl = "https://bloomberg.com/mediafeeds/quicktake/live/playlist.m3u8",
                logo = "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=200",
                category = "News & Business",
                kind = StreamKind.LIVE,
                currentProgram = "Global Markets & Tech Wrap",
                nextProgram = "Business Innovators",
                programProgress = 0.30f
            ),
            Channel(
                id = "c_dw",
                name = "DW News English",
                streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
                logo = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200",
                category = "News & Business",
                kind = StreamKind.LIVE,
                currentProgram = "World News Today",
                nextProgram = "Conflict Zone Debate",
                programProgress = 0.80f
            ),
            Channel(
                id = "c_france24",
                name = "France 24 English",
                streamUrl = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8",
                logo = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=200",
                category = "News & Business",
                kind = StreamKind.LIVE,
                currentProgram = "Eye on Africa & Europe",
                nextProgram = "Culture Matters",
                programProgress = 0.40f
            ),
            Channel(
                id = "c_redbull",
                name = "Red Bull TV",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                logo = "https://images.unsplash.com/photo-1551698618-1dfe5d97d256?w=200",
                category = "Sports & Action",
                kind = StreamKind.LIVE,
                currentProgram = "Cliff Diving World Series",
                nextProgram = "Rampage Mountain Biking",
                programProgress = 0.55f
            ),
            Channel(
                id = "c_euronews",
                name = "Euronews World",
                streamUrl = "https://euronews-euronews-world-1-eu.plex.tv/live/master.m3u8",
                logo = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200",
                category = "News & Business",
                kind = StreamKind.LIVE,
                currentProgram = "European Headlines & Analysis",
                nextProgram = "Smart Health",
                programProgress = 0.20f
            ),
            Channel(
                id = "c_nature",
                name = "Wild Nature 4K",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                logo = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=200",
                category = "Documentary & Nature",
                kind = StreamKind.LIVE,
                currentProgram = "Big Cat Sanctuary Expedition",
                nextProgram = "Oceans Deep Unknown",
                programProgress = 0.70f
            ),
            Channel(
                id = "c_cinema",
                name = "Classic Cinema HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                logo = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=200",
                category = "Movies & Cinema",
                kind = StreamKind.LIVE,
                currentProgram = "Animation Classics: Bunny & Friends",
                nextProgram = "Golden Age Feature",
                programProgress = 0.15f
            )
        )

        val defaultVod = listOf(
            VodItem(
                id = "vod_1",
                title = "Big Buck Bunny (4K Ultra HD)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                poster = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=400",
                backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800",
                category = "Animation",
                rating = "8.9",
                year = "2024",
                duration = "10m",
                description = "A large and lovable rabbit deals with bully forest creatures in this groundbreaking open movie.",
                cast = "Blender Open Project Team"
            ),
            VodItem(
                id = "vod_2",
                title = "Tears of Steel (Sci-Fi Cyberpunk)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400",
                backdrop = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800",
                category = "Sci-Fi",
                rating = "8.6",
                year = "2023",
                duration = "12m",
                description = "Set in a dystopian future Amsterdam, a group of scientists attempts to stage an emotional reconnection to prevent destruction.",
                cast = "Derek de Lint, Sergio Hasselbaink, Denise Rebergen"
            ),
            VodItem(
                id = "vod_3",
                title = "Sintel (Fantasy Adventure)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400",
                backdrop = "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=800",
                category = "Fantasy",
                rating = "9.1",
                year = "2024",
                duration = "15m",
                description = "A lonely young woman attacks an arid wasteland in pursuit of a baby dragon she befriended and nursed back to health.",
                cast = "Halina Reijn, Thom Hoffman"
            ),
            VodItem(
                id = "vod_4",
                title = "Elephants Dream",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                poster = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=400",
                backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
                category = "Sci-Fi",
                rating = "8.2",
                year = "2022",
                duration = "11m",
                description = "Proog and Emo navigate an enigmatic, cavernous machine complex that seems driven by psychic manifestations.",
                cast = "Tygo Gernandt, Cas Jansen"
            ),
            VodItem(
                id = "vod_5",
                title = "Cosmos Laundromat (First Cycle)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                poster = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=400",
                backdrop = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800",
                category = "Drama",
                rating = "8.8",
                year = "2024",
                duration = "13m",
                description = "On a desolate island, a despondent sheep named Franck meets a quirky salesman who offers him the adventure of multiple lifetimes.",
                cast = "Pierre Bokma, Reinout Scholten van Aschat"
            )
        )

        val defaultSeries = listOf(
            SeriesItem(
                id = "ser_1",
                title = "Chronicles of the Universe",
                poster = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400",
                category = "Sci-Fi Documentary",
                rating = "9.4",
                year = "2024",
                seasonsCount = 2,
                description = "A deep journey through cosmic structures, black holes, and the future of interstellar humanity.",
                episodes = listOf(
                    EpisodeItem(
                        id = "ep_1_1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Birth of Stellar Nurseries",
                        duration = "45m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=300",
                        synopsis = "How massive nebulae collapse to ignite new star systems."
                    ),
                    EpisodeItem(
                        id = "ep_1_2",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "The Quantum Horizon",
                        duration = "48m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300",
                        synopsis = "Observing events near supermassive black holes."
                    )
                )
            ),
            SeriesItem(
                id = "ser_2",
                title = "Wild Horizons",
                poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400",
                category = "Nature",
                rating = "9.2",
                year = "2023",
                seasonsCount = 1,
                description = "Breathtaking cinematography across the most remote landscapes on Earth.",
                episodes = listOf(
                    EpisodeItem(
                        id = "ep_2_1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "The Frozen Tundra",
                        duration = "52m",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300",
                        synopsis = "Wildlife surviving in extreme sub-zero conditions."
                    )
                )
            )
        )

        _channels.value = defaultChannels.map {
            it.copy(isFavorite = _favorites.value.contains(it.id))
        }
        _vodItems.value = defaultVod
        _seriesItems.value = defaultSeries

        if (_playlists.value.isEmpty()) {
            _playlists.value = listOf(
                PlaylistItem(
                    id = "pl_default",
                    name = "ONE Demo Fast Streams",
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

        // Update channel list favorite state
        _channels.value = _channels.value.map {
            if (it.id == channelId) it.copy(isFavorite = current.contains(channelId)) else it
        }
    }

    suspend fun addM3uPlaylist(name: String, url: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val content = XtreamClient.httpGet(url)
            val result = M3uParser.parse(content, url)
            if (result.channels.isEmpty()) {
                return@withContext Pair(false, "No channels found in M3U playlist")
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

            Pair(true, "Added ${newChannels.size} channels successfully")
        } catch (e: Exception) {
            Pair(false, "Error: ${e.localizedMessage ?: "Failed to download playlist"}")
        }
    }

    suspend fun addXtreamAccount(name: String, server: String, u: String, p: String): Pair<Boolean, String> {
        val importResult = XtreamClient.fetchAccount(
            XtreamClient.Account(server, u, p)
        )
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
                title = "${channel.name} Late Night Focus",
                startTime = "21:00",
                endTime = "23:00",
                description = "Nightly summary, highlights, and international round-up.",
                isCurrent = false
            )
        )
    }
}
