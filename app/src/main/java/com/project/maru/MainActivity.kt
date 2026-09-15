package com.project.maru

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loginButton: Button
    private lateinit var loginStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        auth = FirebaseAuth.getInstance()
        loginButton = findViewById(R.id.btnGoogleLogin)
        loginStatus = findViewById(R.id.tvLoginStatus)

        auth.currentUser?.let { user ->
            loginStatus.text = "로그인 중: ${user.email ?: user.displayName}"
        }

        loginButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        loginButton.isEnabled = false
        loginStatus.text = "구글 계정을 선택해 주세요"

        lifecycleScope.launch {
            try {
                val googleOption = Builder(
                    getString(R.string.default_web_client_id)
                ).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build()

                val result = CredentialManager.create(this@MainActivity)
                    .getCredential(
                        context = this@MainActivity,
                        request = request
                    )

                val credential = result.credential

                if (credential !is CustomCredential ||
                    credential.type !=
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    loginStatus.text = "구글 로그인 정보를 받지 못했습니다"
                    loginButton.isEnabled = true
                    return@launch
                }

                val googleCredential =
                    GoogleIdTokenCredential.createFrom(credential.data)

                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleCredential.idToken,
                    null
                )

                loginStatus.text = "로그인 중..."

                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this@MainActivity) { task ->
                        loginButton.isEnabled = true

                        if (task.isSuccessful) {
                            val user = task.result?.user
                            loginStatus.text =
                                "로그인 성공!\n${user?.email ?: user?.displayName}"
                        } else {
                            loginStatus.text = "로그인 실패: ${task.exception?.message}"
                            Log.e("GoogleLogin", "Firebase 로그인 실패", task.exception)
                        }
                    }
            } catch (e: GetCredentialCancellationException) {
                loginStatus.text = "로그인을 취소했습니다"
                loginButton.isEnabled = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loginStatus.text = "로그인 오류: ${e.message}"
                loginButton.isEnabled = true
                Log.e("GoogleLogin", "구글 로그인 실패", e)
            }
        }
    }
}