package com.project.maru

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
import com.project.maru.community.CommunityComment
import com.project.maru.community.CommunityDetailActivity
import com.project.maru.community.MyCommentsAdapter
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith

// 기존 자료를 조회한다. 테스트 댓글은 생성하지 않으며 상세 진입도 가로채 조회수 변경을 피한다.
@RunWith(AndroidJUnit4::class)
class MyCommentsInstrumentedTest {
    @Test fun showsExistingOwnCommentsInOrderAndOpensTheirPost() {
        val user = MyApplication.auth.currentUser
        assumeNotNull(user)
        val uid = user!!.uid
        val posts = Tasks.await(MyApplication.db.collection("communityPosts").get(Source.SERVER), 30, TimeUnit.SECONDS)
        val expected = mutableListOf<CommunityComment>()
        for (post in posts) {
            if (post.getBoolean("deleting") == true) continue
            val comments = Tasks.await(post.reference.collection("comments").get(Source.SERVER), 30, TimeUnit.SECONDS)
            for (document in comments) {
                val comment = document.toObject(CommunityComment::class.java)
                if (comment.ownerId != uid) continue
                comment.id = document.id
                comment.postId = post.id
                expected.add(comment)
            }
        }
        expected.sortWith(compareByDescending<CommunityComment> { it.createdAt }.thenBy { it.postId }.thenBy { it.id })
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                host.navController.navigate(R.id.myPageFragment)
            }
            scenario.onActivity { it.findViewById<View>(R.id.tabMyComments).performClick() }
            val end = SystemClock.elapsedRealtime() + 30_000
            var complete = false
            while (!complete && SystemClock.elapsedRealtime() < end) {
                scenario.onActivity { activity ->
                    val status = activity.findViewById<TextView>(R.id.tvMyPageStatus).text.toString()
                    val empty = activity.findViewById<TextView>(R.id.tvMyPageEmpty).text.toString()
                    complete = status.startsWith("내가 작성한 댓글 ") || empty == "아직 작성한 댓글이 없어요."
                }
                if (!complete) SystemClock.sleep(150)
            }
            assertTrue("내 댓글 조회가 완료되어야 합니다", complete)
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.recyclerMyPosts)
                assertTrue(list.adapter is MyCommentsAdapter)
                assertEquals(expected.size, list.adapter!!.itemCount)
            }
            if (expected.isNotEmpty()) {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                var openedPost: String? = null
                val monitor = object : Instrumentation.ActivityMonitor() {
                    override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                        if (intent.component?.className == CommunityDetailActivity::class.java.name) {
                            openedPost = intent.getStringExtra("postId")
                            return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, Intent())
                        }
                        return null
                    }
                }
                instrumentation.addMonitor(monitor)
                try {
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { activity ->
                        val list = activity.findViewById<RecyclerView>(R.id.recyclerMyPosts)
                        val first = list.findViewHolderForAdapterPosition(0) ?: error("첫 댓글이 표시되지 않았어요")
                        assertEquals(expected.first().content, first.itemView.findViewById<TextView>(R.id.textComment).text.toString())
                        first.itemView.performClick()
                    }
                    assertEquals(expected.first().postId, openedPost)
                } finally { instrumentation.removeMonitor(monitor) }
            }
            scenario.onActivity { it.findViewById<View>(R.id.tabMyPosts).performClick() }
            scenario.onActivity {
                assertTrue(it.findViewById<RecyclerView>(R.id.recyclerMyPosts).adapter is com.project.maru.community.CommunityPostAdapter)
            }
        }
    }
}
