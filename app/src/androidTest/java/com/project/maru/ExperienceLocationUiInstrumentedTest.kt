package com.project.maru

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
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
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

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

    @Test fun leafletDisplaysTilesMarkerAndInteractiveZoom() {
        val intent = detailIntent().apply {
            putExtra("com.project.maru.EXPERIENCE_DETAIL_SOURCE", R.id.experienceFragment)
            putExperienceMapCoordinates(requireNotNull(EventCoordinates.fromApi("37.5665", "126.9780")))
        }
        ActivityScenario.launch<DetailActivity>(intent).use {
            onView(withId(R.id.buttonOpenExperienceLocation)).perform(scrollTo())
            awaitMapState(R.id.experienceLocationPreviewHost, "window.maruMapState === 1 && !!document.querySelector('.leaflet-marker-icon')")
            captureMapForReport("preview")
            onView(withId(R.id.buttonOpenExperienceLocation)).perform(click())
            awaitMapState(R.id.experienceLocationHost, "window.maruMapState === 1 && !!document.querySelector('.leaflet-marker-icon')")
            captureMapForReport("fullscreen")
            onView(withId(R.id.buttonExperienceZoomIn)).perform(click())
            awaitMapState(R.id.experienceLocationHost, "map.getZoom() === 17")
            onView(withId(R.id.buttonExperienceZoomOut)).perform(click())
            awaitMapState(R.id.experienceLocationHost, "map.getZoom() === 16")
            dragMap()
            awaitMapState(R.id.experienceLocationHost, "Math.abs(map.getCenter().lng - 126.978) > 0.0001")
            pinchMap(expand = true)
            awaitMapState(R.id.experienceLocationHost, "map.getZoom() > 16 && !map._animatingZoom")
            pinchMap(expand = false)
            awaitMapState(R.id.experienceLocationHost, "map.getZoom() <= 16 && !map._animatingZoom")
            onView(withId(R.id.buttonBackExperienceLocation)).perform(click())
            awaitDetailResume()
        }
    }

    private fun dragMap() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var start: FloatArray
        lateinit var end: FloatArray
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).single()
            val host = activity.findViewById<View>(R.id.experienceLocationHost)
            start = mapTouchPoint(host, 0.30f)
            end = mapTouchPoint(host, 0.70f)
        }
        val downTime = SystemClock.uptimeMillis()
        for (step in 0..20) {
            val action = when (step) {
                0 -> MotionEvent.ACTION_DOWN
                20 -> MotionEvent.ACTION_UP
                else -> MotionEvent.ACTION_MOVE
            }
            val x = start[0] + (end[0] - start[0]) * step / 20
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, start[1], 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.sendPointerSync(event)
            event.recycle()
            SystemClock.sleep(16)
        }
    }
    private fun pinchMap(expand: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var center: FloatArray
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).single()
            center = mapTouchPoint(activity.findViewById(R.id.experienceLocationHost), 0.5f)
        }
        val downTime = SystemClock.uptimeMillis()
        val fingers = Array(2) { index ->
            MotionEvent.PointerProperties().apply {
                id = index
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        fun send(action: Int, count: Int, gap: Float) {
            val points = Array(count) { index ->
                MotionEvent.PointerCoords().apply {
                    x = center[0] + if (index == 0) -gap else gap
                    y = center[1]
                    pressure = 1f
                    size = 1f
                }
            }
            val event = MotionEvent.obtain(
                downTime, SystemClock.uptimeMillis(), action, count, fingers, points,
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
            )
            try {
                instrumentation.sendPointerSync(event)
            } finally {
                event.recycle()
            }
        }

        val start = if (expand) 60f else 180f
        val end = if (expand) 180f else 60f
        send(MotionEvent.ACTION_DOWN, 1, start)
        send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, start)
        for (step in 1..20) {
            SystemClock.sleep(16)
            send(MotionEvent.ACTION_MOVE, 2, start + (end - start) * step / 20)
        }
        send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, end)
        send(MotionEvent.ACTION_UP, 1, end)
    }

    private fun mapTouchPoint(view: View, horizontalFraction: Float): FloatArray {
        val origin = IntArray(2)
        view.getLocationOnScreen(origin)
        return floatArrayOf(origin[0] + view.width * horizontalFraction, origin[1] + view.height * 0.65f)
    }

    private fun captureMapForReport(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureOsm") != "true") return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        SystemClock.sleep(400)
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = requireNotNull(instrumentation.targetContext.getExternalFilesDir(null))
        File(directory, "maru-osm-$name.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }

    private fun awaitMapState(hostId: Int, condition: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + 18_000
        while (SystemClock.uptimeMillis() < deadline) {
            val result = LinkedBlockingQueue<String>()
            instrumentation.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED).firstOrNull()
                val web = activity?.findViewById<FrameLayout>(hostId)?.getChildAt(0) as? WebView
                if (web == null) result.offer("false")
                else web.evaluateJavascript("Boolean($condition)") { result.offer(it ?: "false") }
            }
            if (result.poll(500, TimeUnit.MILLISECONDS) == "true") return
            SystemClock.sleep(100)
        }
        throw AssertionError("지도 상태를 확인하지 못했습니다. $condition")
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
