package com.project.maru

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

internal object ScheduleAlarms {
    const val CHANNEL = "maru_schedules"
    private const val PREFS = "schedule_alarms"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun initialize(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "내 일정 알림", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "직접 등록한 일정의 예약 알림" }
        )
    }

    fun notificationsEnabled(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    fun cached(context: Context, uid: String): List<Schedule> {
        val preferences = prefs(context)
        if (preferences.getString("uid", null) != uid) return emptyList()
        return runCatching {
            val array = JSONArray(preferences.getString("items", "[]"))
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                Schedule(item.getString("id"), item.getString("title"), item.optString("note"),
                    item.getLong("startsAt"), item.getInt("reminderMinutes"), item.getString("revision"), item.optString("eventId"))
            }
        }.getOrDefault(emptyList())
    }

    private fun alarmIntent(context: Context, uid: String, item: Schedule): PendingIntent {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java)
            .setAction("com.project.maru.SCHEDULE_ALARM")
            .setData(Uri.Builder().scheme("maru").authority("schedule").appendPath(uid).appendPath(item.id).build())
            .putExtra("uid", uid).putExtra("id", item.id).putExtra("revision", item.revision)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun cancel(context: Context, uid: String, item: Schedule) {
        val intent = alarmIntent(context, uid, item)
        context.getSystemService(AlarmManager::class.java).cancel(intent)
        intent.cancel()
        NotificationManagerCompat.from(context).cancel("$uid/${item.id}", 0)
        prefs(context).edit().remove("delivered:${item.id}").apply()
    }

    fun clearOtherAccount(context: Context, uid: String?) {
        val oldUid = prefs(context).getString("uid", null) ?: return
        if (uid == oldUid) return
        cached(context, oldUid).forEach { cancel(context, oldUid, it) }
        prefs(context).edit().clear().commit()
    }

    fun replace(context: Context, uid: String, items: List<Schedule>) {
        clearOtherAccount(context, uid)
        val old = cached(context, uid)
        old.filter { previous -> items.none { it.id == previous.id && it.revision == previous.revision } }
            .forEach { cancel(context, uid, it) }
        val array = JSONArray()
        items.forEach { item -> array.put(JSONObject().apply {
            put("id", item.id); put("title", item.title); put("note", item.note)
            put("startsAt", item.startsAt); put("reminderMinutes", item.reminderMinutes)
            put("revision", item.revision); put("eventId", item.eventId)
        }) }
        if (prefs(context).edit().putString("uid", uid).putString("items", array.toString()).commit()) restore(context)
    }

    fun restore(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val manager = context.getSystemService(AlarmManager::class.java)
        cached(context, uid).forEach { item ->
            val at = item.alarmAt
            if (at != null && at > System.currentTimeMillis() && prefs(context).getString("delivered:${item.id}", null) != item.revision) {
                val pending = alarmIntent(context, uid, item)
                try {
                    if (manager.canScheduleExactAlarms()) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                    else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                } catch (_: SecurityException) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                }
            }
        }
    }

    @Synchronized
    fun deliver(context: Context, uid: String, id: String, revision: String) {
        if (FirebaseAuth.getInstance().currentUser?.uid != uid || !notificationsEnabled(context)) return
        val item = cached(context, uid).firstOrNull { it.id == id && it.revision == revision } ?: return
        if (!item.canDeliver(System.currentTimeMillis()) || prefs(context).getString("delivered:$id", null) == revision) return
        val open = HomeActivity.homeIntent(context).putExtra("openScheduleId", id).putExtra("scheduleUid", uid)
        val pending = PendingIntent.getActivity(context, 0, open.setData(Uri.Builder().scheme("maru").authority("schedule").appendPath(uid).appendPath(id).build()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentTitle(item.title)
            .setContentText(ScheduleUi.timeLabel(item.startsAt) + if (item.note.isBlank()) "" else " · ${item.note}")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify("$uid/$id", 0, notification)
            prefs(context).edit().putString("delivered:$id", revision).commit()
        } catch (_: SecurityException) { /* 사용자가 권한을 철회한 경우 다음 예약부터 다시 확인한다. */ }
    }
}

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ScheduleAlarms.deliver(context, intent.getStringExtra("uid").orEmpty(),
            intent.getStringExtra("id").orEmpty(), intent.getStringExtra("revision").orEmpty())
    }
}

class ScheduleRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
            ScheduleAlarms.restore(context)
        }
    }
}
