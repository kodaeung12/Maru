package com.project.maru.community

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.maru.R
import com.project.maru.databinding.ActivityCommunityBinding

class CommunityActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommunityBinding
    private lateinit var adapter: CommunityPostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = "local"
        adapter = CommunityPostAdapter(currentUser, { item ->
            val intent = Intent(this, CommunityDetailActivity::class.java)
            intent.putExtra("postId", item.id)
            startActivity(intent)
        })
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter


        val contract = ActivityResultContracts.StartActivityForResult()
        val callback = object : ActivityResultCallback<ActivityResult> {
            override fun onActivityResult(result: ActivityResult) {
                if (result.resultCode == RESULT_OK) {
                    selectCategory("전체")
                    loadPosts()
                }
            }
        }
        val launcher = registerForActivityResult(contract, callback)

        binding.btnWrite.setOnClickListener {
            val intent = Intent(this, CommunityWriteActivity::class.java)
            launcher.launch(intent)
        }
        binding.btnRefresh.setOnClickListener { loadPosts() }
        binding.btnNotification.setOnClickListener { toast("알림 기능은 준비 중이에요.") }
        binding.categoryAll.setOnClickListener { selectCategory("전체") }
        binding.categoryParenting.setOnClickListener { selectCategory("육아정보") }
        binding.categoryDaily.setOnClickListener { selectCategory("일상공유") }
        binding.categoryConcern.setOnClickListener { selectCategory("고민상담") }
        binding.categoryLocal.setOnClickListener { selectCategory("지역정보") }

        binding.navView.selectedItemId = R.id.communityMenu
        binding.navView.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.communityMenu) {
                true
            } else {
                toast("이 메뉴는 팀 화면 연결 후 사용할 수 있어요.")
                false
            }
        }
        selectCategory("전체")
        loadPosts()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun selectCategory(category: String) {
        val tabs = listOf(binding.categoryAll, binding.categoryParenting,
            binding.categoryDaily, binding.categoryConcern, binding.categoryLocal)
        for (tab: TextView in tabs) {
            val selected = tab.text.toString() == category
            tab.setBackgroundResource(
                if (selected) R.drawable.community_chip_on else R.drawable.community_chip_off
            )
            tab.setTextColor(ContextCompat.getColor(this,
                if (selected) android.R.color.white else R.color.community_pink))
        }
        val count = adapter.applyFilter(category)
        showCount(count)
    }

    private fun loadPosts() {
        setButtonsEnabled(false)
        binding.layoutEmpty.visibility = View.GONE
        binding.tvStatus.text = "게시글을 불러오는 중…"
        adapter.loadItems(applicationContext, { count ->
            setButtonsEnabled(true)
            showCount(count)
        }, {
            setButtonsEnabled(true)
            binding.tvStatus.text = "기기에 저장된 글을 불러오지 못했어요. 새로고침해 주세요."
        })
    }

    private fun showCount(count: Int) {
        binding.layoutEmpty.visibility = if (count == 0) View.VISIBLE else View.GONE
        binding.tvStatus.text = if (count == 0) "기기에 저장된 게시글이 없어요. 첫 글을 작성해 보세요."
            else "게시글 ${count}개"
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnRefresh.isEnabled = enabled
        binding.categoryAll.isEnabled = enabled
        binding.categoryParenting.isEnabled = enabled
        binding.categoryDaily.isEnabled = enabled
        binding.categoryConcern.isEnabled = enabled
        binding.categoryLocal.isEnabled = enabled
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
