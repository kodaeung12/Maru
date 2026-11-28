package com.project.maru
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.CalendarView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ScheduleInstrumentedTest {
    @Test fun homeBellOpensSchedulesAndCalendarSupportsMonthNavigation() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val calendar=activity.findViewById<FamilyCalendarView>(R.id.calendarSchedules)
                assertEquals(View.VISIBLE,calendar.visibility)
                val today=java.time.LocalDate.now()
                calendar.selectDate(java.time.LocalDate.of(2028,1,31))
                calendar.findViewWithTag<View>("calendar-next").performClick()
                assertEquals(java.time.LocalDate.of(2028,2,29),calendar.selectedDate)
                calendar.setScheduleDates(setOf(calendar.selectedDate))
                assertNotNull(calendar.findViewWithTag<View>("calendar-dot-2028-02-29"))
                assertNull(calendar.findViewWithTag<View>("calendar-today"))
                assertNotNull(calendar.findViewWithTag<View>("calendar-title"))
                calendar.selectDate(today)
                assertEquals(today,calendar.selectedDate)
                activity.findViewById<View>(R.id.btnBell).performClick()
            }
            onView(withId(R.id.tvScreenTitle)).check(matches(withText("알림")))
            onView(withId(R.id.recyclerNotifications)).check(matches(isDisplayed()))
            androidx.test.espresso.Espresso.pressBack()
        }
    }

    @Test fun notificationUsesScheduleContentRejectsWrongRevisionAndCancelsOnDelete() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val uid=FirebaseAuth.getInstance().currentUser?.uid ?: error("로그인 필요")
        val granted=ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
        val previous=ScheduleAlarms.cached(context,uid)
        if(!granted) instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
        val item=Schedule(UUID.randomUUID().toString(),"테스트 일정 알림","테스트 장소",System.currentTimeMillis()-1000,0,UUID.randomUUID().toString())
        val manager=context.getSystemService(NotificationManager::class.java)
        try {
            instrumentation.runOnMainSync {
                ScheduleAlarms.replace(context,uid,previous+item)
                ScheduleAlarmReceiver().onReceive(context,Intent().putExtra("uid",uid).putExtra("id",item.id).putExtra("revision","wrong"))
                assertFalse(manager.activeNotifications.any { it.tag=="$uid/${item.id}" })
                ScheduleAlarms.deliver(context,uid,item.id,item.revision)
                val notification=manager.activeNotifications.single { it.tag=="$uid/${item.id}" }.notification
                assertEquals(item.title,notification.extras.getString("android.title"))
                assertTrue(notification.extras.getString("android.text")!!.contains(item.note))
                ScheduleAlarms.deliver(context,uid,item.id,item.revision)
                assertEquals(1,manager.activeNotifications.count { it.tag=="$uid/${item.id}" })
                ScheduleAlarms.replace(context,uid,previous)
                assertFalse(manager.activeNotifications.any { it.tag=="$uid/${item.id}" })
            }
        } finally {
            instrumentation.runOnMainSync { ScheduleAlarms.replace(context,uid,previous) }
            if(!granted) instrumentation.uiAutomation.revokeRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test fun onlineScheduleCrudAndOtherAccountDenied() {
        val uid=FirebaseAuth.getInstance().currentUser?.uid ?: error("로그인 필요")
        val item=Schedule(UUID.randomUUID().toString(),"일정 저장 테스트","테스트용",System.currentTimeMillis()+3600000,0,UUID.randomUUID().toString())
        val ref=ScheduleRepository.collection(uid).document(item.id)
        var current=item
        try {
            Tasks.await(ScheduleRepository.save(uid,item,null),30,TimeUnit.SECONDS)
            assertEquals(item.title,Tasks.await(ref.get(com.google.firebase.firestore.Source.SERVER),30,TimeUnit.SECONDS).getString("title"))
            current=item.copy(title="수정 테스트",revision=UUID.randomUUID().toString())
            Tasks.await(ScheduleRepository.save(uid,current,item.revision),30,TimeUnit.SECONDS)
            assertEquals(current.title,Tasks.await(ref.get(com.google.firebase.firestore.Source.SERVER),30,TimeUnit.SECONDS).getString("title"))
            val stale=runCatching { Tasks.await(ScheduleRepository.save(uid,item.copy(revision=UUID.randomUUID().toString()),item.revision),30,TimeUnit.SECONDS) }
            assertTrue(stale.isFailure)
            val deadline=System.currentTimeMillis()+10000
            while(ScheduleSync.items.none { it.id==item.id && it.title==current.title } && System.currentTimeMillis()<deadline) Thread.sleep(100)
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.findViewById<View>(R.id.btnBell).performClick() }
                onView(withText(org.hamcrest.Matchers.containsString(current.title))).check(matches(isDisplayed())).perform(click())
                onView(withText("일정 수정")).check(matches(isDisplayed()))
                onView(withText("취소")).perform(click())
            }
            val denied=runCatching { Tasks.await(ScheduleRepository.collection("other-account").document(item.id).get(com.google.firebase.firestore.Source.SERVER),30,TimeUnit.SECONDS) }
            assertTrue(denied.isFailure)
        } finally { Tasks.await(ScheduleRepository.delete(uid,current),30,TimeUnit.SECONDS) }
        assertFalse(Tasks.await(ref.get(com.google.firebase.firestore.Source.SERVER),30,TimeUnit.SECONDS).exists())
    }
}
