package com.project.maru.diary

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.project.maru.CalendarSceneryDrawable
import com.project.maru.R
import com.project.maru.calendarCells
import java.time.LocalDate
import java.time.YearMonth

class DiaryCalendarView @JvmOverloads constructor(context: Context, attrs: AttributeSet?=null) : LinearLayout(context,attrs) {
    var selectedDate: LocalDate=DiaryDates.today()
        private set
    var month: YearMonth=YearMonth.from(selectedDate)
        private set
    var onDateSelected: ((LocalDate)->Unit)?=null
    var onMonthChanged: (() -> Unit)? = null
    private var moods: Map<String, String> = emptyMap()
    private val pink=Color.rgb(226,100,134)
    private val lavender=Color.rgb(155,122,202)
    private val mint=Color.rgb(97,190,158)
    private val ink=Color.rgb(53,45,49)
    private val muted=Color.rgb(156,144,151)

    init {
        orientation=VERTICAL
        setPadding(dp(8),0,dp(8),0)
        background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(255,221,232),Color.rgb(255,240,245),Color.rgb(255,248,246))).apply {
            cornerRadius=dp(24).toFloat()
            setStroke(dp(1),Color.rgb(244,199,214))
        }
        render()
    }
    fun setEntries(entries: List<DiaryEntry>) {
        moods = entries.associate { it.date to it.mood }
        render()
    }
    fun show(date: LocalDate) {
        selectedDate = date
        month = YearMonth.from(date)
        render()
    }
    private fun selectDate(date: LocalDate) {
        show(date)
        onDateSelected?.invoke(date)
    }
    private fun moveMonth(amount: Long) {
        val next = month.plusMonths(amount)
        if (next.year !in 1900..2100) return
        show(next.atDay(selectedDate.dayOfMonth.coerceAtMost(next.lengthOfMonth())))
        onMonthChanged?.invoke()
        onDateSelected?.invoke(selectedDate)
    }
    private fun dp(value: Int)=(value*resources.displayMetrics.density).toInt()
    private fun circle(color: Int)=GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(color) }
    private fun label(text: String,size: Float,color: Int)=TextView(context).apply {
        this.text=text; textSize=size; setTextColor(color); gravity=Gravity.CENTER
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private fun render() {
        removeAllViews()
        val today=DiaryDates.today()
        val scene=FrameLayout(context).apply { background=CalendarSceneryDrawable() }
        val sceneRow=LinearLayout(context).apply {
            gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(42),dp(6),dp(8),dp(4))
        }
        val heading=LinearLayout(context).apply { orientation=VERTICAL; gravity=Gravity.CENTER_VERTICAL }
        heading.addView(label("마루일기",20f,ink).apply {
            tag="calendar-title"; gravity=Gravity.START; maxLines=1
            setTypeface(typeface,Typeface.BOLD)
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
            isAccessibilityHeading=true
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this,14,20,1,android.util.TypedValue.COMPLEX_UNIT_SP)
        },LayoutParams(LayoutParams.MATCH_PARENT,dp(30)))
        heading.addView(label("아이와 함께한 하루의 마음",12f,muted).apply {
            gravity=Gravity.START or Gravity.CENTER_VERTICAL; maxLines=1
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this,10,12,1,android.util.TypedValue.COMPLEX_UNIT_SP)
        },LayoutParams(LayoutParams.MATCH_PARENT,dp(24)))
        sceneRow.addView(heading,LayoutParams(0,LayoutParams.MATCH_PARENT,1f))
        sceneRow.addView(android.widget.ImageView(context).apply {
            setImageResource(R.drawable.img_family_hero)
            scaleType=android.widget.ImageView.ScaleType.FIT_CENTER
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        },LayoutParams(dp(72),dp(70)))
        scene.addView(sceneRow,FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT))
        addView(scene,LayoutParams(LayoutParams.MATCH_PARENT,dp(84)))
        val header=LinearLayout(context).apply {
            gravity=Gravity.CENTER_VERTICAL
            background=GradientDrawable().apply { setColor(Color.WHITE); cornerRadius=dp(24).toFloat() }
        }
        fun arrow(text: String, description: String, key: String, action: ()->Unit)=label(text,28f,pink).apply {
            contentDescription=description; tag=key; importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
            isClickable=true; isFocusable=true
            setOnClickListener { action() }
        }
        header.addView(arrow("‹","이전 달","calendar-previous") { moveMonth(-1) },LayoutParams(dp(48),dp(48)))
        header.addView(label("${month.year}년 ${month.monthValue}월",16f,pink).apply {
            tag="calendar-month"; setTypeface(typeface,Typeface.BOLD); maxLines=1
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this,12,16,1,android.util.TypedValue.COMPLEX_UNIT_SP)
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
            accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE
        },LayoutParams(0,dp(48),1f))
        header.addView(arrow("›","다음 달","calendar-next") { moveMonth(1) },LayoutParams(dp(48),dp(48)))
        addView(header,LayoutParams(LayoutParams.MATCH_PARENT,dp(50)).apply { setMargins(dp(12),0,dp(12),dp(10)) })
        val weekdays=LinearLayout(context).apply {
            background=GradientDrawable().apply { setColor(Color.rgb(255,223,233)); cornerRadius=dp(16).toFloat() }
        }
        listOf("일","월","화","수","목","금","토").forEachIndexed { index,name ->
            weekdays.addView(label(name,12f,when(index) { 0 -> pink; 6 -> lavender; else -> muted }),LayoutParams(0,dp(28),1f))
        }
        addView(weekdays)
        calendarCells(month).chunked(7).forEach { week ->
            val row=LinearLayout(context)
            week.forEach { date ->
                val cell=FrameLayout(context)
                row.addView(cell,LayoutParams(0,dp(56),1f))
                if(date != null) {
                    val selected=date==selectedDate
                    val isToday=date==today
                    val mood=DiaryMood.from(moods[date.toString()].orEmpty())
                    cell.tag="calendar-day-$date"
                    cell.isSelected=selected; cell.isClickable=true; cell.isFocusable=true
                    cell.contentDescription="${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일"+
                        (if(isToday) ", 오늘" else "")+(if(selected) ", 선택됨" else "")+(mood?.let { ", ${it.label} 일기 있음" } ?: ", 일기 없음")
                    cell.setOnClickListener { selectDate(date) }
                    val number=label(date.dayOfMonth.toString(),14f,when {
                        selected -> Color.WHITE
                        isToday -> lavender
                        date.dayOfWeek==java.time.DayOfWeek.SUNDAY -> Color.rgb(198,112,127)
                        date.dayOfWeek==java.time.DayOfWeek.SATURDAY -> lavender
                        else -> ink
                    }).apply {
                        includeFontPadding=false
                        if(selected || isToday) {
                            background=GradientDrawable().apply {
                                cornerRadius=dp(12).toFloat()
                                if(selected) {
                                    orientation=GradientDrawable.Orientation.TL_BR
                                    colors=intArrayOf(Color.rgb(240,130,161),Color.rgb(222,96,137))
                                } else setColor(Color.rgb(241,233,252))
                                if(isToday) setStroke(dp(2),lavender)
                            }
                            setTypeface(typeface,Typeface.BOLD)
                        }
                    }
                    cell.addView(number,FrameLayout.LayoutParams(dp(34),dp(34),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin=dp(3) })
                    if(mood != null) cell.addView(android.widget.ImageView(context).apply {
                        tag="diary-mood-$date"
                        setImageResource(mood.image)
                        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                    },FrameLayout.LayoutParams(dp(16),dp(16),Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin=dp(2) })
                }
            }
            addView(row)
        }
        addView(View(context).apply {
            background=CalendarSceneryDrawable(landscape=true)
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        },LayoutParams(LayoutParams.MATCH_PARENT,dp(28)))
        val footer=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(dp(6),dp(4),dp(6),dp(4)) }
        val legend=LinearLayout(context).apply { gravity=Gravity.CENTER }
        listOf("오늘" to lavender,"선택" to pink,"일기" to mint).forEach { (name,color) ->
            val item=LinearLayout(context).apply {
                tag="calendar-legend-$name"; gravity=Gravity.CENTER_VERTICAL
                contentDescription=if(name == "일기") "일기를 쓴 날에는 감정 아이콘이 표시돼요" else "$name 표시"; importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
                setPadding(dp(8),0,dp(8),0)
            }
            if(name == "일기") {
                item.addView(android.widget.ImageView(context).apply {
                    setImageResource(R.drawable.diary_good)
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                },LayoutParams(dp(14),dp(14)))
            } else item.addView(View(context).apply {
                background=circle(color); importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
            },LayoutParams(dp(8),dp(8)))
            item.addView(label(name,12f,ink),LayoutParams(LayoutParams.WRAP_CONTENT,dp(40)).apply { leftMargin=dp(5) })
            legend.addView(item)
        }
        footer.addView(legend,LayoutParams(0,dp(48),1f))

        addView(footer)

    }
    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable("super",super.onSaveInstanceState())
        putString("date",selectedDate.toString())
    }
    override fun onRestoreInstanceState(state: Parcelable?) {
        if(state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable("super",Parcelable::class.java))
            runCatching { LocalDate.parse(state.getString("date")) }.getOrNull()?.let(::selectDate)
        } else super.onRestoreInstanceState(state)
    }
}
