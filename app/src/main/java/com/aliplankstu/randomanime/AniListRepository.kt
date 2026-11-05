package com.aliplankstu.randomanime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object AniListRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun randomAnime(year: Int, month: Int, completed: Boolean): Anime? =
        withContext(Dispatchers.IO) {
            val found = mutableListOf<Anime>()
            var page = 1
            val pageSize = 50
            var hasNext = true

            // Month filtering is done locally because AniList's seasonYear is a year index.
            while (hasNext && page <= 20) {
                val query = """
                    query {
                      Page(page: $page, perPage: $pageSize) {
                        pageInfo { hasNextPage }
                        media(type: ANIME, seasonYear: $year, sort: POPULARITY_DESC, isAdult: false) {
                          id
                          title { romaji native }
                          coverImage { large extraLarge }
                          startDate { year month day }
                          status
                        }
                      }
                    }
                """.trimIndent()

                val pageData = requestPage(query)
                    .getJSONObject("data")
                    .getJSONObject("Page")
                hasNext = pageData.getJSONObject("pageInfo").optBoolean("hasNextPage", false)
                val media = pageData.getJSONArray("media")

                for (i in 0 until media.length()) {
                    val item = media.getJSONObject(i)
                    val start = item.optJSONObject("startDate") ?: continue
                    val itemYear = start.optInt("year", 0)
                    val itemMonth = start.optInt("month", 0)
                    if (itemYear != year || itemMonth !in 1..12) continue
                    if (month != 0 && itemMonth != month) continue

                    val isCompleted = item.optString("status") == "FINISHED"
                    if (completed != isCompleted) continue

                    val titles = item.optJSONObject("title") ?: JSONObject()
                    val title = titles.optString("romaji")
                        .takeIf { it.isNotBlank() } ?: "未命名番剧"
                    val native = titles.optString("native").takeIf { it.isNotBlank() }
                    val coverObj = item.optJSONObject("coverImage") ?: JSONObject()
                    val cover = coverObj.optString("extraLarge")
                        .takeIf { it.isNotBlank() }
                        ?: coverObj.optString("large").takeIf { it.isNotBlank() }

                    found.add(Anime(title, native, cover, itemYear, itemMonth, isCompleted))
                }
                page++
            }

            if (found.isEmpty()) null else found[Random.nextInt(found.size)]
        }

    private suspend fun requestPage(query: String): JSONObject {
        val body = JSONObject().put("query", query).toString().toRequestBody(jsonType)
        val request = Request.Builder()
            .url("https://graphql.anilist.co")
            .post(body)
            .header("Accept", "application/json")
            .header("User-Agent", "RandomAnime-Android")
            .build()

        var lastNetworkError: IOException? = null
        repeat(3) { attempt ->
            try {
                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()
                    if (response.code == 429 || response.code in 500..599) {
                        if (attempt == 2) {
                            throw IllegalStateException("AniList 服务暂时繁忙（HTTP ${response.code}），请稍后重试")
                        }
                        val waitSeconds = response.header("Retry-After")
                            ?.toLongOrNull()?.coerceIn(1, 8) ?: (attempt + 1L) * 2L
                        delay(waitSeconds * 1000L)
                        return@use
                    }
                    if (!response.isSuccessful) {
                        throw IllegalStateException("AniList 请求失败（HTTP ${response.code}）")
                    }
                    val root = JSONObject(responseText)
                    if (root.has("errors")) {
                        throw IllegalStateException("AniList 返回了异常数据，请稍后重试")
                    }
                    if (!root.has("data")) {
                        throw IllegalStateException("AniList 返回的数据不完整，请重试")
                    }
                    return root
                }
            } catch (e: IOException) {
                lastNetworkError = e
                if (attempt < 2) delay((attempt + 1L) * 1000L)
            }
        }
        throw IllegalStateException("网络连接失败，请检查网络后重试", lastNetworkError)
    }
}
