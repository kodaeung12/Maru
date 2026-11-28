package com.project.maru

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.maru.community.CommunityNotificationsActivity
import androidx.navigation.fragment.NavHostFragment
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NotificationCenterInstrumentedTest {
    @Test fun readStateIsSeparateForEachAccountAndScheduleRevision() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uid = "notification-test-" + UUID.randomUUID()
        val item = Schedule(UUID.randomUUID().toString(), "로컬 확인", "", System.currentTimeMillis(), 0, "v1")
        try {
            assertFalse(NotificationCenter.isRead(context, uid, item))
            NotificationCenter.markRead(context, uid, item)
            assertTrue(NotificationCenter.isRead(context, uid, item))
            assertFalse(NotificationCenter.isRead(context, uid + "-other", item))
            assertFalse(NotificationCenter.isRead(context, uid, item.copy(revision = "v2")))
        } finally {
            context.getSharedPreferences("notification_reads", 0).edit().remove("$uid/${item.id}/${item.revision}").commit()
        }
    }
    @Test fun dismissingScheduleNotificationKeepsTheScheduleAndIsolatesAccountsAndRevisions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uid = "notification-delete-test-" + UUID.randomUUID()
        val item = Schedule(UUID.randomUUID().toString(), "삭제 확인", "", System.currentTimeMillis(), 0, "v1")
        val originalSchedules = ScheduleSync.items.toList()
        try {
            assertFalse(NotificationCenter.isDismissed(context, uid, item))
            NotificationCenter.dismiss(context, uid, item)
            assertTrue(NotificationCenter.isDismissed(context, uid, item))
            assertFalse(NotificationCenter.isDismissed(context, uid + "-other", item))
            assertFalse(NotificationCenter.isDismissed(context, uid, item.copy(revision = "v2")))
            assertEquals(originalSchedules, ScheduleSync.items)
        } finally {
            context.getSharedPreferences("notification_reads", 0).edit().remove("hidden/$uid/${item.id}/${item.revision}").commit()
        }
    }
    @Test fun communityAndMyPageOpenTheSameNotificationScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            for (destination in listOf(R.id.communityFragment, R.id.myPageFragment)) {
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                    host.navController.navigate(destination)
                }
                instrumentation.waitForIdleSync()
                val monitor = instrumentation.addMonitor(CommunityNotificationsActivity::class.java.name, null, false)
                try {
                    scenario.onActivity { it.findViewById<View>(R.id.btnNotification).performClick() }
                    val notification = monitor.waitForActivityWithTimeout(8000)
                    assertNotNull("같은 알림 화면으로 이동해야 해요", notification)
                    instrumentation.runOnMainSync { notification!!.finish() }
                    instrumentation.waitForIdleSync()
                } finally { instrumentation.removeMonitor(monitor) }
            }
        }
    }
}
