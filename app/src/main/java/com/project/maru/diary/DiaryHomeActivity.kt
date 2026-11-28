package com.project.maru.diary

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.Source
import com.project.maru.databinding.ActivityDiaryHomeBinding
import java.time.LocalDate

class DiaryHomeActivity : DiaryActivity() {
    private lateinit var binding: ActivityDiaryHomeBinding
    private var entries: List<DiaryEntry> = emptyList()
    private var requestNumber = 0
    private var ready = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        binding = ActivityDiaryHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inset(binding.root)
        binding.header.btnBack.setOnClickListener { finish() }
        savedInstanceState?.getString("date")?.let { binding.calendar.show(LocalDate.parse(it)) }
        binding.calendar.onDateSelected = { showSelected() }
        binding.calendar.onMonthChanged = { load() }
        binding.btnAction.setOnClickListener { openSelected() }
        binding.tvPreview.setOnClickListener { if (ready) openSelected() }
        binding.btnRetry.setOnClickListener { load() }
        binding.btnWeek.setOnClickListener {
            if (!ready) return@setOnClickListener
            val weekly = weekly()
            val text = if (weekly.isEmpty()) "아직 감정 기록이 없어요. 일기를 쓰고 나의 마음을 돌아보세요."
                else DiaryMood.entries.joinToString("\n") { mood -> "${mood.label}  ${weekly.count { it.mood == mood.key }}일" }
            AlertDialog.Builder(this).setTitle("이번 주 내 감정").setMessage(text + "\n\n감정 요약은 자기 기록을 바탕으로 하며 진단이 아니에요.")
                .setPositiveButton("확인", null).show()
        }
    }
    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) load()
    }

    override fun onStop() {
        requestNumber++
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("date", binding.calendar.selectedDate.toString())
        super.onSaveInstanceState(outState)
    }
    private fun load() {
        if (!active()) return
        val number = ++requestNumber
        ready = false
        entries = emptyList()
        binding.calendar.setEntries(emptyList())
        binding.tvStatus.text = "일기를 불러오는 중…"
        binding.btnRetry.visibility = View.GONE
        binding.btnAction.isEnabled = false
        showSelected()
        val month = binding.calendar.month
        val week = DiaryDates.weekStart(DiaryDates.today())
        binding.tvWeekRange.text = "${week.monthValue}.${week.dayOfMonth} ~ ${week.plusDays(6).monthValue}.${week.plusDays(6).dayOfMonth}"
        binding.root.postDelayed({
            if (active() && number == requestNumber && !ready) {
                requestNumber++
                binding.tvStatus.text = "서버 응답이 늦어지고 있어요. 인터넷 연결 후 다시 불러와 주세요."
                binding.tvPreview.text = "저장된 일기를 확인하지 못했어요."
                binding.btnRetry.visibility = View.VISIBLE
            }
        }, 20000)
        val from = minOf(month.atDay(1), week)
        val to = maxOf(month.atEndOfMonth(), week.plusDays(6))
        DiaryRepository.entries(uid).whereGreaterThanOrEqualTo("date", from.toString())
            .whereLessThanOrEqualTo("date", to.toString()).get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                if (!active() || number != requestNumber) return@addOnSuccessListener
                val diaryList = mutableListOf<DiaryEntry>()
                for (document in snapshot) {
                    val entry = document.toObject(DiaryEntry::class.java)
                    if (entry != null) diaryList.add(entry)
                }
                entries = diaryList
                ready = true
                binding.calendar.setEntries(entries)
                binding.tvStatus.text = ""
                val weekly = weekly()
                binding.tvSummary.text = DiaryMood.summary(weekly.map { it.mood })
                binding.tvSummaryDetail.text = if (weekly.isEmpty()) "일기를 작성하면 이번 주 마음을 볼 수 있어요." else "이번 주 ${weekly.size}일의 마음을 기록했어요. 눌러서 확인하기"
                binding.tvWeekRange.text = "${week.monthValue}.${week.dayOfMonth} ~ ${week.plusDays(6).monthValue}.${week.plusDays(6).dayOfMonth}"
                showSelected()
            }.addOnFailureListener {
                if (!active() || number != requestNumber) return@addOnFailureListener
                requestNumber++
                binding.tvStatus.text = DiaryRepository.message(it)
                binding.btnRetry.visibility = View.VISIBLE
                binding.tvPreview.text = "일기를 불러오지 못했어요. 다시 불러오기를 눌러 주세요."
            }
    }
    private fun weekly(): List<DiaryEntry> {
        val week = DiaryDates.weekStart(DiaryDates.today())
        return entries.filter { it.date >= week.toString() && it.date <= week.plusDays(6).toString() }
    }
    private fun showSelected() {
        val date = binding.calendar.selectedDate
        val entry = entries.firstOrNull { it.date == date.toString() }
        binding.tvSelectedDate.text = if (date == DiaryDates.today()) "오늘의 일기" else "${date.monthValue}월 ${date.dayOfMonth}일의 일기"
        binding.tvPreview.text = when {
            !ready -> "일기를 불러오는 중…"
            entry != null -> "${DiaryMood.from(entry.mood)?.label.orEmpty()} · ${entry.content.take(100)}\n일기 상세 보기 ›"
            date > DiaryDates.today() -> "미래의 일기는 아직 작성할 수 없어요."
            else -> "아직 작성한 일기가 없어요. 오늘의 마음을 남겨 보세요."
        }
        binding.btnAction.text = when {
            entry != null -> "일기 확인하기"
            date == DiaryDates.today() -> "오늘의 일기 등록하기"
            else -> "선택한 날짜의 일기 등록하기"
        }
        binding.btnAction.isEnabled = ready && date <= DiaryDates.today()
        binding.btnAction.alpha = if (binding.btnAction.isEnabled) 1f else 0.45f
    }
    private fun openSelected() {
        val date = binding.calendar.selectedDate
        if (!ready || date > DiaryDates.today()) return
        val target = if (entries.any { it.date == date.toString() }) DiaryDetailActivity::class.java else DiaryEditorActivity::class.java
        startActivity(Intent(this, target).putExtra("date", date.toString()))
    }
}
