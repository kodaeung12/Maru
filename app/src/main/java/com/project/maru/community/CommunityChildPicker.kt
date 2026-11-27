package com.project.maru.community

import android.widget.ArrayAdapter
import android.widget.Spinner
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.MyApplication.Companion.db
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

// 자녀 목록은 본인만 조회하고 공개 글에는 선택한 나이만 저장한다.
class CommunityChildPicker(private val spinner: Spinner) {
    private val ages = mutableListOf(-1)
    private val labels = mutableListOf("자녀 나이 표시 안 함")
    private var generation = 0
    val selectedAge: Int get() = ages.getOrElse(spinner.selectedItemPosition) { -1 }

    fun load(initialAge: Int = -1) {
        val request = ++generation
        ages.clear(); labels.clear()
        ages.add(-1); labels.add("자녀 나이 표시 안 함")
        if (initialAge >= 0) {
            ages.add(initialAge); labels.add("만 ${initialAge}세 자녀 (작성 당시)")
        }
        show(if (initialAge >= 0) 1 else 0)
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("children").get()
            .addOnSuccessListener { snapshot ->
                if (request != generation || auth.currentUser?.uid != uid) return@addOnSuccessListener
                val selection = spinner.selectedItemPosition
                for (document in snapshot) {
                    val age = try {
                        Period.between(LocalDate.parse(document.getString("birthDate")),
                            LocalDate.now(ZoneId.of("Asia/Seoul"))).years
                    } catch (_: Exception) { continue }
                    if (age !in 0..30) continue
                    ages.add(age)
                    labels.add("${document.getString("name").orEmpty().ifBlank { "자녀" }} · 만 ${age}세")
                }
                show(selection)
            }
            // 불러오지 못해도 기본 비공개 선택으로 글을 작성할 수 있다.
            .addOnFailureListener { }
    }

    fun close() { generation++ }

    private fun show(selection: Int) {
        spinner.adapter = ArrayAdapter(spinner.context, android.R.layout.simple_spinner_dropdown_item, labels.toList())
        spinner.setSelection(selection.coerceIn(0, ages.lastIndex))
    }
}
