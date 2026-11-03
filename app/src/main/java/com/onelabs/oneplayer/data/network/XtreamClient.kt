package com.onelabs.oneplayer.data.network

import com.onelabs.oneplayer.data.model.Channel
import com.onelabs.oneplayer.data.model.StreamKind
import com.onelabs.oneplayer.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

object XtreamClient {

    const val DEFAULT_CONNECT_TIMEOUT_MS = 60000 // 60s
    const val DEFAULT_READ_TIMEOUT_MS = 180000   // 180s (3 minutes for huge playlists)

    data class Account(
        val server: String,
        val username: String,
        val password: String
    )

    data class ImportResult(
        val channels: List<Channel>,
        val liveCount: Int,
        val vodCount: Int,
        val seriesCount: Int,
        val message: String,
        val success: Boolean
    )

    fun normalizeServer(server: String): String {
        var s = server.trim()
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "http://$s"
        }
        return s.removeSuffix("/")
    }

    suspend fun fetchAccount(
        account: Account,
        userAgent: String = "VLC/3.0.18 LibVLC/3.0.18",
        maxLive: Int = 50000,
        maxVod: Int = 25000,
        maxSeries: Int = 10000,
        onProgress: ((Int) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        val server = normalizeServer(account.server)
        val u = account.username.trim()
        val p = account.password.trim()

        if (server.isEmpty() || u.isEmpty() || p.isEmpty()) {
            return@withContext ImportResult(
                channels = emptyList(),
                liveCount = 0,
                vodCount = 0,
                seriesCount = 0,
                message = "Server, username and password are required",
                success = false
            )
        }

        try {
            // Verify credentials via player_api
            val authUrl = "$server/player_api.php?username=${enc(u)}&password=${enc(p)}"
            val authJsonStr = httpGet(authUrl, userAgent = userAgent)
            val authJson = JSONObject(authJsonStr)
            val userInfo = authJson.optJSONObject("user_info")

            if (userInfo == null || userInfo.optString("auth", "0") == "0" && userInfo.optString("status") != "Active") {
                return@withContext fetchViaGetPhp(account, userAgent, onProgress)
            }

            // Categories map
            val liveCatMap = loadCategoryMap(server, u, p, "get_live_categories", userAgent)
            val vodCatMap = loadCategoryMap(server, u, p, "get_vod_categories", userAgent)
            val seriesCatMap = loadCategoryMap(server, u, p, "get_series_categories", userAgent)

            val results = ArrayList<Channel>(4096)

            // 1. Live streams
            val liveArray = loadArray(server, u, p, "get_live_streams", userAgent)
            val liveCount = minOf(liveArray.length(), maxLive)
            for (i in 0 until liveCount) {
                val obj = liveArray.getJSONObject(i)
                val streamId = obj.optString("stream_id")
                val name = obj.optString("name", "Channel $streamId")
                val catId = obj.optString("category_id")
                val icon = obj.optString("stream_icon")
                val categoryName = liveCatMap[catId] ?: "Live TV"
                val streamUrl = "$server/live/${enc(u)}/${enc(p)}/$streamId.m3u8"

                results.add(
                    Channel(
                        id = "xtream_live_$streamId",
                        name = name,
                        streamUrl = streamUrl,
                        logo = icon,
                        category = categoryName,
                        kind = StreamKind.LIVE
                    )
                )
                if (i % 250 == 0) onProgress?.invoke(results.size)
            }

            // 2. VOD streams
            val vodArray = loadArray(server, u, p, "get_vod_streams", userAgent)
            val vodCount = minOf(vodArray.length(), maxVod)
            for (i in 0 until vodCount) {
                val obj = vodArray.getJSONObject(i)
                val streamId = obj.optString("stream_id")
                val name = obj.optString("name", "Movie $streamId")
                val catId = obj.optString("category_id")
                val icon = obj.optString("stream_icon")
                val ext = obj.optString("container_extension", "mp4")
                val categoryName = vodCatMap[catId] ?: "Movies"
                val streamUrl = "$server/movie/${enc(u)}/${enc(p)}/$streamId.$ext"

                results.add(
                    Channel(
                        id = "xtream_vod_$streamId",
                        name = name,
                        streamUrl = streamUrl,
                        logo = icon,
                        category = categoryName,
                        kind = StreamKind.VOD
                    )
                )
                if (i % 250 == 0) onProgress?.invoke(results.size)
            }

            // 3. Series
            val seriesArray = loadArray(server, u, p, "get_series", userAgent)
            val seriesCount = minOf(seriesArray.length(), maxSeries)
            for (i in 0 until seriesCount) {
                val obj = seriesArray.getJSONObject(i)
                val seriesId = obj.optString("series_id")
                val name = obj.optString("name", "Series $seriesId")
                val catId = obj.optString("category_id")
                val icon = obj.optString("cover")
                val categoryName = seriesCatMap[catId] ?: "Series"
                val streamUrl = "$server/series/${enc(u)}/${enc(p)}/$seriesId.mp4"

                results.add(
                    Channel(
                        id = "xtream_series_$seriesId",
                        name = name,
                        streamUrl = streamUrl,
                        logo = icon,
                        category = categoryName,
                        kind = StreamKind.SERIES
                    )
                )
            }

            onProgress?.invoke(results.size)
            ImportResult(
                channels = results,
                liveCount = liveCount,
                vodCount = vodCount,
                seriesCount = seriesCount,
                message = "Successfully loaded full Xtream catalog (${results.size} streams)",
                success = true
            )
        } catch (e: Exception) {
            // Fallback to get.php M3U
            fetchViaGetPhp(account, userAgent, onProgress)
        }
    }

    suspend fun fetchViaGetPhp(
        account: Account,
        userAgent: String = "VLC/3.0.18 LibVLC/3.0.18",
        onProgress: ((Int) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        val server = normalizeServer(account.server)
        val u = account.username.trim()
        val p = account.password.trim()

        try {
            val getPhpUrl = "$server/get.php?username=${enc(u)}&password=${enc(p)}&type=m3u_plus&output=hls"
            val m3uContent = httpGet(getPhpUrl, userAgent = userAgent)
            val parseResult = M3uParser.parse(m3uContent, getPhpUrl, 100000, onProgress)

            val channels = parseResult.channels.map {
                Channel(
                    id = "m3u_${it.url.hashCode()}",
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

            val live = channels.count { it.kind == StreamKind.LIVE }
            val vod = channels.count { it.kind == StreamKind.VOD }
            val series = channels.count { it.kind == StreamKind.SERIES }

            ImportResult(
                channels = channels,
                liveCount = live,
                vodCount = vod,
                seriesCount = series,
                message = "Loaded ${channels.size} channels via get.php M3U",
                success = channels.isNotEmpty()
            )
        } catch (e: Exception) {
            ImportResult(
                channels = emptyList(),
                liveCount = 0,
                vodCount = 0,
                seriesCount = 0,
                message = "Connection error: ${e.localizedMessage ?: "Failed to reach Xtream server"}",
                success = false
            )
        }
    }

    private fun loadCategoryMap(
        server: String,
        u: String,
        p: String,
        action: String,
        userAgent: String
    ): Map<String, String> {
        return try {
            val url = "$server/player_api.php?username=${enc(u)}&password=${enc(p)}&action=$action"
            val jsonStr = httpGet(url, userAgent = userAgent)
            val arr = JSONArray(jsonStr)
            val map = HashMap<String, String>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("category_id")
                val name = obj.optString("category_name")
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    map[id] = name
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun loadArray(
        server: String,
        u: String,
        p: String,
        action: String,
        userAgent: String
    ): JSONArray {
        return try {
            val url = "$server/player_api.php?username=${enc(u)}&password=${enc(p)}&action=$action"
            val jsonStr = httpGet(url, userAgent = userAgent)
            JSONArray(jsonStr)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    fun httpGet(
        urlStr: String,
        userAgent: String = "VLC/3.0.18 LibVLC/3.0.18",
        connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS
    ): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = connectTimeoutMs
        conn.readTimeout = readTimeoutMs
        conn.setRequestProperty("User-Agent", userAgent)
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate")
        conn.setRequestProperty("Accept", "*/*")
        conn.instanceFollowRedirects = true

        val code = conn.responseCode
        if (code in 200..299) {
            val isGzip = "gzip".equals(conn.contentEncoding, ignoreCase = true)
            val rawStream: InputStream = conn.inputStream
            val stream = if (isGzip) GZIPInputStream(rawStream) else rawStream
            return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8), 65536).use { reader ->
                reader.readText()
            }
        } else {
            throw Exception("HTTP $code: ${conn.responseMessage}")
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
