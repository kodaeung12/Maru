package com.project.maru.experience

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.project.maru.eventReviewId

internal object FavoriteRepository {
    fun collection(uid: String) = FirebaseFirestore.getInstance()
        .collection("users").document(uid).collection("favorites")

    fun id(event: Experience): String = eventReviewId(event.title, event.location, event.date)

    // 명시한 저장 상태를 적용한다. 같은 요청을 재시도해도 반대로 토글되지 않는다.
    fun setSaved(uid: String, event: Experience, saved: Boolean): Task<Void> {
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) {
            return Tasks.forException(IllegalStateException("로그인 후 즐겨찾기를 이용해 주세요."))
        }
        val eventId = id(event)
        val ref = collection(uid).document(eventId)
        return FirebaseFirestore.getInstance().runTransaction { transaction ->
            val current = transaction.get(ref)
            if (!saved) {
                transaction.delete(ref)
            } else if (!current.exists()) {
                transaction.set(ref, mapOf(
                    "eventId" to eventId,
                    "title" to event.title,
                    "location" to event.location,
                    "date" to event.date,
                    "category" to event.category,
                    "imageUrl" to event.imageUrl.orEmpty(),
                    "targetAge" to event.targetAge,
                    "region" to event.region,
                    "startDate" to event.startDate?.toString().orEmpty(),
                    "endDate" to event.endDate?.toString().orEmpty(),
                    "savedAt" to FieldValue.serverTimestamp()
                ))
            }
            null
        }
    }

    fun read(document: DocumentSnapshot): Experience = Experience(
        title = document.getString("title").orEmpty(),
        location = document.getString("location").orEmpty(),
        date = document.getString("date").orEmpty(),
        category = document.getString("category").orEmpty(),
        imageUrl = document.getString("imageUrl")?.takeIf { it.isNotBlank() },
        targetAge = document.getString("targetAge").orEmpty(),
        region = document.getString("region").orEmpty(),
        startDate = ExperiencePeriod.parseApiDate(document.getString("startDate")),
        endDate = ExperiencePeriod.parseApiDate(document.getString("endDate"))
    )
}

// 화면이 보일 때만 구독하고 계정이 바뀌면 이전 계정 상태를 즉시 비운다.
internal class FavoriteStore(
    private val owner: LifecycleOwner,
    private val onError: (String) -> Unit
) : DefaultLifecycleObserver {
    private val auth = FirebaseAuth.getInstance()
    private var registration: ListenerRegistration? = null
    private var generation = 0
    private var uid: String? = null
    private val pendingIds = mutableSetOf<String>()
    private val authListener = FirebaseAuth.AuthStateListener { listen(it.currentUser?.uid) }
    var onChanged: () -> Unit = {}
    var items: List<Experience> = emptyList()
        private set
    var ready = false
        private set
    var error: String? = null
        private set
    val signedIn: Boolean get() = uid != null

    init { owner.lifecycle.addObserver(this) }

    override fun onStart(owner: LifecycleOwner) { auth.addAuthStateListener(authListener) }

    override fun onStop(owner: LifecycleOwner) {
        auth.removeAuthStateListener(authListener)
        registration?.remove()
        registration = null
        generation++
        pendingIds.clear()
        ready = false
    }

    override fun onDestroy(owner: LifecycleOwner) { owner.lifecycle.removeObserver(this) }

    fun reload() { listen(auth.currentUser?.uid) }

    private fun listen(userId: String?) {
        val request = ++generation
        registration?.remove()
        registration = null
        uid = userId
        items = emptyList()
        pendingIds.clear()
        ready = false
        error = if (userId == null) "로그인 후 즐겨찾기를 이용해 주세요." else null
        onChanged()
        if (userId == null) return
        registration = FavoriteRepository.collection(userId).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, failure ->
            if (request != generation) return@addSnapshotListener
            if (failure != null || snapshot == null) {
                items = emptyList()
                ready = false
                error = "즐겨찾기를 불러오지 못했어요. 연결 상태를 확인하고 다시 시도해 주세요."
            } else {
                // 서버에 승인되지 않은 쓰기를 저장 완료로 표시하지 않는다.
                if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                items = snapshot.documents.sortedByDescending { it.getTimestamp("savedAt") }
                    .map(FavoriteRepository::read)
                ready = true
                error = null
            }
            onChanged()
        }
    }

    fun isSaved(event: Experience): Boolean = items.any { FavoriteRepository.id(it) == FavoriteRepository.id(event) }
    fun isPending(event: Experience): Boolean = FavoriteRepository.id(event) in pendingIds

    fun toggle(event: Experience) {
        val userId = uid
        if (userId == null || !ready) {
            onError(error ?: "즐겨찾기를 확인 중이에요. 잠시 후 다시 눌러 주세요.")
            return
        }
        val eventId = FavoriteRepository.id(event)
        if (!pendingIds.add(eventId)) return
        val request = generation
        val save = !isSaved(event)
        onChanged()
        FavoriteRepository.setSaved(userId, event, save).addOnCompleteListener { task ->
            if (request != generation) return@addOnCompleteListener
            pendingIds.remove(eventId)
            onChanged()
            if (!task.isSuccessful) onError("즐겨찾기를 변경하지 못했어요. 인터넷 연결을 확인하고 다시 시도해 주세요.")
        }
    }
}
