package com.project.maru

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

data class Review(
    val name: String = "",
    val rating: Int = 0,
    val date: String = "",
    val content: String = "",
    val ownerId: String = ""
)

// 같은 제목이라도 장소/일정이 다르면 별도의 행사로 취급한다.
internal fun eventReviewId(title: String, place: String, date: String): String {
    val identity = listOf(title, place, date).joinToString("") {
        val value = it.trim()
        "${value.length}:$value"
    }
    return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

object ReviewRepository {
    private fun reviews(eventId: String) = FirebaseFirestore.getInstance()
        .collection("events").document(eventId).collection("reviews")

    fun load(eventId: String): Task<List<Review>> = reviews(eventId).get(Source.SERVER).continueWith { task ->
        task.result.documents.sortedByDescending { it.getTimestamp("createdAt")?.seconds ?: 0L }.map { doc ->
            Review(
                name = doc.getString("authorName").orEmpty(),
                rating = (doc.getLong("rating") ?: 0L).toInt().coerceIn(1, 5),
                date = doc.getTimestamp("createdAt")?.toDate()?.let {
                    SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(it)
                }.orEmpty(),
                content = doc.getString("content").orEmpty(),
                ownerId = doc.getString("ownerId").orEmpty()
            )
        }
    }

    // 행사별 1인 1개 문서. 재시도 시 중복되지 않고 기존 리뷰를 수정한다.
    fun save(eventId: String, rating: Int, content: String): Task<Void> {
        val user = requireNotNull(FirebaseAuth.getInstance().currentUser) { "로그인 후 작성해 주세요." }
        require(rating in 1..5 && content.length in 1..2000)
        if (ContentPolicy.containsAbuse(content)) {
            return com.google.android.gms.tasks.Tasks.forException(IllegalArgumentException(ContentPolicy.MESSAGE))
        }
        val ref = reviews(eventId).document(user.uid)
        return FirebaseFirestore.getInstance().runTransaction { transaction ->
            val previous = transaction.get(ref)
            transaction.set(ref, mapOf(
                "ownerId" to user.uid,
                "authorName" to (user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore("@") ?: "마루 이용자").take(100),
                "rating" to rating,
                "content" to content,
                "createdAt" to (previous.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            null
        }
    }
}
