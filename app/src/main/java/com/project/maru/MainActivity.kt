package com.project.maru

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        /*
        구글 로그인 코드는 로그인 화면을 만들 때 다시 여기에 넣을 예정

        auth = FirebaseAuth.getInstance()
        loginButton = findViewById(R.id.btnGoogleLogin)
        loginStatus = findViewById(R.id.tvLoginStatus)

        auth.currentUser?.let { user ->
            loginStatus.text = "로그인 중: ${user.email ?: user.displayName}"
        }

        loginButton.setOnClickListener {
            signInWithGoogle()
        }
        */
    }

    /*
    private fun signInWithGoogle() {
        기존 구글 로그인 함수 전체를 여기에 보관
    }
    */
}