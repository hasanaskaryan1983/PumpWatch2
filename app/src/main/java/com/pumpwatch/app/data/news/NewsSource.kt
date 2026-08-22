package com.pumpwatch.app.data.news

import android.util.Xml
import com.pumpwatch.app.domain.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class NewsSource {

    private val cache = mutableMapOf<String, Pair<Long, List<NewsItem>>>()
    private val cacheLock = Any()
    private const val TTL = 15 * 60_000L

    suspend fun fetchNews(symbol: String): List<NewsItem> {
        val key = symbol.lowercase()

        synchronized(cacheLock) {
            cache[key]?.let { (timestamp, items) ->
                if (System.currentTimeMillis() - timestamp < TTL) {
                    return items
                }
            }
        }

        val items = withContext(Dispatchers.IO) {
            try {
                fetchFromRss(key)
            } catch (e: Exception) {
                emptyList()
            }
        }

        synchronized(cacheLock) {
            cache[key] = System.currentTimeMillis() to items
        }

        return items
    }

    private fun fetchFromRss(tag: String): List<NewsItem> {
        val url = URL("https://cointelegraph.com/rss/tag/$tag")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("User-Agent", "PumpWatch/1.0")

        return connection.inputStream.use { inputStream ->
            parseRss(inputStream)
        }
    }

    private fun parseRss(inputStream: InputStream): List<NewsItem> {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)

        val items = mutableListOf<NewsItem>()
        var inItem = false
        var currentTag = ""
        var title = ""
        var link = ""
        var pubDate = ""

        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                    if (currentTag.equals("item", ignoreCase = true)) {
                        inItem = true
                        title = ""
                        link = ""
                        pubDate = ""
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem) {
                        when (currentTag.lowercase()) {
                            "title" -> title += parser.text
                            "link" -> link += parser.text
                            "pubdate" -> pubDate += parser.text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (currentTag.equals("item", ignoreCase = true) && inItem) {
                        inItem = false
                        val publishedAt = runCatching {
                            dateFormat.parse(pubDate)?.time ?: 0L
                        }.getOrDefault(0L)

                        items.add(
                            NewsItem(
                                title = title.trim(),
                                url = link.trim(),
                                source = "Cointelegraph",
                                publishedAt = publishedAt,
                                sentiment = 0.0
                            )
                        )
                    }
                }
            }
            eventType = parser.next()
        }

        return items
    }
}
