package com.project.maru

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

internal object ScheduleUi {
    fun timeLabel(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
    private fun reminderLabel(value: Int) = when(value) { 0 -> "정시 알림"; 60 -> "1시간 전 알림"; 1440 -> "하루 전 알림"; else -> "알림 없음" }

    fun bindHome(fragment: Fragment, view: View) {
        val activity = fragment.requireActivity()
        val calendar = view.findViewById<FamilyCalendarView>(R.id.calendarSchedules)
        val rows = view.findViewById<LinearLayout>(R.id.layoutScheduleItems)
        var selected = calendar.selectedDate
        val render: () -> Unit = {
            val textScheduleDate = view.findViewById<TextView>(R.id.textScheduleDate)
            val dateItems = ScheduleSync.items.filter { it.date == selected }
            textScheduleDate.text = selected.format(DateTimeFormatter.ofPattern("M월 d일", java.util.Locale.KOREAN)) + " 일정"
            calendar.setScheduleDates(ScheduleSync.items.map { it.date }.toSet())
            val addDescription = "${selected.monthValue}월 ${selected.dayOfMonth}일에 일정 추가"
            view.findViewById<View>(R.id.buttonAddSelectedDateSchedule).contentDescription = addDescription
            view.findViewById<View>(R.id.viewSelectedDateDot).visibility =
                if (dateItems.isNotEmpty()) View.VISIBLE else View.GONE
            rows.removeAllViews()
            if (dateItems.isEmpty() && ScheduleSync.ready && ScheduleSync.error == null) rows.addView(TextView(activity).apply {
                text = "등록된 일정이 없어요."
                textSize = 13f
                setTextColor(android.graphics.Color.rgb(156,144,151))
                setPadding(dp(activity,12), dp(activity,16), dp(activity,12), dp(activity,16))
            })
            dateItems.forEach { item ->
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10))
                    background = androidx.core.content.ContextCompat.getDrawable(activity, R.drawable.bg_calendar_agenda)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { showEditor(activity, item = item) }
                }
                row.addView(View(activity).apply {
                    background = androidx.core.content.ContextCompat.getDrawable(activity, R.drawable.bg_calendar_dot)
                }, LinearLayout.LayoutParams(dp(activity, 8), dp(activity, 8)))
                val textColumn = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(activity, 10), 0, 0, 0)
                }
                textColumn.addView(TextView(activity).apply {
                    text = "${timeLabel(item.startsAt).takeLast(5)}  ${item.title}"
                    setTextColor(android.graphics.Color.rgb(48, 48, 48))
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                textColumn.addView(TextView(activity).apply {
                    text = listOf(item.note.takeIf { it.isNotBlank() }, reminderLabel(item.reminderMinutes)).filterNotNull().joinToString(" · ")
                    setTextColor(android.graphics.Color.rgb(135, 128, 126))
                    textSize = 12f
                    setPadding(0, dp(activity, 3), 0, 0)
                })
                row.addView(textColumn, LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                val rowParams = LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                rowParams.bottomMargin = dp(activity, 8)
                rows.addView(row, rowParams)
            }
            val now = System.currentTimeMillis()
            val hasScheduledAlert = ScheduleSync.ready && ScheduleSync.items.any {
                it.startsAt >= now && it.alarmAt?.let { alarmAt -> alarmAt >= now } == true
            }
            view.findViewById<View>(R.id.viewBellDot).visibility = if (hasScheduledAlert) View.VISIBLE else View.GONE
            val stateText = view.findViewById<TextView>(R.id.textScheduleState)
            when {
                ScheduleSync.error != null -> {
                    stateText.text = ScheduleSync.error
                    stateText.visibility = View.VISIBLE
                }
                !ScheduleSync.ready -> {
                    stateText.text = "일정을 불러오는 중이에요."
                    stateText.visibility = View.VISIBLE
                }
                else -> stateText.visibility = View.GONE
            }
        }
        calendar.onDateSelected = { date -> selected = date; render() }
        view.findViewById<View>(R.id.buttonAddSelectedDateSchedule).setOnClickListener { showEditor(activity, selected) }
        view.findViewById<View>(R.id.btnBell).setOnClickListener { showNotifications(activity) }
        fragment.viewLifecycleOwner.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { ScheduleSync.subscribe(render) }
            override fun onStop(owner: LifecycleOwner) { ScheduleSync.unsubscribe(render) }
            override fun onResume(owner: LifecycleOwner) { ScheduleAlarms.restore(activity); render() }
        })
    }

    fun showNotifications(activity: FragmentActivity) {
        val items = ScheduleSync.items.filter { it.startsAt >= System.currentTimeMillis() }.take(30)
        val dialog = AlertDialog.Builder(activity).setTitle("다가오는 일정")
        if(items.isEmpty()) dialog.setMessage(ScheduleSync.error ?: "다가오는 일정이 없어요. 달력에 일정을 추가해 보세요.")
        else dialog.setItems(items.map { "${timeLabel(it.startsAt)}  ${it.title}\n${reminderLabel(it.reminderMinutes)}" }.toTypedArray()) { _, index -> showEditor(activity,item=items[index]) }
        dialog.setPositiveButton("일정 추가") { _, _ -> showEditor(activity) }
            .setNegativeButton("닫기",null).show()
    }

    fun bindBellBadge(fragment: Fragment, dot: View) {
        val update: () -> Unit = {
            val now = System.currentTimeMillis()
            val hasScheduledAlert = ScheduleSync.ready && ScheduleSync.items.any {
                it.startsAt >= now && it.alarmAt?.let { alarmAt -> alarmAt >= now } == true
            }
            dot.visibility = if (hasScheduledAlert) View.VISIBLE else View.GONE
        }
        fragment.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { ScheduleSync.subscribe(update) }
            override fun onStop(owner: LifecycleOwner) { ScheduleSync.unsubscribe(update) }
        })
    }

    fun showEditor(activity: FragmentActivity, date: LocalDate = LocalDate.now(), initialTitle: String = "", initialNote: String = "", eventId: String = "", item: Schedule? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { Toast.makeText(activity,"로그인 후 일정을 추가해 주세요.",Toast.LENGTH_SHORT).show(); return }
        val box = LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL; setPadding(40,16,40,0) }
        val title = EditText(activity).apply { hint="일정 제목"; setSingleLine(); filters=arrayOf(android.text.InputFilter.LengthFilter(100)); setText(item?.title ?: initialTitle) }
        val note = EditText(activity).apply { hint="장소 / 메모"; inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE; maxLines=4; filters=arrayOf(android.text.InputFilter.LengthFilter(1000)); setText(item?.note ?: initialNote) }
        var whenAt = item?.startsAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) } ?: if (date == LocalDate.now()) ZonedDateTime.now().plusHours(1).withSecond(0).withNano(0) else date.atTime(10,0).atZone(ZoneId.systemDefault())
        val dateButton = Button(activity)
        val timeButton = Button(activity)
        fun refresh() { dateButton.text=whenAt.toLocalDate().toString(); timeButton.text=whenAt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) }
        refresh()
        dateButton.setOnClickListener { DatePickerDialog(activity,{ _,y,m,d -> whenAt=LocalDate.of(y,m+1,d).atTime(whenAt.toLocalTime()).atZone(ZoneId.systemDefault()); refresh() },whenAt.year,whenAt.monthValue-1,whenAt.dayOfMonth).show() }
        timeButton.setOnClickListener { TimePickerDialog(activity,{ _,h,m -> whenAt=whenAt.withHour(h).withMinute(m); refresh() },whenAt.hour,whenAt.minute,true).show() }
        val reminders = listOf(0,60,1440,-1)
        val spinner = Spinner(activity).apply { adapter=ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,reminders.map(::reminderLabel)); setSelection(reminders.indexOf(item?.reminderMinutes ?: 0).coerceAtLeast(0)) }
        val error = TextView(activity).apply { setTextColor(android.graphics.Color.RED) }
        listOf(title,note,dateButton,timeButton,spinner,error).forEach(box::addView)
        val builder = AlertDialog.Builder(activity).setTitle(if(item == null) "일정 추가" else "일정 수정").setView(ScrollView(activity).apply { addView(box) }).setPositiveButton("저장",null).setNegativeButton("취소",null)
        if(item != null) builder.setNeutralButton("삭제",null)
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val start = whenAt.toInstant().toEpochMilli()
                val reminder = reminders[spinner.selectedItemPosition]
                val draft = Schedule(item?.id ?: UUID.randomUUID().toString(),title.text.toString().trim(),note.text.toString().trim(),start,reminder,UUID.randomUUID().toString(),item?.eventId ?: eventId)
                when {
                    draft.title.isBlank() -> error.text="일정 제목을 입력해 주세요."
                    start <= System.currentTimeMillis() -> error.text="미래의 날짜와 시간을 선택해 주세요."
                    draft.alarmAt != null && draft.alarmAt!! <= System.currentTimeMillis() -> error.text="알림 시점이 이미 지났어요. 다른 알림 시점을 선택해 주세요."
                    else -> {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
                        ScheduleRepository.save(uid,draft,item?.revision).addOnCompleteListener { result ->
                            if(result.isSuccessful) {
                                if(reminder >= 0 && !ScheduleAlarms.notificationsEnabled(activity)) ActivityCompat.requestPermissions(activity,arrayOf(Manifest.permission.POST_NOTIFICATIONS),780)
                                Toast.makeText(activity,"일정을 저장했어요.",Toast.LENGTH_SHORT).show(); dialog.dismiss()
                            } else { error.text=result.exception?.message ?: "저장하지 못했어요."; dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true }
                        }
                    }
                }
            }
            if(item != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                AlertDialog.Builder(activity).setTitle("일정을 삭제할까요?").setMessage(item.title).setNegativeButton("취소",null).setPositiveButton("삭제") { _, _ ->
                    ScheduleRepository.delete(uid,item).addOnCompleteListener { result -> if(result.isSuccessful) dialog.dismiss() else error.text=result.exception?.message }
                }.show()
            }
        }
        dialog.show()
    }

    private fun dp(activity: FragmentActivity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
