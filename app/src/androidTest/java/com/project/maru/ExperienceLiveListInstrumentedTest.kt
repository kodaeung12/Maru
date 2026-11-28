package com.project.maru

import android.os.SystemClock
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExperienceLiveListInstrumentedTest {
    @Test
    fun showsResultCountAfterLiveSeoulApiLoad() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveSeoulApi") == "true")
        assumeTrue(BuildConfig.SEOUL_API_KEY.isNotBlank())

        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                host.navController.navigate(R.id.experienceFragment)
            }
            val deadline = SystemClock.elapsedRealtime() + 20_000
            var ready = false
            while (!ready && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity { activity ->
                    val count = activity.findViewById<RecyclerView>(R.id.recyclerExperience).adapter?.itemCount ?: 0
                    val status = activity.findViewById<TextView>(R.id.textSearchStatus)
                    ready = count > 0 && status.text.toString() == activity.getString(R.string.search_count, count)
                }
                if (!ready) SystemClock.sleep(100)
            }
            assertTrue("Seoul API request did not finish before the timeout", ready)
            onView(withId(R.id.textSearchStatus)).check(matches(isDisplayed()))
        }
    }
}
