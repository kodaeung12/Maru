package com.project.maru.community

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.MyApplication.Companion.db
import com.project.maru.NotificationCenter
import com.project.maru.Schedule
import com.project.maru.ScheduleSync
import com.project.maru.ScheduleUi
import com.project.maru.R
import com.project.maru.databinding.ActivityCommunityNotificationsBinding
import com.project.maru.databinding.ItemCommunityNotificationBinding

// 홈, 커뮤니티, 마이페이지에서 함께 사용하는 알림 화면.
class CommunityNotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommunityNotificationsBinding
    private var registration: ListenerRegistration? = null
    private var documents: List<DocumentSnapshot> = emptyList()
    private var account: String? = null
    private var generation = 0
    private var communityError = false
    private val scheduleChanged: () -> Unit = { render() }
    private val adapter = NotificationAdapter({ row -> open(row) }, { row -> confirmDelete(row) })
    private val deleting = mutableSetOf<String>()
    private val authChanged = FirebaseAuth.AuthStateListener { firebaseAuth ->
        registration?.remove()
        registration = null
        val request = ++generation
        account = firebaseAuth.currentUser?.uid
        documents = emptyList()
        communityError = false
        render()
        val uid = account
        if (uid != null) registration = db.collection(CommunitySocial.NOTIFICATIONS)
            .whereEqualTo("recipientId", uid).addSnapshotListener { snapshot, error ->
                if (request != generation || auth.currentUser?.uid != uid || isFinishing || isDestroyed) return@addSnapshotListener
                communityError = error != null
                if (snapshot != null && error == null) documents = snapshot.documents
                render()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerNotifications.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotifications.adapter = adapter
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authChanged)
        ScheduleSync.subscribe(scheduleChanged)
    }
    override fun onStop() {
        ++generation
        auth.removeAuthStateListener(authChanged)
        registration?.remove()
        registration = null
        ScheduleSync.unsubscribe(scheduleChanged)
        super.onStop()
    }
    private fun render() {
        val uid = auth.currentUser?.uid
        val rows = if (uid == null || uid != account) emptyList() else {
            val community = documents.map { document ->
                NotificationRow(document.getString("type").orEmpty(), document.getString("postTitle").orEmpty(),
                    document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    document.getBoolean("read") == true, document = document)
            }
            val schedules = NotificationCenter.schedules(this).map { item ->
                NotificationRow("schedule", item.title, item.alarmAt ?: item.startsAt,
                    NotificationCenter.isRead(this, uid, item), schedule = item)
            }
            (community + schedules).sortedByDescending { it.time }
        }
        adapter.submit(rows)
        binding.tvStatus.text = when {
            uid == null -> "로그인 후 알림을 확인해 주세요."
            communityError || ScheduleSync.error != null -> "일부 알림을 불러오지 못했어요. 연결 상태를 확인해 주세요."
            rows.isEmpty() -> "아직 받은 알림이나 다가오는 일정이 없어요."
            else -> "읽지 않은 알림 ${rows.count { !it.read }}개 · 일정·공감·댓글"
        }
    }
    private fun confirmDelete(row: NotificationRow) {
        val uid = account ?: return
        if (auth.currentUser?.uid != uid) return
        val request = generation
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("알림을 삭제할까요?")
            .setMessage("알림 목록에서만 삭제돼요. 원래 게시글과 일정은 유지됩니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                if (request != generation || auth.currentUser?.uid != uid) return@setPositiveButton
                val schedule = row.schedule
                if (schedule != null) {
                    NotificationCenter.dismiss(this, uid, schedule)
                    render()
                    Toast.makeText(this, "알림을 삭제했어요.", Toast.LENGTH_SHORT).show()
                } else {
                    val document = row.document ?: return@setPositiveButton
                    if (!deleting.add(document.id)) return@setPositiveButton
                    document.reference.delete().addOnCompleteListener { task ->
                        deleting.remove(document.id)
                        if (request != generation || auth.currentUser?.uid != uid || isFinishing || isDestroyed)
                            return@addOnCompleteListener
                        if (task.isSuccessful) {
                            documents = documents.filterNot { it.id == document.id }
                            render()
                        }
                        Toast.makeText(this, if (task.isSuccessful) "알림을 삭제했어요." else
                            "알림을 삭제하지 못했어요. 연결 상태와 권한을 확인해 주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.show()
    }

    private fun open(row: NotificationRow) {
        val uid = account ?: return
        if (auth.currentUser?.uid != uid) return
        val schedule = row.schedule
        if (schedule != null) {
            val current = ScheduleSync.items.firstOrNull { it.id == schedule.id }
            if (current == null) {
                Toast.makeText(this, "삭제된 일정이에요.", Toast.LENGTH_SHORT).show()
                render()
                return
            }
            NotificationCenter.markRead(this, uid, current)
            render()
            ScheduleUi.showEditor(this, item = current)
        } else {
            val document = row.document ?: return
            val request = generation
            document.reference.update("read", true).addOnSuccessListener {
                if (request == generation && auth.currentUser?.uid == uid && !isFinishing && !isDestroyed)
                    startActivity(Intent(this, CommunityDetailActivity::class.java).putExtra("postId", document.getString("postId")))
            }.addOnFailureListener {
                if (auth.currentUser?.uid == uid && !isDestroyed)
                    Toast.makeText(this, "읽음 처리에 실패했어요. 다시 눌러 주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private data class NotificationRow(val type: String, val title: String, val time: Long, val read: Boolean,
    val document: DocumentSnapshot? = null, val schedule: Schedule? = null)

private class NotificationAdapter(private val onClick: (NotificationRow) -> Unit,
    private val onDelete: (NotificationRow) -> Unit) : RecyclerView.Adapter<NotificationAdapter.Holder>() {
    private var rows: List<NotificationRow> = emptyList()
    class Holder(val binding: ItemCommunityNotificationBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemCommunityNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = rows.size
    override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
        val row = rows[position]
        tvMessage.text = when (row.type) {
            "schedule" -> if (row.schedule!!.startsAt > System.currentTimeMillis()) "다가오는 일정" else "일정 알림"
            "like" -> "공감이 달렸어요"
            else -> "댓글이 달렸어요"
        }
        ivType.setImageResource(when (row.type) {
            "schedule" -> R.drawable.ic_calendar
            "like" -> R.drawable.community_figma_heart_filled
            else -> R.drawable.community_figma_chat
        })
        iconBadge.setBackgroundResource(if (row.type == "like") R.drawable.community_note_like else R.drawable.community_note_comment)
        tvTitle.text = row.title
        tvDate.text = row.schedule?.let { ScheduleUi.timeLabel(it.startsAt) } ?: CommunityPresentation.time(row.time)
        tvRead.text = "새 알림"
        tvRead.visibility = if (row.read) View.GONE else View.VISIBLE
        root.alpha = if (row.read) 0.8f else 1f
        root.setOnClickListener { onClick(row) }
        btnDeleteNotification.contentDescription = "${row.title} 알림 삭제"
        btnDeleteNotification.setOnClickListener { onDelete(row) }
    }
    fun submit(items: List<NotificationRow>) { rows = items
        notifyDataSetChanged() }
}
