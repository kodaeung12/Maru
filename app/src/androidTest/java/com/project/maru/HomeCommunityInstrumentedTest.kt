package com.project.maru

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.project.maru.community.CommunityRepository
import com.project.maru.community.CommunityDetailActivity
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HomeCommunityInstrumentedTest {
    @Test fun latestServerPostsRenderInOrderAndOpenDetail() {
        assumeNotNull(FirebaseAuth.getInstance().currentUser)
        val expected=Tasks.await(CommunityRepository.loadLatestPosts(),30,TimeUnit.SECONDS)
        assertTrue(expected.size<=4)
        assertTrue(expected.none { it.deleting })
        assertEquals(expected.sortedByDescending { it.createdAt },expected)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            val deadline=System.currentTimeMillis()+15000
            var complete=false
            while(!complete && System.currentTimeMillis()<deadline) {
                scenario.onActivity { activity ->
                    val status=activity.findViewById<TextView>(R.id.textHomeCommunityStatus)
                    complete=status.visibility==View.GONE || status.text==activity.getString(R.string.home_community_empty)
                }
                if(!complete) Thread.sleep(100)
            }
            assertTrue("홈 최신 글 조회 완료",complete)
            scenario.onActivity { activity ->
                val rows=activity.findViewById<LinearLayout>(R.id.layoutHomeCommunityPosts)
                assertEquals(expected.size,rows.childCount)
                expected.forEachIndexed { index,post ->
                    assertEquals(post.title,rows.getChildAt(index).findViewById<TextView>(R.id.textHomePostTitle).text.toString())
                }
            }
            if(expected.isNotEmpty()) {
                val monitor=instrumentation.addMonitor(CommunityDetailActivity::class.java.name,null,false)
                try {
                    scenario.onActivity { activity -> activity.findViewById<LinearLayout>(R.id.layoutHomeCommunityPosts).getChildAt(0).performClick() }
                    val detail=monitor.waitForActivityWithTimeout(5000)
                    assertNotNull(detail)
                    assertEquals(expected.first().id,detail.intent.getStringExtra("postId"))
                    instrumentation.runOnMainSync { detail.finish() }
                } finally { instrumentation.removeMonitor(monitor) }
            }
        }
    }

    @Test fun allPostsOpensCommunityTab() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.buttonMoreHomeCommunity).performClick()
                val host=activity.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                assertEquals(R.id.communityFragment,host.navController.currentDestination?.id)
            }
        }
    }
}
