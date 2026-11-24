package com.project.maru.community

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.maru.databinding.ActivityCommunityDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommunityDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommunityDetailBinding
    private var postId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCommunityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getStringExtra("postId") ?: ""
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { loadPost() }
        if (postId.isBlank()) {
            binding.tvStatus.text = "게시글 정보가 없어요. 목록에서 다시 선택해 주세요."
        } else {
            loadPost()
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun loadPost() {
        binding.btnRetry.visibility = View.GONE
        binding.btnRetry.isEnabled = false
        binding.tvStatus.text = "온라인 게시글을 불러오는 중…"
        CommunityRepository.loadPost(postId).addOnCompleteListener(this) { task ->
            binding.btnRetry.isEnabled = true
            if (!task.isSuccessful) {
                binding.tvStatus.text = "게시글을 읽지 못했어요. 연결 상태를 확인하고 다시 불러와 주세요."
                binding.btnRetry.visibility = View.VISIBLE
                return@addOnCompleteListener
            }
            val post = task.result
            if (post == null) {
                binding.tvStatus.text = "게시글을 찾지 못했어요. 삭제되었을 수 있어요."
                return@addOnCompleteListener
            }
            binding.tvCategory.text = post.category
            binding.tvTitle.text = post.title
            binding.tvOwner.text = post.authorName
            binding.tvDate.text = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(post.createdAt))
            binding.tvContent.text = post.content
            binding.tvStatus.text = "온라인 게시글"
        }
    }
}
