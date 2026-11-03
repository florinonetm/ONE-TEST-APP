package com.onelabs.oneplayer.data.parser

import com.onelabs.oneplayer.data.model.Channel
import com.onelabs.oneplayer.data.model.StreamKind
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.GZIPInputStream

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

    fun parse(
        content: String,
        sourceUrl: String = "",
        maxLimit: Int = 250000,
        onProgress: ((Int) -> Unit)? = null
    ): Result {
        return parseStream(
            BufferedReader(StringReader(cleanContent(content)), 65536),
            sourceUrl,
            maxLimit,
            onProgress
        )
    }

    fun parseFromInputStream(
        inputStream: InputStream,
        isGzip: Boolean = false,
        sourceUrl: String = "",
        maxLimit: Int = 250000,
        onProgress: ((Int) -> Unit)? = null
    ): Result {
        val stream = if (isGzip) GZIPInputStream(inputStream) else inputStream
        val reader = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8), 65536)
        return parseStream(reader, sourceUrl, maxLimit, onProgress)
    }

    private fun cleanContent(content: String): String {
        // Strip UTF-8 BOM if present
        return if (content.startsWith("\uFEFF")) {
            content.substring(1)
        } else {
            content
        }
    }

    fun parseStream(
        reader: BufferedReader,
        sourceUrl: String = "",
        maxLimit: Int = 250000,
        onProgress: ((Int) -> Unit)? = null
    ): Result {
        val channels = ArrayList<Entry>(8192)
        var line: String?
        var currentTvgName = ""
        var currentGroup = "General"
        var currentLogo = ""
        var currentDuration = -1
        var isMasterHls = false
        var count = 0

        while (reader.readLine().also { line = it } != null) {
            val raw = line ?: continue
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) {
                isMasterHls = true
            }

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                currentDuration = extractDuration(trimmed)
                val tvgName = extractAttr(trimmed, "tvg-name")
                val groupTitle = extractAttr(trimmed, "group-title")
                val logo = extractAttr(trimmed, "tvg-logo")

                if (groupTitle.isNotEmpty()) currentGroup = groupTitle
                if (logo.isNotEmpty()) currentLogo = logo
                currentTvgName = tvgName.ifEmpty { extractTitleFromLine(trimmed) }
            } else if (trimmed.startsWith("#EXTGRP:", ignoreCase = true)) {
                val grp = trimmed.substringAfter(":", "").trim()
                if (grp.isNotEmpty()) currentGroup = grp
            } else if (!trimmed.startsWith("#")) {
                val streamUrl = resolveUrl(trimmed, sourceUrl)
                if (isStreamUrl(streamUrl) || trimmed.contains("://")) {
                    val finalName = currentTvgName.ifEmpty { guessNameFromUrl(streamUrl) }
                    val kind = detectKind(currentGroup, streamUrl, finalName)

                    channels.add(
                        Entry(
                            name = finalName,
                            url = streamUrl,
                            group = currentGroup,
                            logo = currentLogo,
                            duration = currentDuration,
                            kind = kind
                        )
                    )
                    count++
                    if (count % 500 == 0) {
                        onProgress?.invoke(count)
                    }

                    if (channels.size >= maxLimit) break

                    // Reset for next entry
                    currentTvgName = ""
                    currentGroup = "General"
                    currentLogo = ""
                    currentDuration = -1
                }
            }
        }

        onProgress?.invoke(channels.size)
        return Result(channels = channels, isMaster = isMasterHls, total = channels.size)
    }

    private fun detectKind(group: String, url: String, name: String = ""): Kind {
        val gLower = group.lowercase()
        val uLower = url.lowercase()
        val nLower = name.lowercase()

        // 1. URL Path conventions (most accurate for Xtream & IPTV)
        if (uLower.contains("/series/")) return Kind.SERIES
        if (uLower.contains("/movie/")) return Kind.VOD
        if (uLower.contains("/live/")) return Kind.LIVE

        // 2. File extensions (VOD files like .mp4, .mkv, .avi)
        if (uLower.endsWith(".mp4") || uLower.endsWith(".mkv") || uLower.endsWith(".avi")) {
            // Check if it has episode pattern S01E01
            if (Regex("""(?i)[sS]\d{1,2}\s*[eE]\d{1,3}""").containsMatchIn(nLower) ||
                gLower.contains("series") || gLower.contains("serial")
            ) {
                return Kind.SERIES
            }
            return Kind.VOD
        }

        // 3. Category & Group names
        if (gLower.contains("series") || gLower.contains("serial") || gLower.contains("shows") ||
            gLower.contains("season") || gLower.contains("episoade")
        ) {
            return Kind.SERIES
        }

        if (gLower.contains("movie") || gLower.contains("vod") || gLower.contains("cinema") ||
            gLower.contains("film") || gLower.contains("filme") || gLower.contains("pelicula")
        ) {
            return Kind.VOD
        }

        // 4. Name patterns (e.g. "Breaking Bad S01 E01")
        if (Regex("""(?i)[sS]\d{1,2}\s*[eE]\d{1,3}""").containsMatchIn(nLower)) {
            return Kind.SERIES
        }

        return Kind.LIVE
    }

    // High performance attribute extraction without regex
    private fun extractAttr(line: String, attrName: String): String {
        val target = "$attrName="
        var idx = line.indexOf(target, ignoreCase = true)
        if (idx == -1) return ""
        idx += target.length

        if (idx >= line.length) return ""
        val quoteChar = line[idx]
        return if (quoteChar == '"' || quoteChar == '\'') {
            val endIdx = line.indexOf(quoteChar, idx + 1)
            if (endIdx != -1) line.substring(idx + 1, endIdx).trim() else ""
        } else {
            val endIdx = line.indexOfAny(charArrayOf(' ', ',', '\t'), idx)
            if (endIdx != -1) line.substring(idx, endIdx).trim() else line.substring(idx).trim()
        }
    }

    private fun extractDuration(line: String): Int {
        val afterColon = line.substringAfter(":", "").trim()
        var i = 0
        while (i < afterColon.length && (afterColon[i] == '-' || afterColon[i].isDigit())) {
            i++
        }
        val numStr = afterColon.substring(0, i)
        return numStr.toIntOrNull() ?: -1
    }

    private fun extractTitleFromLine(line: String): String {
        val commaIdx = line.lastIndexOf(',')
        return if (commaIdx != -1 && commaIdx < line.length - 1) {
            line.substring(commaIdx + 1).trim()
        } else {
            ""
        }
    }

    fun guessNameFromUrl(url: String): String {
        return try {
            val path = java.net.URL(url).path
            val lastSegment = path.split("/").filter { it.isNotEmpty() }.lastOrNull()
            lastSegment?.replace(Regex("""\.(m3u8?|mpd|ts|mp4|mkv)$""", RegexOption.IGNORE_CASE), "")
                ?.replace("_", " ")
                ?.replace("-", " ")
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
        val cleanUrl = url.replace(" ", "%20")
        if (cleanUrl.contains("://") || baseUrl.isEmpty()) return cleanUrl
        return try {
            java.net.URL(java.net.URL(baseUrl), cleanUrl).toString()
        } catch (_: Exception) {
            cleanUrl
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
