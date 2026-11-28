package com.project.maru

import android.content.Intent
import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.project.maru.diary.DiaryCalendarView
import com.project.maru.diary.DiaryEditorActivity
import com.project.maru.diary.DiaryHomeActivity
import com.project.maru.diary.DiaryDates
import com.project.maru.diary.DiaryEntry
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// 실제 서버에는 쓰지 않고 화면 배치, 피그마 감정 이미지와 날짜 이동만 확인한다.
@RunWith(AndroidJUnit4::class)
class DiaryUiInstrumentedTest {
    @Test fun calendarAndFiveMoodAssetsFitOnScreen() {
        assumeNotNull(FirebaseAuth.getInstance().currentUser)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        ActivityScenario.launch<DiaryHomeActivity>(Intent(context, DiaryHomeActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val calendar = activity.findViewById<DiaryCalendarView>(R.id.calendar)
                assertTrue(calendar.width > 0)
                assertTrue(calendar.height > 0)
                val month = calendar.month
                calendar.findViewWithTag<android.view.View>("calendar-next").performClick()
                assertEquals(month.plusMonths(1), calendar.month)
                calendar.findViewWithTag<android.view.View>("calendar-previous").performClick()
                assertEquals(month, calendar.month)
                calendar.setEntries(listOf(DiaryEntry(date = DiaryDates.today().toString(), mood = "good")))
                assertNotNull(calendar.findViewWithTag<android.view.View>("diary-mood-${DiaryDates.today()}"))
            }
            instrumentation.waitForIdleSync()
            instrumentation.uiAutomation.takeScreenshot()?.let { screenshot ->
                File(context.getExternalFilesDir(null), "diary-calendar-preview.png").outputStream().use {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                screenshot.recycle()
            }
        }
        ActivityScenario.launch<DiaryEditorActivity>(Intent(context, DiaryEditorActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val moods = activity.findViewById<LinearLayout>(R.id.layoutMoods)
                assertEquals(5, moods.childCount)
                listOf("행복", "좋음", "보통", "힘듦", "속상함").forEachIndexed { index, label ->
                    val item = moods.getChildAt(index) as LinearLayout
                    assertEquals(label, item.contentDescription)
                    val image = item.getChildAt(0) as ImageView
                    assertNotNull(image.drawable)
                    assertEquals(36f, image.width / activity.resources.displayMetrics.density, 1f)
                }
                assertEquals("오늘의 이야기", activity.findViewById<TextView>(R.id.tvStory).text)
            }
            instrumentation.waitForIdleSync()
            instrumentation.uiAutomation.takeScreenshot()?.let { screenshot ->
                File(context.getExternalFilesDir(null), "diary-editor-preview.png").outputStream().use {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                screenshot.recycle()
            }
        }
    }
}
