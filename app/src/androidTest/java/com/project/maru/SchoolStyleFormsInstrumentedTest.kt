package com.project.maru

import android.view.View
import android.widget.LinearLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.maru.databinding.ItemDiaryMoodBinding
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith

// 서버에 쓰지 않고 XML로 옮긴 입력창과 항목을 확인한다.
@RunWith(AndroidJUnit4::class)
class SchoolStyleFormsInstrumentedTest {
    @Test fun scheduleDialogKeepsTitleValidationAndDateTimeControls() {
        assumeNotNull(MyApplication.auth.currentUser)
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { ScheduleUi.showEditor(it) }
            onView(withId(R.id.editTitle)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withId(R.id.buttonDate)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withId(R.id.buttonTime)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withId(R.id.spinnerReminder)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText("저장")).inRoot(isDialog()).perform(click())
            onView(withId(R.id.textError)).inRoot(isDialog()).check(matches(withText("일정 제목을 입력해 주세요.")))
            onView(withText("취소")).inRoot(isDialog()).perform(click())
        }
    }

    @Test fun childDialogKeepsRequiredNameValidationAndGenderChoices() {
        assumeNotNull(MyApplication.auth.currentUser)
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                host.navController.navigate(R.id.myInfoFragment)
            }
            onView(withId(R.id.buttonAddMyChild)).perform(scrollTo(), click())
            onView(withId(R.id.radioGirl)).inRoot(isDialog()).check(matches(withText("여아")))
            onView(withId(R.id.radioBoy)).inRoot(isDialog()).check(matches(withText("남아")))
            onView(withText(R.string.save)).inRoot(isDialog()).perform(click())
            onView(withId(R.id.editChildName)).inRoot(isDialog()).check(matches(hasErrorText(
                InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.child_name_required))))
            onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click())
        }
    }

    @Test fun moodItemXmlKeepsFiveEqualWidthChoices() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val row = LinearLayout(activity)
                for (mood in com.project.maru.diary.DiaryMood.entries) {
                    val item = ItemDiaryMoodBinding.inflate(activity.layoutInflater, row, false)
                    item.imageMood.setImageResource(mood.image)
                    item.textMood.text = mood.label
                    row.addView(item.root)
                    val parameters = item.root.layoutParams as LinearLayout.LayoutParams
                    assertEquals(0, parameters.width)
                    assertEquals(1f, parameters.weight, 0f)
                    assertEquals(View.VISIBLE, item.imageMood.visibility)
                }
                assertEquals(5, row.childCount)
            }
        }
    }
}
