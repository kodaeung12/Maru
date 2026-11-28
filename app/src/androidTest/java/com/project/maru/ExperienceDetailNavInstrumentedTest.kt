package com.project.maru

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.project.maru.experience.Experience
import com.project.maru.experience.experienceDetailIntent
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExperienceDetailNavInstrumentedTest {
    @Test fun detailUsesSelectedHomeTab() {
        val event = Experience("탭 전달 검증", "장소", "기간", "체험")
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            val cases = listOf(
                R.id.homeFragment to R.id.navHome,
                R.id.experienceFragment to R.id.navEvent,
                R.id.myPageFragment to R.id.navMypage
            )
            for ((source, detailTab) in cases) {
                scenario.onActivity { activity ->
                    activity.findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = source
                    activity.startActivity(experienceDetailIntent(activity, event))
                }
                onView(withId(detailTab)).check(matches(isSelected()))
                pressBack()
            }
        }
    }
}
