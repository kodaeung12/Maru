package com.project.maru.experience

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface SeoulCultureApiService {

    @GET("{key}/json/aiViewCulture/{start}/{end}/")
    fun getCultureEvents(
        @Path("key") apiKey: String,
        @Path("start") start: Int,
        @Path("end") end: Int
    ): Call<SeoulCultureResponse>
}

object SeoulCultureRetrofit {

    private const val BASE_URL = "http://openapi.seoul.go.kr:8088/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: SeoulCultureApiService =
        retrofit.create(SeoulCultureApiService::class.java)
}

data class SeoulCultureResponse(
    val aiViewCulture: SeoulCultureData
)

data class SeoulCultureData(
    val list_total_count: Int,
    val row: List<SeoulCultureEvent>
)

data class SeoulCultureEvent(
    val TITLE: String,
    val DATE: String,
    val PLACE: String,
    val CODENAME: String,
    val GUNAME: String,
    val USE_TRGT: String,
    val MAIN_IMG: String,
    val STRTDATE: String? = null,
    val END_DATE: String? = null
)
