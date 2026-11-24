package com.project.maru

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.security.MessageDigest
import java.util.Locale

internal fun normalizeSearchTerm(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
internal fun searchTermId(term: String): String = MessageDigest.getInstance("SHA-256")
    .digest(term.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
internal data class RankedSearch(val term: String, val count: Long)

internal object SearchRanking {
    private val db get() = FirebaseFirestore.getInstance()
    val collection get() = db.collection("searchTerms")
    fun top() = collection.orderBy("count", Query.Direction.DESCENDING).limit(10)

    // 입력 중에는 기록하지 않고 검색 실행 시에만 서버에서 집계한다.
    fun record(raw: String): Task<Boolean> {
        val term=normalizeSearchTerm(raw)
        val uid=FirebaseAuth.getInstance().currentUser?.uid
        if(uid == null || term.isBlank() || term.length > 50 || ContentPolicy.containsAbuse(term)) return Tasks.forResult(false)
        val aggregate=collection.document(searchTermId(term))
        val contribution=aggregate.collection("contributors").document(uid)
        return db.runTransaction { tx ->
            val previous=tx.get(contribution)
            val total=tx.get(aggregate)
            val last=previous.getTimestamp("countedAt")?.toDate()?.time
            if(last != null && System.currentTimeMillis()-last < 60_000) false
            else {
                tx.set(aggregate,mapOf("term" to term,"count" to ((total.getLong("count") ?: 0L)+1L),
                    "updatedAt" to FieldValue.serverTimestamp()))
                tx.set(contribution,mapOf("countedAt" to FieldValue.serverTimestamp()))
                true
            }
        }
    }
}
