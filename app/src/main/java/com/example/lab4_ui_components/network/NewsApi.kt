package com.example.lab4_ui_components.network

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Общий ответ сервера
data class HackerNewsResponse(
    val hits: List<ApiNews> = emptyList()
)

// Одна публикация из JSON
data class ApiNews(
    @SerializedName("objectID")
    val objectId: String = "",

    val title: String? = null,

    @SerializedName("story_title")
    val storyTitle: String? = null,

    @SerializedName("story_text")
    val storyText: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    val author: String? = null,
    val url: String? = null
)

// Описание REST API
interface NewsApiService {

    @GET("api/v1/search")
    suspend fun getTopNews(
        @Query("tags") tags: String = "front_page",
        @Query("hitsPerPage") limit: Int = 20
    ): HackerNewsResponse
}

// Настройка Retrofit
object NewsApiClient {

    private const val BASE_URL =
        "https://hn.algolia.com/"

    val service: NewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(NewsApiService::class.java)
    }
}
