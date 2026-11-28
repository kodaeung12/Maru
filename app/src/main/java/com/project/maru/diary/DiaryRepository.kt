package com.project.maru.diary

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.project.maru.MyApplication.Companion.db
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

data class DiaryEntry(
    val date: String = "",
    val mood: String = "",
    val content: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

object DiaryDates {
    fun today(): LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))
    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

// 날짜를 문서 ID로 사용해 하루에 한 편씩 저장한다. 모든 경로는 로그인 계정 아래에 둔다.
object DiaryRepository {
    fun entries(uid: String) = db.collection("users").document(uid).collection("diaries")

    fun save(uid: String, date: LocalDate, mood: String, content: String, original: DiaryEntry?) =
        db.runTransaction { transaction ->
            val ref = entries(uid).document(date.toString())
            val current = transaction.get(ref)
            if ((original == null && current.exists()) ||
                (original != null && (!current.exists() || current.getTimestamp("updatedAt") != original.updatedAt))) {
                throw IllegalStateException("일기가 다른 화면에서 변경됐어요. 다시 불러온 뒤 저장해 주세요.")
            }
            val values = hashMapOf<String, Any>(
                "date" to date.toString(), "mood" to mood, "content" to content.trim(),
                "createdAt" to (original?.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            transaction.set(ref, values)
            null
        }

    fun delete(uid: String, entry: DiaryEntry) = db.runTransaction { transaction ->
        val ref = entries(uid).document(entry.date)
        val current = transaction.get(ref)
        if (current.exists() && current.getTimestamp("updatedAt") != entry.updatedAt) {
            throw IllegalStateException("일기가 변경됐어요. 다시 확인한 뒤 삭제해 주세요.")
        }
        if (current.exists()) transaction.delete(ref)
        null
    }

    fun message(error: Exception): String {
        var cause: Throwable = error
        while (cause.cause != null) cause = cause.cause!!
        if (cause is IllegalStateException) return cause.message ?: "일기를 다시 불러와 주세요."
        return when ((error as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "일기 접근 권한이 없어요. Firestore 일기 규칙 게시 여부를 확인해 주세요."
            else -> "일기를 처리하지 못했어요. 인터넷 연결을 확인하고 다시 시도해 주세요."
        }
    }
}
