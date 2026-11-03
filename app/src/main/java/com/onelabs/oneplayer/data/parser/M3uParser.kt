package com.onelabs.oneplayer.data.parser

import com.onelabs.oneplayer.data.model.Channel
import com.onelabs.oneplayer.data.model.StreamKind
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

object M3uParser {

    enum class Kind {
        LIVE, VOD, SERIES
    }

    data class Entry(
        val name: String,
        val url: String,
        val group: String,
        val logo: String,
        val duration: Int = -1,
        val kind: Kind = Kind.LIVE,
        val resolution: String = "",
        val bandwidth: Int = 0
    )

    data class Result(
        val channels: List<Entry>,
        val isMaster: Boolean = false,
        val total: Int = channels.size
    )

    fun parse(content: String, sourceUrl: String = "", maxLimit: Int = 5000): Result {
        return parseStream(BufferedReader(StringReader(content)), sourceUrl, maxLimit)
    }

    fun parseStream(reader: BufferedReader, sourceUrl: String = "", maxLimit: Int = 5000): Result {
        val channels = mutableListOf<Entry>()
        var line: String?
        var currentTvgName = ""
        var currentGroup = "General"
        var currentLogo = ""
        var currentDuration = -1
        var isMasterHls = false

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXT-X-STREAM-INF")) {
                isMasterHls = true
            }

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                currentDuration = parseDuration(trimmed)
                currentTvgName = parseQuotedAttr(trimmed, "tvg-name")
                    .ifEmpty { parseTitle(trimmed) }
                val tvgLogo = parseQuotedAttr(trimmed, "tvg-logo")
                if (tvgLogo.isNotEmpty()) currentLogo = tvgLogo
                val groupTitle = parseQuotedAttr(trimmed, "group-title")
                if (groupTitle.isNotEmpty()) currentGroup = groupTitle
            } else if (!trimmed.startsWith("#")) {
                val streamUrl = resolveUrl(trimmed, sourceUrl)
                if (isStreamUrl(streamUrl) || trimmed.contains("://")) {
                    val name = currentTvgName.ifEmpty { guessNameFromUrl(streamUrl) }
                    val kind = when {
                        currentGroup.contains("movie", ignoreCase = true) ||
                                streamUrl.contains("/movie/", ignoreCase = true) -> Kind.VOD
                        currentGroup.contains("series", ignoreCase = true) ||
                                streamUrl.contains("/series/", ignoreCase = true) -> Kind.SERIES
                        else -> Kind.LIVE
                    }
                    channels.add(
                        Entry(
                            name = name,
                            url = streamUrl,
                            group = currentGroup,
                            logo = currentLogo,
                            duration = currentDuration,
                            kind = kind
                        )
                    )
                    if (channels.size >= maxLimit) break

                    // Reset for next entry
                    currentTvgName = ""
                    currentGroup = "General"
                    currentLogo = ""
                    currentDuration = -1
                }
            }
        }

        return Result(channels = channels, isMaster = isMasterHls, total = channels.size)
    }

    private fun parseDuration(extinf: String): Int {
        val afterColon = extinf.substringAfter(":", "").trim()
        val numStr = afterColon.takeWhile { it == '-' || it.isDigit() }
        return numStr.toIntOrNull() ?: -1
    }

    private fun parseQuotedAttr(line: String, attrName: String): String {
        val pattern = """$attrName="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(line)
        if (match != null) return match.groupValues[1]

        val singleQuotePattern = """$attrName='([^']*)'""".toRegex(RegexOption.IGNORE_CASE)
        val singleMatch = singleQuotePattern.find(line)
        if (singleMatch != null) return singleMatch.groupValues[1]

        val rawPattern = """$attrName=([^, ]+)""".toRegex(RegexOption.IGNORE_CASE)
        return rawPattern.find(line)?.groupValues?.get(1) ?: ""
    }

    private fun parseTitle(extinf: String): String {
        return extinf.substringAfterLast(",").trim()
    }

    fun guessNameFromUrl(url: String): String {
        return try {
            val path = java.net.URL(url).path
            val lastSegment = path.split("/").filter { it.isNotEmpty() }.lastOrNull()
            lastSegment?.replace(Regex("""\.(m3u8?|mpd|ts|mp4|mkv)$""", RegexOption.IGNORE_CASE), "")
                ?.replace("_", " ")
                ?: "Stream Channel"
        } catch (_: Exception) {
            "Stream Channel"
        }
    }

    fun isStreamUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.startsWith("rtmp://") ||
                lower.startsWith("rtsp://")
    }

    fun resolveUrl(url: String, baseUrl: String): String {
        if (url.contains("://") || baseUrl.isEmpty()) return url
        return try {
            java.net.URL(java.net.URL(baseUrl), url).toString()
        } catch (_: Exception) {
            url
        }
    }

    fun Entry.toChannel(): Channel {
        return Channel(
            id = UUID.randomUUID().toString(),
            name = name,
            streamUrl = url,
            logo = logo,
            category = group,
            kind = when (kind) {
                Kind.LIVE -> StreamKind.LIVE
                Kind.VOD -> StreamKind.VOD
                Kind.SERIES -> StreamKind.SERIES
            }
        )
    }
}
