package com.project.maru

import android.content.Intent
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.UiController
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.project.maru.experience.Experience
import com.project.maru.experience.FavoriteRepository
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FavoritesInstrumentedTest {
    private fun uid(): String = requireNotNull(FirebaseAuth.getInstance().currentUser) { "로그인된 에뮬레이터가 필요합니다." }.uid
    private fun event() = Experience("즐겨찾기 확인 " + UUID.randomUUID(), "서울 체험관", "2026-11-23", "체험",
        targetAge = "5세 이상", region = "종로구", startDate = LocalDate.of(2026, 11, 23), endDate = LocalDate.of(2026, 11, 23))

    @Test fun storesSnapshotAndRepeatedSaveIsIdempotent() {
        val uid = uid()
        val event = event()
        val ref = FavoriteRepository.collection(uid).document(FavoriteRepository.id(event))
        try {
            Tasks.await(FavoriteRepository.setSaved(uid, event, true), 30, TimeUnit.SECONDS)
            val first = Tasks.await(ref.get(Source.SERVER), 30, TimeUnit.SECONDS)
            assertEquals(event, FavoriteRepository.read(first))
            assertNotNull(first.getTimestamp("savedAt"))
            Tasks.await(FavoriteRepository.setSaved(uid, event, true), 30, TimeUnit.SECONDS)
            val second = Tasks.await(ref.get(Source.SERVER), 30, TimeUnit.SECONDS)
            assertEquals(first.getTimestamp("savedAt"), second.getTimestamp("savedAt"))
            Tasks.await(FavoriteRepository.setSaved(uid, event, false), 30, TimeUnit.SECONDS)
            Tasks.await(FavoriteRepository.setSaved(uid, event, false), 30, TimeUnit.SECONDS)
            assertFalse(Tasks.await(ref.get(Source.SERVER), 30, TimeUnit.SECONDS).exists())
        } finally {
            Tasks.await(FavoriteRepository.setSaved(uid, event, false), 30, TimeUnit.SECONDS)
        }
    }

    @Test fun otherAccountsFavoritesCannotBeReadWrittenOrDeleted() {
        uid()
        val event = event()
        val ref = FavoriteRepository.collection("integration-other-account").document(FavoriteRepository.id(event))
        denied(ref.get(Source.SERVER))
        denied(ref.set(mapOf("title" to "거절되어야 합니다")))
        denied(ref.delete())
    }

    @Test fun detailSaveAppearsInFavoritesAndCardCanRemoveIt() {
        val uid = uid()
        val event = event()
        val ref = FavoriteRepository.collection(uid).document(FavoriteRepository.id(event))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_TITLE, event.title)
            putExtra(DetailActivity.EXTRA_PLACE, event.location)
            putExtra(DetailActivity.EXTRA_DATE, event.date)
            putExtra(DetailActivity.EXTRA_CATEGORY, event.category)
            putExtra(DetailActivity.EXTRA_REGION, event.region)
            putExtra(DetailActivity.EXTRA_TARGET_AGE, event.targetAge)
            putExtra(DetailActivity.EXTRA_START_DATE, event.startDate.toString())
            putExtra(DetailActivity.EXTRA_END_DATE, event.endDate.toString())
        }
        try {
            ActivityScenario.launch<DetailActivity>(intent).use {
                onView(withId(R.id.buttonDetailFavorite)).perform(awaitState { it.alpha == 1f && it.isEnabled })
                onView(withId(R.id.buttonDetailFavorite)).check(matches(isNotSelected())).perform(click())
                onView(withId(R.id.buttonDetailFavorite)).perform(awaitState { it.isSelected && it.isEnabled })
            }
            assertTrue(Tasks.await(ref.get(Source.SERVER), 30, TimeUnit.SECONDS).exists())
            ActivityScenario.launch(HomeActivity::class.java).use {
                onView(withId(R.id.btnFavorite)).perform(click())
                onView(withId(R.id.editSearch)).perform(replaceText(event.title), closeSoftKeyboard())
                onView(withId(R.id.recyclerExperience)).perform(awaitState { view ->
                    (view as androidx.recyclerview.widget.RecyclerView).adapter?.itemCount == 1
                })
                onView(allOf(withId(R.id.textTitle), withText(event.title))).check(matches(isDisplayed()))
                onView(withId(R.id.buttonFavoriteEvent)).check(matches(isSelected())).perform(click())
                onView(withId(R.id.recyclerExperience)).perform(awaitState { view ->
                    (view as androidx.recyclerview.widget.RecyclerView).adapter?.itemCount == 0
                })
                onView(allOf(withId(R.id.textTitle), withText(event.title))).check(doesNotExist())
            }
            assertFalse(Tasks.await(ref.get(Source.SERVER), 30, TimeUnit.SECONDS).exists())
        } finally {
            Tasks.await(FavoriteRepository.setSaved(uid, event, false), 30, TimeUnit.SECONDS)
        }
    }

    private fun denied(task: Task<*>) {
        try {
            Tasks.await(task, 30, TimeUnit.SECONDS)
            fail("다른 계정의 즐겨찾기에 접근하면 안 됩니다.")
        } catch (error: java.util.concurrent.ExecutionException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, (error.cause as FirebaseFirestoreException).code)
        }
    }

    private fun awaitState(predicate: (View) -> Boolean): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()
        override fun getDescription(): String = "즐겨찾기 상태가 화면에 반영될 때까지 대기"
        override fun perform(controller: UiController, view: View) {
            val deadline = SystemClock.elapsedRealtime() + 15000
            while (!predicate(view) && SystemClock.elapsedRealtime() < deadline) controller.loopMainThreadForAtLeast(50)
            assertTrue("즐겨찾기 상태 반영 시간 초과", predicate(view))
        }
    }
}
