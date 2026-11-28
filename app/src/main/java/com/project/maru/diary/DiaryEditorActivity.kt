package com.project.maru.diary

import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Source
import com.project.maru.databinding.ActivityDiaryEditorBinding
import com.project.maru.databinding.ItemDiaryMoodBinding
import java.time.LocalDate

class DiaryEditorActivity : DiaryActivity() {
    private lateinit var binding: ActivityDiaryEditorBinding
    private lateinit var date: LocalDate
    private var original: DiaryEntry? = null
    private var selectedMood = ""
    private var ready = false
    private var busy = false
    private var initialText = ""
    private var initialMood = ""
    private var loadFinished = false
    private val moodViews = mutableListOf<Pair<DiaryMood, LinearLayout>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        date = dateExtra()
        if (date > DiaryDates.today()) { toast("미래의 일기는 작성할 수 없어요.")
            finish()
            return }
        binding = ActivityDiaryEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inset(binding.root)
        binding.header.tvTitle.text = "오늘의 일기"
        binding.tvDate.text = date.format(dateFormat)
        binding.header.btnBack.setOnClickListener { leave() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { leave() }
        })
        makeMoods()
        binding.etContent.doAfterTextChanged { binding.tvCount.text = "${it?.length ?: 0} / 2000" }
        binding.btnAction.setOnClickListener { save() }
        binding.btnAction.isEnabled = false
        binding.etContent.isEnabled = false
        binding.tvStatus.text = "일기를 확인하는 중…"
        binding.root.postDelayed({
            if (active() && !loadFinished) {
                loadFinished = true
                binding.tvStatus.text = "서버 응답이 늦어지고 있어요. 뒤로 가서 다시 시도해 주세요."
            }
        }, 20000)
        DiaryRepository.entries(uid).document(date.toString()).get(Source.SERVER)
            .addOnSuccessListener { document ->
                if (!active() || loadFinished) return@addOnSuccessListener
                loadFinished = true
                original = document.toObject(DiaryEntry::class.java)
                initialText = original?.content.orEmpty()
                initialMood = original?.mood.orEmpty()
                // 화면 회전 후에도 입력한 내용과 수정 기준 버전을 유지한다.
                if (savedInstanceState?.containsKey("draft") == true) {
                    binding.etContent.setText(savedInstanceState.getString("draft"))
                    selectedMood = savedInstanceState.getString("mood").orEmpty()
                    if (savedInstanceState.getBoolean("hadOriginal")) {
                        original = original?.copy(updatedAt = Timestamp(savedInstanceState.getLong("seconds"), savedInstanceState.getInt("nanos")))
                        if (original == null) { binding.tvStatus.text = "일기가 삭제됐어요. 뒤로 가서 다시 확인해 주세요."
                            return@addOnSuccessListener }
                    } else if (original != null) {
                        binding.tvStatus.text = "이 날짜에 일기가 이미 등록됐어요. 뒤로 가서 확인해 주세요."
                        return@addOnSuccessListener
                    }
                } else {
                    binding.etContent.setText(initialText)
                    selectedMood = initialMood
                }
                ready = true
                binding.etContent.isEnabled = true
                binding.btnAction.isEnabled = true
                binding.header.tvTitle.text = if (original == null) "오늘의 일기" else "일기 수정"
                binding.btnAction.text = if (original == null) "일기 등록하기" else "일기 수정하기"
                binding.tvStatus.text = ""
                renderMood()
            }.addOnFailureListener {
                if (active() && !loadFinished) {
                    loadFinished = true
                    binding.tvStatus.text = DiaryRepository.message(it) + " 뒤로 가서 다시 시도해 주세요."
                }
            }
    }

    private fun makeMoods() {
        for (mood in DiaryMood.entries) {
            val item = ItemDiaryMoodBinding.inflate(layoutInflater, binding.layoutMoods, false)
            item.imageMood.setImageResource(mood.image)
            item.textMood.text = mood.label
            item.root.contentDescription = mood.label
            item.root.setOnClickListener {
                if (ready && !busy) {
                    selectedMood = mood.key
                    renderMood()
                }
            }
            binding.layoutMoods.addView(item.root)
            moodViews.add(mood to item.root)
        }
    }

    private fun renderMood() {
        moodViews.forEach { (mood, view) ->
            view.isSelected = mood.key == selectedMood
            view.background = if (view.isSelected) GradientDrawable().apply {
                setColor(Color.rgb(255,241,241))
                cornerRadius = 12 * resources.displayMetrics.density
                setStroke((resources.displayMetrics.density).toInt().coerceAtLeast(1), Color.rgb(255,148,148))
            } else null
        }
    }
    private fun save() {
        if (!ready || busy || !active()) return
        val content = binding.etContent.text.toString().trim()
        if (DiaryMood.from(selectedMood) == null) { toast("오늘의 감정을 선택해 주세요.")
            return }
        if (content.isBlank()) { binding.etContent.error = "오늘의 이야기를 입력해 주세요."
            return }
        if (content.length > 2000) { toast("일기는 2000자까지 입력할 수 있어요.")
            return }
        busy = true
        binding.btnAction.isEnabled = false
        binding.etContent.isEnabled = false
        binding.tvStatus.text = "일기를 저장하는 중…"
        DiaryRepository.save(uid, date, selectedMood, content, original)
            .addOnSuccessListener {
                if (active()) { toast("일기를 저장했어요.")
                    finish() }
            }.addOnFailureListener {
                if (!active()) return@addOnFailureListener
                busy = false
                binding.btnAction.isEnabled = true
                binding.etContent.isEnabled = true
                binding.tvStatus.text = DiaryRepository.message(it)
            }
    }
    private fun leave() {
        if (busy) { toast("저장이 끝날 때까지 기다려 주세요.")
            return }
        if (binding.etContent.text.toString() != initialText || selectedMood != initialMood) {
            AlertDialog.Builder(this).setTitle("작성을 그만둘까요?").setMessage("저장하지 않은 내용은 사라져요.")
                .setNegativeButton("계속 작성", null).setPositiveButton("나가기") { _, _ -> finish() }.show()
        } else finish()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        if (::binding.isInitialized && ready) {
            outState.putString("draft", binding.etContent.text.toString())
            outState.putString("mood", selectedMood)
            outState.putBoolean("hadOriginal", original != null)
            original?.updatedAt?.let { outState.putLong("seconds", it.seconds)
                outState.putInt("nanos", it.nanoseconds) }
        }
        super.onSaveInstanceState(outState)
    }
}
