package com.project.maru

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SeoulCultureRetrofit {

    private const val BASE_URL = "http://openapi.seoul.go.kr:8088/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: SeoulCultureApiService =
        retrofit.create(SeoulCultureApiService::class.java)
}
