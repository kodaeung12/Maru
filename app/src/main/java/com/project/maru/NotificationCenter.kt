package com.project.maru

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.project.maru.community.CommunitySocial

// 일정은 기존 예약을 유지하고, 확인 여부만 계정과 일정 버전별로 저장한다.
internal object NotificationCenter {
    private fun preferences(context: Context) = context.getSharedPreferences("notification_reads", Context.MODE_PRIVATE)
    private fun key(uid: String, item: Schedule) = "$uid/${item.id}/${item.revision}"
    fun isRead(context: Context, uid: String, item: Schedule) = preferences(context).getBoolean(key(uid, item), false)
    fun markRead(context: Context, uid: String, item: Schedule) {
        preferences(context).edit().putBoolean(key(uid, item), true).apply()
    }
    fun isDismissed(context: Context, uid: String, item: Schedule) =
        preferences(context).getBoolean("hidden/" + key(uid, item), false)
    fun dismiss(context: Context, uid: String, item: Schedule) {
        preferences(context).edit().putBoolean("hidden/" + key(uid, item), true).apply()
        androidx.core.app.NotificationManagerCompat.from(context).cancel("$uid/${item.id}", 0)
    }
    fun schedules(context: Context): List<Schedule> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        val alarms = context.getSharedPreferences("schedule_alarms", Context.MODE_PRIVATE)
        return ScheduleSync.items.filter {
            !isDismissed(context, uid, it) &&
                (it.startsAt >= System.currentTimeMillis() || alarms.getString("delivered:${it.id}", null) == it.revision)
        }
    }
    fun bindBadge(fragment: Fragment, dot: View) {
        var registration: ListenerRegistration? = null
        var unread = 0
        var uid: String? = null
        val update: () -> Unit = {
            val account = FirebaseAuth.getInstance().currentUser?.uid
            val hasSchedule = account != null && schedules(dot.context).any { !isRead(dot.context, account, it) }
            dot.visibility = if (account != null && (unread > 0 || hasSchedule)) View.VISIBLE else View.GONE
        }
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            registration?.remove()
            uid = auth.currentUser?.uid
            unread = 0
            update()
            val account = uid
            if (account != null) registration = MyApplication.db.collection(CommunitySocial.NOTIFICATIONS)
                .whereEqualTo("recipientId", account).addSnapshotListener { snapshot, error ->
                    if (uid == account && FirebaseAuth.getInstance().currentUser?.uid == account && error == null) {
                        unread = snapshot?.documents?.count { it.getBoolean("read") != true } ?: 0
                        update()
                    }
                }
        }
        fragment.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                FirebaseAuth.getInstance().addAuthStateListener(authListener)
                ScheduleSync.subscribe(update)
            }
            override fun onResume(owner: LifecycleOwner) { update() }
            override fun onStop(owner: LifecycleOwner) {
                FirebaseAuth.getInstance().removeAuthStateListener(authListener)
                registration?.remove()
                registration = null
                uid = null
                unread = 0
                ScheduleSync.unsubscribe(update)
            }
        })
    }
}
