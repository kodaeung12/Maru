package com.project.maru

import android.content.Context
import android.os.SystemClock
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.maru.experience.Experience
import com.project.maru.experience.RecentExperienceStore
import com.project.maru.experience.SimilarEventsActivity
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimilarEventsActivityInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = context.getSharedPreferences("recent_experiences", Context.MODE_PRIVATE)
    private var previousHistory: String? = null
    private val viewed = Experience("도자기 만들기", "체험관", "2026-11-21", "체험", region = "종로구")
    private val candidate = viewed.copy(title = "도자기 공방")

    @Before
    fun isolateHistory() {
        previousHistory = preferences.getString("history", null)
        preferences.edit().remove("history").commit()
    }

    @After
    fun restoreHistory() {
        preferences.edit().putString("history", previousHistory).commit()
    }

    @Test
    fun showsRecommendationAndOpensItsDetail() {
        RecentExperienceStore.recordView(context, viewed)
        launch(listOf(viewed, candidate)).use {
            onView(allOf(withId(R.id.textTitle), withText(candidate.title)))
                .check(matches(isDisplayed()))
                .perform(click())
            onView(withId(R.id.textDetailTitle)).check(matches(withText(candidate.title)))
            assertEquals(candidate, RecentExperienceStore.readRecent(context).first().experience)
        }
    }

    @Test
    fun showsNoHistoryMessage() {
        launch(listOf(candidate)).use {
            onView(withId(R.id.textEmptyRecommendation))
                .check(matches(allOf(isDisplayed(), withText(R.string.similar_events_no_history))))
        }
    }

    @Test
    fun showsNoMatchesMessageWhenOnlyViewedCandidateIsAvailable() {
        RecentExperienceStore.recordView(context, viewed)
        launch(listOf(viewed)).use {
            onView(withId(R.id.textEmptyRecommendation))
                .check(matches(allOf(isDisplayed(), withText(R.string.similar_events_no_matches))))
        }
    }

    @Test
    fun showsUnavailableMessageWhenCandidateLoadFails() {
        RecentExperienceStore.recordView(context, viewed)
        launch(null).use {
            onView(withId(R.id.textEmptyRecommendation))
                .check(matches(allOf(isDisplayed(), withText(R.string.similar_events_candidates_unavailable))))
        }
    }

    @Test
    fun opensRecommendationFromLiveSeoulApiList() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveSeoulApi") == "true")
        assumeTrue(BuildConfig.SEOUL_API_KEY.isNotBlank())
        RecentExperienceStore.recordView(context, viewed)

        ActivityScenario.launch(ExperienceListActivity::class.java).use { scenario ->
            val deadline = SystemClock.elapsedRealtime() + 20_000
            var ready = false
            while (!ready && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity { ready = it.findViewById<Button>(R.id.btnSimilarEvents).isEnabled }
                if (!ready) SystemClock.sleep(100)
            }
            assertTrue("Seoul API request did not finish before the timeout", ready)
            onView(withId(R.id.btnSimilarEvents)).perform(click())
            onView(withId(R.id.textRecommendationTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.recyclerSimilarEvents)).check(matches(isDisplayed()))
            onView(withText(R.string.similar_events_candidates_unavailable)).check(doesNotExist())
        }
    }

    private fun launch(candidates: List<Experience>?) = ActivityScenario.launch<SimilarEventsActivity>(
        SimilarEventsActivity.createIntent(context, candidates)
    )
}
