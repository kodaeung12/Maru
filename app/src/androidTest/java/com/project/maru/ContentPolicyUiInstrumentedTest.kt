package com.project.maru

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.maru.community.CommunityWriteActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentPolicyUiInstrumentedTest {
    @Test fun postShowsCorrectionMessageBeforeSubmitting() {
        ActivityScenario.launch(CommunityWriteActivity::class.java).use {
            onView(withId(R.id.etTitle)).perform(replaceText("안내"))
            onView(withId(R.id.etContent)).perform(replaceText("씨 발"), closeSoftKeyboard())
            onView(withId(R.id.btnSave)).perform(click())
            onView(withId(R.id.tvStatus)).check(matches(withText(ContentPolicy.MESSAGE)))
        }
    }
    @Test fun reviewShowsCorrectionMessageBeforeSubmitting() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_TITLE, "검증용 행사")
            putExtra(DetailActivity.EXTRA_PLACE, "검증 장소")
            putExtra(DetailActivity.EXTRA_DATE, "2026-11-23")
        }
        ActivityScenario.launch<DetailActivity>(intent).use {
            onView(withId(R.id.tabReview)).perform(scrollTo(), click())
            onView(withId(R.id.editReviewContent)).perform(scrollTo(), replaceText("개.새.끼"), closeSoftKeyboard())
            onView(withId(R.id.buttonSaveReview)).perform(scrollTo(), click())
            onView(withId(R.id.editReviewContent)).check(matches(hasErrorText(ContentPolicy.MESSAGE)))
        }
    }
}
