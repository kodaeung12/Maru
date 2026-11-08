package com.project.maru

import retrofit2.Call
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
