package com.project.maru

import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.project.maru.experience.location.EventCoordinates
import com.project.maru.experience.location.putExperienceMapCoordinates
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExperienceLocationUiInstrumentedTest {
    @Test fun noCoordinatesKeepsPlaceWithoutMap() {
        ActivityScenario.launch<DetailActivity>(detailIntent()).use {
            onView(withId(R.id.textDetailPlace)).check(matches(withText("서울시청")))
            onView(withId(R.id.experienceLocationPreview))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test fun opensLocationAndReturnsToDetail() {
        assumeTrue(BuildConfig.KAKAO_MAP_API_KEY.isNotBlank())
        val intent = detailIntent().apply {
            putExperienceMapCoordinates(requireNotNull(EventCoordinates.fromApi("37.5665", "126.9780")))
        }
        ActivityScenario.launch<DetailActivity>(intent).use {
            onView(withId(R.id.buttonOpenExperienceLocation)).perform(scrollTo(), click())
            onView(withId(R.id.textExperienceLocationTitle)).check(matches(withText("위치 확인 행사")))
            onView(withId(R.id.textExperienceLocationPlace)).check(matches(withText("서울시청")))
            onView(withId(R.id.buttonBackExperienceLocation)).perform(click())
            awaitDetailResume()
            onView(withId(R.id.textDetailPlace)).check(matches(withText("서울시청")))
        }
    }

    @Test fun previewTapAndButtonPreserveExperienceSelection() {
        assumeTrue(BuildConfig.KAKAO_MAP_API_KEY.isNotBlank())
        val intent = detailIntent()
            .putExtra("com.project.maru.EXPERIENCE_DETAIL_SOURCE", R.id.experienceFragment)
            .apply { putExperienceMapCoordinates(requireNotNull(EventCoordinates.fromApi("37.5665", "126.9780"))) }
        ActivityScenario.launch<DetailActivity>(intent).use {
            for (entry in listOf(R.id.buttonOpenExperienceLocationPreview, R.id.buttonOpenExperienceLocation)) {
                onView(withId(entry)).perform(scrollTo(), click())
                onView(withId(R.id.buttonBackExperienceLocation)).perform(click())
                awaitDetailResume()
                onView(withId(R.id.navEvent)).check(matches(isSelected()))
            }
            it.recreate()
            onView(withId(R.id.navEvent)).check(matches(isSelected()))
        }
    }

    private fun awaitDetailResume() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + 5_000
        var resumed = false
        while (!resumed && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED).any { it is DetailActivity }
            }
            if (!resumed) SystemClock.sleep(50)
        }
        assertTrue("행사 상세 화면으로 복귀해야 합니다.", resumed)
    }

    private fun detailIntent() = Intent(ApplicationProvider.getApplicationContext(), DetailActivity::class.java)
        .putExtra(DetailActivity.EXTRA_TITLE, "위치 확인 행사")
        .putExtra(DetailActivity.EXTRA_PLACE, "서울시청")
}
