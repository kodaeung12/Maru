package com.project.maru

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class Schedule(
    val id: String,
    val title: String,
    val note: String,
    val startsAt: Long,
    val reminderMinutes: Int = 0,
    val revision: String,
    val eventId: String = ""
) {
    val date: LocalDate get() = Instant.ofEpochMilli(startsAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val alarmAt: Long? get() = if (reminderMinutes < 0) null else startsAt - reminderMinutes * 60_000L
    fun canDeliver(now: Long): Boolean = alarmAt?.let { it <= now && startsAt > now - 86_400_000L } ?: false
}

internal object ScheduleRepository {
    fun collection(uid: String) = FirebaseFirestore.getInstance().collection("users").document(uid).collection("schedules")

    fun read(doc: DocumentSnapshot): Schedule = Schedule(
        doc.id, doc.getString("title").orEmpty(), doc.getString("note").orEmpty(),
        doc.getLong("startsAt") ?: 0L, (doc.getLong("reminderMinutes") ?: -1L).toInt(),
        doc.getString("revision").orEmpty(), doc.getString("eventId").orEmpty()
    )

    fun save(uid: String, item: Schedule, expectedRevision: String?): Task<Void> {
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) {
            return Tasks.forException(IllegalStateException("로그인 후 일정을 저장해 주세요."))
        }
        val ref = collection(uid).document(item.id)
        return FirebaseFirestore.getInstance().runTransaction { tx ->
            val old = tx.get(ref)
            val revision = old.getString("revision")
            check(!old.exists() && expectedRevision == null || revision == expectedRevision || revision == item.revision) {
                "다른 기기에서 일정이 변경되었어요. 목록을 새로 확인해 주세요."
            }
            tx.set(ref, mapOf(
                "title" to item.title, "note" to item.note, "startsAt" to item.startsAt,
                "reminderMinutes" to item.reminderMinutes, "revision" to item.revision, "eventId" to item.eventId,
                "createdAt" to (old.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            null
        }
    }

    fun delete(uid: String, item: Schedule): Task<Void> {
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) {
            return Tasks.forException(IllegalStateException("로그인 상태를 확인해 주세요."))
        }
        val ref = collection(uid).document(item.id)
        return FirebaseFirestore.getInstance().runTransaction { tx ->
            val old = tx.get(ref)
            if (old.exists()) {
                check(old.getString("revision") == item.revision) { "일정이 변경되었어요. 다시 확인해 주세요." }
                tx.delete(ref)
            }
            null
        }
    }
}

// 앱 실행 중 계정의 일정 변경을 구독하고 이 기기의 예약 알림도 갱신한다.
internal object ScheduleSync {
    var items: List<Schedule> = emptyList()
        private set
    var error: String? = null
        private set
    var ready = false
        private set
    private var uid: String? = null
    private var generation = 0
    private var registration: ListenerRegistration? = null
    private val listeners = mutableSetOf<() -> Unit>()
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        val appContext = context.applicationContext
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser?.uid
            val request = ++generation
            registration?.remove()
            registration = null
            if (uid != user) ScheduleAlarms.clearOtherAccount(appContext, user)
            uid = user
            items = if (user == null) emptyList() else ScheduleAlarms.cached(appContext, user)
            ready = false
            error = if (user == null) "로그인 후 내 일정을 볼 수 있어요." else null
            changed()
            if (user != null) {
                ScheduleAlarms.restore(appContext)
                registration = ScheduleRepository.collection(user).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, failure ->
                    if (request != generation) return@addSnapshotListener
                    if (failure != null || snapshot == null) {
                        error = "온라인 일정을 확인하지 못했어요. 연결 상태를 확인해 주세요."
                    } else if (!snapshot.metadata.hasPendingWrites()) {
                        items = snapshot.documents.map(ScheduleRepository::read).sortedBy { it.startsAt }
                        ready = true
                        error = null
                        ScheduleAlarms.replace(appContext, user, items)
                    }
                    changed()
                }
            }
        }
    }

    fun subscribe(listener: () -> Unit) { listeners.add(listener); listener() }
    fun unsubscribe(listener: () -> Unit) { listeners.remove(listener) }
    private fun changed() { listeners.toList().forEach { it() } }
}
