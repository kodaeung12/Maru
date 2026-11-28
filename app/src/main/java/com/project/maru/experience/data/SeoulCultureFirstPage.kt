package com.project.maru.experience.data

import com.project.maru.experience.Experience
import com.project.maru.experience.list.isSuitableForAge5To8
import com.project.maru.experience.list.toExperience

import com.project.maru.BuildConfig
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

internal const val SEOUL_CULTURE_PAGE_SIZE = 1000

internal object SeoulCultureFirstPage {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var firstRequest: Deferred<SeoulCultureData?>? = null

    suspend fun load(): SeoulCultureData? {
        val request = request()
        val data = request.await()
        if (data == null) {
            synchronized(this) { if (firstRequest === request) firstRequest = null }
        }
        return data
    }

    @Synchronized
    private fun request(): Deferred<SeoulCultureData?> = firstRequest ?: scope.async(start = CoroutineStart.LAZY) {
        if (BuildConfig.SEOUL_API_KEY.isBlank()) return@async null
        try {
            val response = SeoulCultureRetrofit.apiService
                .getCultureEvents(BuildConfig.SEOUL_API_KEY, 1, SEOUL_CULTURE_PAGE_SIZE)
                .execute()
            response.body()?.aiViewCulture?.takeIf {
                response.isSuccessful && (it.row.isNotEmpty() || it.list_total_count == 0)
            }
        } catch (_: IOException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }.also { firstRequest = it }
}

internal fun SeoulCultureData.familyExperiences(): List<Experience> =
    row.map { it.toExperience() }.filter { isSuitableForAge5To8(it.targetAge) }
