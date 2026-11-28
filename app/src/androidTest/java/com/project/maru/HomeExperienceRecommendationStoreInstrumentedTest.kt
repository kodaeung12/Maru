package com.project.maru

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.maru.experience.location.EventCoordinates
import com.project.maru.experience.Experience
import com.project.maru.experience.recommendation.HomeExperienceRecommendationState
import com.project.maru.experience.recommendation.HomeExperienceRecommendationStore
import java.time.LocalDate
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeExperienceRecommendationStoreInstrumentedTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val file = "home_recommend_test_${UUID.randomUUID()}"
    private val context = object : ContextWrapper(app) {
        override fun getSharedPreferences(name: String, mode: Int) = app.getSharedPreferences(file, mode)
    }

    @After fun removeTestSnapshot() {
        app.deleteSharedPreferences(file)
    }

    @Test fun restoresBasicAndPersonalizedSnapshots() {
        assertNull(HomeExperienceRecommendationStore.read(context))
        val date = LocalDate.of(2026, 12, 20)
        val event = Experience("공예 체험", "시청", date.toString(), "체험",
            imageUrl = "https://example.com/event.png", targetAge = "6세", region = "중구",
            startDate = date, endDate = date, coordinates = EventCoordinates.fromApi("37.5665", "126.9780"), feeLabel = "무료")
        for (personalized in listOf(false, true)) {
            val state = HomeExperienceRecommendationState(personalized, listOf(event))
            HomeExperienceRecommendationStore.write(context, state, 100)
            assertEquals(HomeExperienceRecommendationStore.Saved(state, 100), HomeExperienceRecommendationStore.read(context))
        }
    }
}
