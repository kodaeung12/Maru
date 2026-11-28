package com.project.maru.diary

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.Source
import com.project.maru.databinding.ActivityDiaryDetailBinding

class DiaryDetailActivity : DiaryActivity() {
    private lateinit var binding: ActivityDiaryDetailBinding
    private var entry: DiaryEntry? = null
    private var busy = false
    private var requestNumber = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        binding = ActivityDiaryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inset(binding.root)
        binding.header.tvTitle.text = "일기 상세"
        binding.header.btnBack.setOnClickListener { if (!busy) finish() }
        binding.btnAction.setOnClickListener {
            if (!busy) startActivity(Intent(this, DiaryHomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        binding.btnRetry.setOnClickListener { load() }
        binding.btnEdit.setOnClickListener {
            if (entry != null && !busy) startActivity(Intent(this, DiaryEditorActivity::class.java).putExtra("date", dateExtra().toString()))
        }
        binding.btnDelete.setOnClickListener {
            val current = entry ?: return@setOnClickListener
            if (busy) return@setOnClickListener
            AlertDialog.Builder(this).setTitle("일기를 삭제할까요?").setMessage("삭제한 일기는 복구할 수 없어요.")
                .setNegativeButton("취소", null).setPositiveButton("삭제") { _, _ -> delete(current) }.show()
        }
    }
    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized && !busy) load()
    }

    override fun onStop() {
        requestNumber++
        super.onStop()
    }

    private fun controls(enabled: Boolean) {
        binding.btnEdit.isEnabled = enabled
        binding.btnDelete.isEnabled = enabled
        binding.btnEdit.alpha = if (enabled) 1f else 0.4f
        binding.btnDelete.alpha = binding.btnEdit.alpha
    }
    private fun load() {
        if (!active()) return
        val number = ++requestNumber
        entry = null
        controls(false)
        binding.btnRetry.visibility = View.GONE
        binding.tvDate.text = dateExtra().format(dateFormat)
        binding.tvContent.text = ""
        binding.tvMood.text = ""
        binding.ivMood.setImageDrawable(null)
        binding.tvStatus.text = "일기를 불러오는 중…"
        binding.root.postDelayed({
            if (active() && number == requestNumber && entry == null) {
                requestNumber++
                binding.tvStatus.text = "서버 응답이 늦어지고 있어요. 인터넷 연결 후 다시 불러와 주세요."
                binding.btnRetry.visibility = View.VISIBLE
            }
        }, 20000)
        DiaryRepository.entries(uid).document(dateExtra().toString()).get(Source.SERVER)
            .addOnSuccessListener { document ->
                if (!active() || number != requestNumber) return@addOnSuccessListener
                entry = document.toObject(DiaryEntry::class.java)
                val current = entry
                if (current == null) { requestNumber++
                    binding.tvStatus.text = "삭제되었거나 존재하지 않는 일기예요."
                    return@addOnSuccessListener }
                val mood = DiaryMood.from(current.mood)
                binding.tvMood.text = mood?.description.orEmpty()
                mood?.let { binding.ivMood.setImageResource(it.image) }
                binding.tvContent.text = current.content
                binding.tvStatus.text = ""
                controls(true)
            }.addOnFailureListener {
                if (active() && number == requestNumber) { requestNumber++
                    binding.tvStatus.text = DiaryRepository.message(it)
                    binding.btnRetry.visibility = View.VISIBLE }
            }
    }
    private fun delete(current: DiaryEntry) {
        if (busy || !active()) return
        busy = true
        controls(false)
        binding.tvStatus.text = "일기를 삭제하는 중…"
        DiaryRepository.delete(uid, current).addOnSuccessListener {
            if (active()) { toast("일기를 삭제했어요.")
                finish() }
        }.addOnFailureListener {
            if (active()) { busy = false
                controls(true)
                binding.tvStatus.text = DiaryRepository.message(it) }
        }
    }
}
