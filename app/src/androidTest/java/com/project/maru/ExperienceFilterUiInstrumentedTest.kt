package com.project.maru

import android.os.SystemClock
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.project.maru.experience.Experience
import com.project.maru.experience.FavoriteRepository
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ExperienceFilterUiInstrumentedTest {
    @Test fun searchRegionCategoryAndPeriodWorkTogetherAfterLayoutMerge() {
        val uid = requireNotNull(FirebaseAuth.getInstance().currentUser).uid
        val prefix = "filter_" + UUID.randomUUID()
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        fun event(name: String, region: String, category: String, date: LocalDate) = Experience(
            "$prefix $name", "체험관", date.toString(), category, targetAge = "5세 이상", region = region,
            startDate = date, endDate = date)
        val events = listOf(
            event("과학 놀이", "종로구", "체험", today),
            event("과학 교육", "강남구", "교육", today.plusMonths(1)),
            event("미술 교육", "종로구", "교육", today))
        try {
            events.forEach { Tasks.await(FavoriteRepository.setSaved(uid, it, true), 30, TimeUnit.SECONDS) }
            ActivityScenario.launch(HomeActivity::class.java).use {
                onView(withId(R.id.btnFavorite)).perform(click())
                onView(withId(R.id.editSearch)).perform(replaceText(prefix), closeSoftKeyboard())
                count(3)
                onView(withId(R.id.editSearch)).perform(replaceText("$prefix 과학"), closeSoftKeyboard())
                count(2)
                onView(withId(R.id.filterRegion)).perform(click())
                onData(equalTo("종로구")).perform(click())
                count(1)
                onView(withId(R.id.filterRegion)).perform(click())
                onView(withText("전체 지역")).perform(click())
                count(2)
                onView(withId(R.id.filterCategory)).perform(click())
                onView(withText(R.string.experience_list_category_education)).perform(click())
                count(1)
                onView(withId(R.id.filterPeriod)).perform(click())
                onView(withText(R.string.experience_period_this_month)).perform(click())
                count(0)
                onView(withId(R.id.filterPeriod)).perform(click())
                onView(withText(R.string.experience_period_all)).perform(click())
                count(1)
                onView(allOf(withId(R.id.textTitle), withText(events[1].title))).perform(click())
                onView(withId(R.id.textDetailTitle)).check(matches(withText(events[1].title)))
            }
        } finally {
            events.forEach { Tasks.await(FavoriteRepository.setSaved(uid, it, false), 30, TimeUnit.SECONDS) }
        }
    }

    private fun count(expected: Int) {
        onView(withId(R.id.recyclerExperience)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()
            override fun getDescription() = "필터 결과 $expected 개 확인"
            override fun perform(controller: UiController, view: View) {
                val list = view as RecyclerView
                val deadline = SystemClock.elapsedRealtime() + 15000
                while (list.adapter?.itemCount != expected && SystemClock.elapsedRealtime() < deadline) {
                    controller.loopMainThreadForAtLeast(50)
                }
                assertTrue("검색/필터 결과가 예상과 다릅니다.", list.adapter?.itemCount == expected)
            }
        })
    }
}
