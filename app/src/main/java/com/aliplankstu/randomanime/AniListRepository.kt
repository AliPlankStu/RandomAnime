package com.aliplankstu.randomanime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object AniListRepository {
    private val client = OkHttpClient.Builder()
        .callTimeout(35, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun randomAnime(year: Int, month: Int, completed: Boolean): Anime? =
        withContext(Dispatchers.IO) {
            val found = mutableListOf<Anime>()
            var page = 1
            val pageSize = 50
            var hasNext = true

            // AniList uses seasonYear as its year index. Fetch all pages for that year,
            // then apply the exact calendar-month filter locally.
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
                val body = JSONObject().put("query", query).toString()
                    .toRequestBody(jsonType)
                val request = Request.Builder()
                    .url("https://graphql.anilist.co")
                    .post(body)
                    .header("Accept", "application/json")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IllegalStateException("AniList HTTP ${response.code}")
                    val root = JSONObject(response.body?.string() ?: "{}")
                    if (root.has("errors")) throw IllegalStateException("AniList 暂时无法提供数据")
                    val pageData = root.getJSONObject("data").getJSONObject("Page")
                    hasNext = pageData.getJSONObject("pageInfo").getBoolean("hasNextPage")
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
                        val title = titles.optString("romaji").takeIf { it.isNotBlank() } ?: "未命名番剧"
                        val native = titles.optString("native").takeIf { it.isNotBlank() }
                        val coverObj = item.optJSONObject("coverImage") ?: JSONObject()
                        val cover = coverObj.optString("extraLarge")
                            .takeIf { it.isNotBlank() } ?: coverObj.optString("large").takeIf { it.isNotBlank() }
                        found.add(Anime(title, native, cover, itemYear, itemMonth, isCompleted))
                    }
                    page++
                }
            }
            if (found.isEmpty()) null else found[Random.nextInt(found.size)]
        }
}
