package com.project.maru

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.maru.experience.Experience
import com.project.maru.experience.recommendation.RecentExperienceStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RecentExperienceStoreInstrumentedTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val testPreferences = "recent_experience_test_${UUID.randomUUID()}"
    private val context = object : ContextWrapper(appContext) {
        override fun getSharedPreferences(name: String, mode: Int) =
            appContext.getSharedPreferences(testPreferences, mode)
    }

    @After
    fun deleteTestPreferences() {
        appContext.deleteSharedPreferences(testPreferences)
    }

    @Test
    fun savingSixEventsKeepsFiveMostRecentInOrder() {
        val events = (1..6).map(::event)
        events.forEach { RecentExperienceStore.recordView(context, it) }

        assertEquals(events.takeLast(5).reversed(), RecentExperienceStore.readRecent(context).map { it.experience })
    }

    @Test
    fun viewingSameEventAgainMovesUpdatedRecordToFrontWithoutDuplicate() {
        val events = (1..5).map(::event)
        events.forEach { RecentExperienceStore.recordView(context, it) }
        val updated = events[1].copy(title = "  행사   2  ", region = "강남구")
        RecentExperienceStore.recordView(context, updated)

        assertEquals(
            listOf(updated, events[4], events[3], events[2], events[0]),
            RecentExperienceStore.readRecent(context).map { it.experience }
        )
    }

    private fun event(index: Int) = Experience("행사 $index", "체험관", "2026-11-21", "체험")
}
