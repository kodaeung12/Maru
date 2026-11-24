package com.project.maru.community

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.project.maru.R
import com.project.maru.databinding.ActivityCommunityWriteBinding
import java.util.UUID

class CommunityWriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommunityWriteBinding

    private var draftId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        draftId = savedInstanceState?.getString("draftId") ?: UUID.randomUUID().toString()
        enableEdgeToEdge()
        binding = ActivityCommunityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categories = resources.getStringArray(R.array.community_categories)
        val aadapter = ArrayAdapter(this, R.layout.community_spinner_item, categories)
        aadapter.setDropDownViewResource(R.layout.community_spinner_dropdown)
        binding.spinnerCategory.adapter = aadapter
        binding.btnSave.setOnClickListener { savePost() }
        binding.btnBack.setOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun savePost() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        if (title.isBlank()) {
            binding.etTitle.error = "제목을 입력해 주세요."
            binding.etTitle.requestFocus()
            return
        }
        if (content.isBlank()) {
            binding.etContent.error = "내용을 입력해 주세요."
            binding.etContent.requestFocus()
            return
        }
        if (com.project.maru.ContentPolicy.containsAbuse(title) || com.project.maru.ContentPolicy.containsAbuse(content)) {
            binding.tvStatus.text = com.project.maru.ContentPolicy.MESSAGE
            return
        }
        if (!binding.btnSave.isEnabled) return
        if (title.length > 50 || content.length > 5000) {
            binding.tvStatus.text = "제목은 50자, 내용은 5000자까지 입력해 주세요."
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            binding.tvStatus.text = "로그인 후 게시글을 작성해 주세요."
            return
        }
        val post = CommunityPost(
            id = draftId,
            category = binding.spinnerCategory.selectedItem.toString(),
            title = title,
            content = content,
            ownerId = user.uid,
            authorName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "마루 이용자",
            createdAt = System.currentTimeMillis()
        )
        setButtonsEnabled(false)
        binding.tvStatus.text = "온라인으로 등록하는 중…"
        CommunityRepository.createPost(post).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "게시글을 온라인으로 등록했어요.", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                showSaveError()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("draftId", draftId)
        super.onSaveInstanceState(outState)
    }

    private fun showSaveError() {
        setButtonsEnabled(true)
        binding.tvStatus.text = "온라인 등록에 실패했어요. 인터넷 연결과 로그인 상태를 확인하고 다시 시도해 주세요."
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnSave.isEnabled = enabled
        binding.etTitle.isEnabled = enabled
        binding.etContent.isEnabled = enabled
        binding.spinnerCategory.isEnabled = enabled
    }
}
