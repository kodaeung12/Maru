package com.project.maru.diary

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.project.maru.MyApplication.Companion.auth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// 일기 화면에서 공통으로 사용하는 로그인 확인과 여백 처리.
abstract class DiaryActivity : AppCompatActivity() {
    protected var uid = ""
    protected val dateFormat = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
    private val authListener = FirebaseAuth.AuthStateListener {
        if (it.currentUser?.uid != uid) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uid = auth.currentUser?.uid.orEmpty()
        if (uid.isEmpty()) {
            toast("로그인 후 일기를 이용해 주세요.")
            finish()
        }
    }

    protected fun inset(root: android.view.View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    protected fun dateExtra(): LocalDate {
        return try {
            LocalDate.parse(intent.getStringExtra("date"))
        } catch (_: Exception) {
            DiaryDates.today()
        }
    }

    protected fun active(): Boolean {
        return !isFinishing && !isDestroyed && auth.currentUser?.uid == uid
    }

    protected fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authListener)
    }

    override fun onStop() {
        auth.removeAuthStateListener(authListener)
        super.onStop()
    }
}
