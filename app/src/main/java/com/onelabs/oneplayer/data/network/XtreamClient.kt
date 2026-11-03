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
    const val DEFAULT_READ_TIMEOUT_MS = 300000   // 300s (5 minutes for 25,000+ items)

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

    data class HttpStreamResponse(
        val connection: HttpURLConnection,
        val stream: InputStream,
        val isGzip: Boolean
    ) : AutoCloseable {
        override fun close() {
            try { stream.close() } catch (_: Exception) {}
            try { connection.disconnect() } catch (_: Exception) {}
        }
    }

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
        maxLimit: Int = 250000,
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

        // 1. First authenticate with player_api to verify account
        var authValid = false
        try {
            val authUrl = "$server/player_api.php?username=${enc(u)}&password=${enc(p)}"
            val authJsonStr = httpGet(authUrl, userAgent = userAgent, connectTimeoutMs = 30000, readTimeoutMs = 30000)
            val authJson = JSONObject(authJsonStr)
            val userInfo = authJson.optJSONObject("user_info")
            if (userInfo != null) {
                val authVal = userInfo.optString("auth", "1")
                val status = userInfo.optString("status", "Active")
                if (authVal != "0" && status.equals("Active", ignoreCase = true)) {
                    authValid = true
                }
            }
        } catch (_: Exception) {
            // Some servers block player_api auth query or only expose get.php; proceed to catalog fetch
        }

        // 2. Fetch via full get.php streaming engine (standard for 25,000+ items on Xtream)
        val getPhpResult = fetchViaGetPhpStream(server, u, p, userAgent, maxLimit, onProgress)
        if (getPhpResult.channels.size >= 5000 || (getPhpResult.channels.isNotEmpty() && getPhpResult.vodCount > 0)) {
            // Successfully got the full unified catalog!
            return@withContext getPhpResult
        }

        // 3. If get.php returned only live channels or was blocked, supplement/fallback with player_api.php
        val apiResults = fetchViaPlayerApi(server, u, p, userAgent, maxLimit, onProgress)

        // 4. Merge results if both have entries (e.g. get.php had live, player_api had VOD/Series or vice versa)
        val mergedMap = LinkedHashMap<String, Channel>(apiResults.channels.size + getPhpResult.channels.size)
        for (ch in getPhpResult.channels) {
            mergedMap[ch.streamUrl] = ch
        }
        for (ch in apiResults.channels) {
            mergedMap[ch.streamUrl] = ch
        }

        val finalChannels = ArrayList(mergedMap.values)
        if (finalChannels.isEmpty()) {
            return@withContext if (!authValid) {
                ImportResult(
                    channels = emptyList(),
                    liveCount = 0,
                    vodCount = 0,
                    seriesCount = 0,
                    message = "Could not connect to Xtream server. Check server address and credentials.",
                    success = false
                )
            } else {
                ImportResult(
                    channels = emptyList(),
                    liveCount = 0,
                    vodCount = 0,
                    seriesCount = 0,
                    message = "Xtream account is active, but no streams were found.",
                    success = false
                )
            }
        }

        val liveCount = finalChannels.count { it.kind == StreamKind.LIVE }
        val vodCount = finalChannels.count { it.kind == StreamKind.VOD }
        val seriesCount = finalChannels.count { it.kind == StreamKind.SERIES }

        onProgress?.invoke(finalChannels.size)
        ImportResult(
            channels = finalChannels,
            liveCount = liveCount,
            vodCount = vodCount,
            seriesCount = seriesCount,
            message = "Successfully loaded all ${finalChannels.size} items ($liveCount Live, $vodCount Movies, $seriesCount Series)",
            success = true
        )
    }

    private suspend fun fetchViaGetPhpStream(
        server: String,
        u: String,
        p: String,
        userAgent: String,
        maxLimit: Int,
        onProgress: ((Int) -> Unit)?
    ): ImportResult = withContext(Dispatchers.IO) {
        // Try standard m3u_plus first, then fallback to ts
        val urlVariants = listOf(
            "$server/get.php?username=${enc(u)}&password=${enc(p)}&type=m3u_plus",
            "$server/get.php?username=${enc(u)}&password=${enc(p)}&type=m3u_plus&output=ts",
            "$server/get.php?username=${enc(u)}&password=${enc(p)}&type=m3u_plus&output=m3u8"
        )

        for (urlStr in urlVariants) {
            try {
                openHttpStream(urlStr, userAgent).use { resp ->
                    val parseResult = M3uParser.parseFromInputStream(
                        inputStream = resp.stream,
                        isGzip = resp.isGzip,
                        sourceUrl = urlStr,
                        maxLimit = maxLimit,
                        onProgress = onProgress
                    )

                    if (parseResult.channels.isNotEmpty()) {
                        val channels = parseResult.channels.mapIndexed { idx, it ->
                            Channel(
                                id = "xtream_${it.url.hashCode()}_$idx",
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

                        return@withContext ImportResult(
                            channels = channels,
                            liveCount = live,
                            vodCount = vod,
                            seriesCount = series,
                            message = "Successfully loaded all ${channels.size} items ($live Live, $vod Movies, $series Series)",
                            success = true
                        )
                    }
                }
            } catch (_: Exception) {
                // Try next variant
            }
        }

        ImportResult(
            channels = emptyList(),
            liveCount = 0,
            vodCount = 0,
            seriesCount = 0,
            message = "Failed to stream catalog from get.php",
            success = false
        )
    }

    private suspend fun fetchViaPlayerApi(
        server: String,
        u: String,
        p: String,
        userAgent: String,
        maxLimit: Int,
        onProgress: ((Int) -> Unit)?
    ): ImportResult = withContext(Dispatchers.IO) {
        val results = ArrayList<Channel>(8192)

        // Categories map
        val liveCatMap = loadCategoryMap(server, u, p, "get_live_categories", userAgent)
        val vodCatMap = loadCategoryMap(server, u, p, "get_vod_categories", userAgent)
        val seriesCatMap = loadCategoryMap(server, u, p, "get_series_categories", userAgent)

        // 1. Live streams
        val liveArray = loadArray(server, u, p, "get_live_streams", userAgent)
        val liveCount = liveArray.length()
        for (i in 0 until liveCount) {
            val obj = liveArray.getJSONObject(i)
            val streamId = obj.optString("stream_id")
            val name = obj.optString("name", "Channel $streamId")
            val catId = obj.optString("category_id")
            val icon = obj.optString("stream_icon")
            val categoryName = liveCatMap[catId] ?: "Live TV"
            val streamUrl = "$server/live/${enc(u)}/${enc(p)}/$streamId.ts"

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
            if (results.size % 500 == 0) onProgress?.invoke(results.size)
            if (results.size >= maxLimit) break
        }

        // 2. VOD streams (All movies)
        var vodCount = 0
        val vodArray = loadArray(server, u, p, "get_vod_streams", userAgent)
        if (vodArray.length() > 0) {
            vodCount = vodArray.length()
            for (i in 0 until vodCount) {
                val obj = vodArray.getJSONObject(i)
                val streamId = obj.optString("stream_id")
                val name = obj.optString("name", "Movie $streamId")
                val catId = obj.optString("category_id")
                val icon = obj.optString("stream_icon")
                val ext = obj.optString("container_extension", "mp4").ifEmpty { "mp4" }
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
                if (results.size % 500 == 0) onProgress?.invoke(results.size)
                if (results.size >= maxLimit) break
            }
        } else if (vodCatMap.isNotEmpty()) {
            // Some Xtream servers disable global get_vod_streams; load by category
            for ((catId, catName) in vodCatMap) {
                if (results.size >= maxLimit) break
                val catArray = loadArray(server, u, p, "get_vod_streams&category_id=$catId", userAgent)
                vodCount += catArray.length()
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    val name = obj.optString("name", "Movie $streamId")
                    val icon = obj.optString("stream_icon")
                    val ext = obj.optString("container_extension", "mp4").ifEmpty { "mp4" }
                    val streamUrl = "$server/movie/${enc(u)}/${enc(p)}/$streamId.$ext"

                    results.add(
                        Channel(
                            id = "xtream_vod_$streamId",
                            name = name,
                            streamUrl = streamUrl,
                            logo = icon,
                            category = catName,
                            kind = StreamKind.VOD
                        )
                    )
                    if (results.size % 500 == 0) onProgress?.invoke(results.size)
                    if (results.size >= maxLimit) break
                }
            }
        }

        // 3. Series (All TV Series)
        var seriesCount = 0
        val seriesArray = loadArray(server, u, p, "get_series", userAgent)
        if (seriesArray.length() > 0) {
            seriesCount = seriesArray.length()
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
                if (results.size % 500 == 0) onProgress?.invoke(results.size)
                if (results.size >= maxLimit) break
            }
        } else if (seriesCatMap.isNotEmpty()) {
            // Load series by category if global array returned empty
            for ((catId, catName) in seriesCatMap) {
                if (results.size >= maxLimit) break
                val catArray = loadArray(server, u, p, "get_series&category_id=$catId", userAgent)
                seriesCount += catArray.length()
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    val seriesId = obj.optString("series_id")
                    val name = obj.optString("name", "Series $seriesId")
                    val icon = obj.optString("cover")
                    val streamUrl = "$server/series/${enc(u)}/${enc(p)}/$seriesId.mp4"

                    results.add(
                        Channel(
                            id = "xtream_series_$seriesId",
                            name = name,
                            streamUrl = streamUrl,
                            logo = icon,
                            category = catName,
                            kind = StreamKind.SERIES
                        )
                    )
                    if (results.size % 500 == 0) onProgress?.invoke(results.size)
                    if (results.size >= maxLimit) break
                }
            }
        }

        val actualLive = results.count { it.kind == StreamKind.LIVE }
        val actualVod = results.count { it.kind == StreamKind.VOD }
        val actualSeries = results.count { it.kind == StreamKind.SERIES }

        ImportResult(
            channels = results,
            liveCount = actualLive,
            vodCount = actualVod,
            seriesCount = actualSeries,
            message = "Loaded ${results.size} streams from player_api ($actualLive Live, $actualVod Movies, $actualSeries Series)",
            success = results.isNotEmpty()
        )
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
            val jsonStr = httpGet(url, userAgent = userAgent, connectTimeoutMs = 30000, readTimeoutMs = 45000)
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
            val jsonStr = httpGet(url, userAgent = userAgent, connectTimeoutMs = 45000, readTimeoutMs = 180000)
            JSONArray(jsonStr)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    fun openHttpStream(
        urlStr: String,
        userAgent: String = "VLC/3.0.18 LibVLC/3.0.18",
        connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS
    ): HttpStreamResponse {
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
            return HttpStreamResponse(conn, conn.inputStream, isGzip)
        } else {
            conn.disconnect()
            throw Exception("HTTP $code: ${conn.responseMessage}")
        }
    }

    fun httpGet(
        urlStr: String,
        userAgent: String = "VLC/3.0.18 LibVLC/3.0.18",
        connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS
    ): String {
        openHttpStream(urlStr, userAgent, connectTimeoutMs, readTimeoutMs).use { resp ->
            val stream = if (resp.isGzip) GZIPInputStream(resp.stream) else resp.stream
            return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8), 65536).use { reader ->
                reader.readText()
            }
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
