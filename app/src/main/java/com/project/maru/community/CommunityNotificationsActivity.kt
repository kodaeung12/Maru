package com.project.maru.community

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.MyApplication.Companion.db
import com.project.maru.databinding.ActivityCommunityNotificationsBinding
import com.project.maru.databinding.ItemCommunityNotificationBinding
import java.text.SimpleDateFormat
import java.util.Locale

class CommunityNotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommunityNotificationsBinding
    private var registration: ListenerRegistration? = null
    private var rows: List<DocumentSnapshot> = emptyList()
    private val adapter = NotificationAdapter { document ->
        // 글이 이미 삭제됐더라도 상세 화면에서 안내하고, 알림 자체는 읽음으로 처리한다.
        document.reference.update("read", true).addOnSuccessListener {
            if (!isFinishing && !isDestroyed) startActivity(Intent(this, CommunityDetailActivity::class.java)
                .putExtra("postId", document.getString("postId")))
        }.addOnFailureListener { Toast.makeText(this, "읽음 처리에 실패했어요. 다시 눌러 주세요.", Toast.LENGTH_SHORT).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSchedules.setOnClickListener { com.project.maru.ScheduleUi.showNotifications(this) }
        binding.recyclerNotifications.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotifications.adapter = adapter
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid
        if (uid == null) { binding.tvStatus.text = "로그인 후 알림을 확인해 주세요."; return }
        binding.tvStatus.text = "알림을 불러오는 중…"
        registration = db.collection(CommunitySocial.NOTIFICATIONS).whereEqualTo("recipientId", uid)
            .addSnapshotListener { snapshot, error ->
                if (isFinishing || isDestroyed) return@addSnapshotListener
                if (error != null || snapshot == null) {
                    binding.tvStatus.text = "알림을 불러오지 못했어요. 연결 상태와 Firestore 규칙을 확인해 주세요."
                } else {
                    rows = snapshot.documents.sortedByDescending { it.getTimestamp("createdAt") }
                    adapter.submit(rows)
                    val unread = rows.count { it.getBoolean("read") != true }
                    binding.tvStatus.text = if (rows.isEmpty()) "아직 받은 알림이 없어요." else "읽지 않은 알림 ${unread}개"
                }
            }
    }

    override fun onStop() { registration?.remove(); registration = null; super.onStop() }
}

private class NotificationAdapter(private val onClick: (DocumentSnapshot) -> Unit) : RecyclerView.Adapter<NotificationAdapter.Holder>() {
    private var rows: List<DocumentSnapshot> = emptyList()
    class Holder(val binding: ItemCommunityNotificationBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemCommunityNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = rows.size
    override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
        val row = rows[position]
        tvMessage.text = if (row.getString("type") == "like") "회원님의 게시글에 공감이 달렸어요!"
            else "회원님의 게시글에 새로운 댓글이 달렸어요!"
        tvTitle.text = row.getString("postTitle").orEmpty()
        tvDate.text = row.getTimestamp("createdAt")?.toDate()?.let {
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(it)
        }.orEmpty()
        tvRead.text = if (row.getBoolean("read") == true) "읽음" else "새 알림"
        root.alpha = if (row.getBoolean("read") == true) 0.65f else 1f
        root.setOnClickListener { onClick(row) }
    }
    fun submit(items: List<DocumentSnapshot>) { rows = items; notifyDataSetChanged() }
}
