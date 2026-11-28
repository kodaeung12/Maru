package com.project.maru

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.ScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.maru.community.*
import com.project.maru.databinding.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// 쓰기 요청 없이 실제 화면과 로컬 예시 데이터를 사용해 디자인을 검증한다.
@RunWith(AndroidJUnit4::class)
class CommunityDesignInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        Thread.sleep(350)
        instrumentation.waitForIdleSync()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("화면 캡처 실패")
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
    private fun insets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    @Test fun homeKeepsWriteNavigationAndWritePreviewUpdates() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity {
                val host = it.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                host.navController.navigate(R.id.communityFragment)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                val host = it.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                assertEquals(R.id.communityFragment, host.navController.currentDestination?.id)
                it.findViewById<View>(R.id.introBanner).visibility = View.VISIBLE
                assertNotNull(it.findViewById<View>(R.id.etSearch))
                assertNotNull(it.findViewById<View>(R.id.spinnerSort))
            }
            screenshot("community-figma-home")
            val monitor = instrumentation.addMonitor(CommunityWriteActivity::class.java.name, null, false)
            try {
                scenario.onActivity { it.findViewById<View>(R.id.btnWrite).performClick() }
                val write = monitor.waitForActivityWithTimeout(8000)
                assertNotNull(write)
                instrumentation.runOnMainSync { write.finish() }
            } finally { instrumentation.removeMonitor(monitor) }
        }
        ActivityScenario.launch(CommunityWriteActivity::class.java).use { scenario ->
            scenario.onActivity {
                val binding = ActivityCommunityWriteBinding.bind(it.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0))
                binding.etTitle.setText("5살 아이 편식 해결한 방법 공유해요!")
                binding.etContent.setText("저희 아이가 채소를 정말 안 먹었는데,\n이 방법으로 조금씩 먹기 시작했어요!")
                assertEquals(binding.etTitle.text.toString(), binding.tvPreviewTitle.text.toString())
                assertEquals(10, binding.imageGrid.adapter.count)
                assertEquals("${binding.etTitle.text.length}/50", binding.tvTitleCount.text.toString())
            }
            screenshot("community-figma-write-top")
            scenario.onActivity {
                val grid = it.findViewById<GridView>(R.id.imageGrid)
                (grid.parent.parent as ScrollView).fullScroll(View.FOCUS_DOWN)
            }
            screenshot("community-figma-write-bottom")
            scenario.onActivity { activity ->
                val content = activity.findViewById<android.widget.EditText>(R.id.etContent)
                content.requestFocus()
                (activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                    .showSoftInput(content, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            screenshot("community-figma-write-keyboard")
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                val ime = decor.rootWindowInsets?.getInsets(android.view.WindowInsets.Type.ime())?.bottom ?: 0
                if (ime > 0) {
                    val rect = android.graphics.Rect()
                    assertTrue(activity.findViewById<View>(R.id.btnSaveBottom).getGlobalVisibleRect(rect))
                    assertTrue("등록 버튼은 키보드 위에 보여야 한다", rect.bottom <= decor.height - ime + 4)
                }
            }
        }
    }

    @Test fun detailCommentsNotificationsAndOptionsRenderFromLocalExamples() {
        ActivityScenario.launch(CommunityWriteActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val b = ActivityCommunityDetailBinding.inflate(activity.layoutInflater)
                activity.setContentView(b.root); insets(b.root)
                b.postBody.visibility = View.VISIBLE; b.commentComposer.visibility = View.VISIBLE
                b.tvCategory.text = "육아정보"; b.tvTitle.text = "5살 아이 편식 해결한 방법 공유해요!"
                b.tvOwner.text = "행복엄마"; b.tvDate.text = "2026.11.27 10:24"
                b.tvContent.text = "저희 아이가 채소를 정말 안 먹었는데,\n이 방법으로 조금씩 먹기 시작했어요!\n혹시 비슷한 고민 있으신 분들께\n도움이 되었으면 해서 공유해요 :)"
                b.tvStats.text = "조회 48"; b.btnLike.text = "공감 12"; b.tvCommentCount.text = "댓글 2"
                CommunityImages.show(b.ivRepresentative, "meal")
                b.commentList.layoutManager = LinearLayoutManager(activity)
                b.commentList.adapter = CommunityCommentAdapter("", {}).apply { submitList(listOf(
                    CommunityComment(id="sample1",authorName="마루맘",content="저도 같은 고민이었는데 정말 도움이 되었어요!",createdAt=System.currentTimeMillis(),childAge=6),
                    CommunityComment(id="sample2",authorName="행복엄마",content="맞아요! 저도 요리하면서 많이 나아졌어요.",createdAt=System.currentTimeMillis())
                )) }
                b.spinnerChildAge.adapter = ArrayAdapter(activity,R.layout.community_spinner_item,listOf("자녀 나이 표시 안 함"))
                assertNotNull(b.btnLike.compoundDrawablesRelative[0])
            }
            screenshot("community-figma-detail")
            scenario.onActivity { activity ->
                val b = ActivityCommunityDetailBinding.bind((activity.findViewById<android.view.ViewGroup>(android.R.id.content)).getChildAt(0))
                assertEquals(2, b.commentList.adapter?.itemCount)
                assertNotNull("두 번째 댓글도 스크롤 본문에 배치돼야 한다", b.commentList.findViewHolderForAdapterPosition(1))
                (b.postBody.parent as androidx.core.widget.NestedScrollView).fullScroll(View.FOCUS_DOWN)
            }
            screenshot("community-figma-comments")
            scenario.onActivity { activity ->
                val b = ActivityCommunityNotificationsBinding.inflate(activity.layoutInflater)
                activity.setContentView(b.root); insets(b.root)
                b.tvStatus.text = "읽지 않은 알림 2개"
                b.recyclerNotifications.layoutManager = LinearLayoutManager(activity)
                b.recyclerNotifications.adapter = object : RecyclerView.Adapter<NoteHolder>() {
                    override fun getItemCount() = 2
                    override fun onCreateViewHolder(parent: android.view.ViewGroup, type: Int) = NoteHolder(ItemCommunityNotificationBinding.inflate(activity.layoutInflater,parent,false))
                    override fun onBindViewHolder(holder: NoteHolder, position: Int) = with(holder.b) {
                        tvMessage.text = if(position==0) "댓글이 달렸어요" else "공감이 달렸어요"
                        tvTitle.text = "5살 아이 편식 해결한 방법 공유해요!"
                        tvDate.text = if(position==0) "5분 전" else "25분 전"
                        ivType.setImageResource(if(position==0) R.drawable.community_figma_chat else R.drawable.community_figma_heart_filled)
                        iconBadge.setBackgroundResource(if(position==0) R.drawable.community_note_comment else R.drawable.community_note_like)
                    }
                }
            }
            screenshot("community-figma-notifications")
            scenario.onActivity { activity ->
                val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(activity)
                val b = CommunityPostOptionsBinding.inflate(activity.layoutInflater)
                b.tvPostTitle.text = "5살 아이 편식 해결한 방법 공유해요!"
                b.btnEdit.visibility=View.GONE; b.btnDelete.visibility=View.GONE
                sheet.setContentView(b.root); sheet.show()
            }
            screenshot("community-figma-options")
        }
    }
    private class NoteHolder(val b: ItemCommunityNotificationBinding) : RecyclerView.ViewHolder(b.root)
}
