package com.project.maru.community

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.maru.R
import com.project.maru.databinding.ActivityCommunityWriteBinding
import kotlin.concurrent.thread

class CommunityWriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommunityWriteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val post = CommunityPost(
            category = binding.spinnerCategory.selectedItem.toString(),
            title = title,
            content = content,
            ownerId = "local",
            authorName = "로컬 사용자",
            createdAt = System.currentTimeMillis()
        )
        setButtonsEnabled(false)
        binding.tvStatus.text = "기기에 저장하는 중…"


        thread {
            val helper = CommunityDatabaseHelper(applicationContext)
            try {
                val newId = helper.insertPost(post)
                Handler(Looper.getMainLooper()).post {
                    if (newId == -1L) {
                        showSaveError()
                    } else {
                        setButtonsEnabled(true)
                        Toast.makeText(this, "기기에 게시글을 저장했어요.", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            } catch (error: Exception) {
                Handler(Looper.getMainLooper()).post { showSaveError() }
            } finally {
                helper.close()
            }
        }
    }

    private fun showSaveError() {
        setButtonsEnabled(true)
        binding.tvStatus.text = "기기에 저장하지 못했어요. 저장 공간을 확인하고 다시 시도해 주세요."
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnSave.isEnabled = enabled
        binding.etTitle.isEnabled = enabled
        binding.etContent.isEnabled = enabled
        binding.spinnerCategory.isEnabled = enabled
    }
}
