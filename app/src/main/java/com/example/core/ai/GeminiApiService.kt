package com.example.core.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequestDto(
    @Json(name = "contents") val contents: List<ContentDto>
)

@JsonClass(generateAdapter = true)
data class ContentDto(
    @Json(name = "parts") val parts: List<PartDto>
)

@JsonClass(generateAdapter = true)
data class PartDto(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineDataDto? = null
)

@JsonClass(generateAdapter = true)
data class InlineDataDto(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponseDto(
    @Json(name = "candidates") val candidates: List<CandidateDto>? = null
)

@JsonClass(generateAdapter = true)
data class CandidateDto(
    @Json(name = "content") val content: ContentDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiModelsResponseDto(
    @Json(name = "models") val models: List<GeminiModelDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiModelDto(
    @Json(name = "name") val name: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "supportedGenerationMethods") val supportedGenerationMethods: List<String>? = null
)

interface GeminiApiService {
    @POST
    suspend fun generateContent(
        @Url endpoint: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequestDto
    ): GeminiResponseDto

    @GET("v1beta/models")
    suspend fun listModels(@Query("key") apiKey: String): GeminiModelsResponseDto
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}
