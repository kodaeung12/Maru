package com.project.maru

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseUser
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.databinding.ActivityLoginBinding

// 수업 예제의 ViewBinding, 화면 전환, Firebase 비동기 인증을 Maru에 적용.
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.loginRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.buttonGoSignup.setOnClickListener { showSignupScreen() }
        binding.buttonBackLogin.setOnClickListener { showLoginScreen() }
        binding.buttonSignup.setOnClickListener { createEmailAccount() }
        binding.buttonLogin.setOnClickListener { loginWithEmail() }
        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            binding.editLoginPassword.text.clear()
            showLoginScreen()
            toast(getString(R.string.logged_out))
        }
        // 기존 Google 버튼의 준비 중 상태를 유지한다.
        binding.btnGoogleLogin.setOnClickListener { toast(getString(R.string.google_login_pending)) }
        showInitialScreen()
    }

    private fun showInitialScreen() {
        val user = auth.currentUser
        if (user == null) {
            showLoginScreen()
            return
        }
        setButtonsEnabled(false)
        // 저장된 인증 상태 대신 서버에서 이메일 인증 상태를 새로 확인한다.
        user.reload().addOnCompleteListener(this) { task ->
            setButtonsEnabled(true)
            val refreshedUser = auth.currentUser
            if (task.isSuccessful && refreshedUser != null && refreshedUser.isEmailVerified) {
                showHomeScreen(refreshedUser)
            } else {
                auth.signOut()
                showLoginScreen()
                if (!task.isSuccessful) showError(task.exception)
            }
        }
    }

    private fun createEmailAccount() {
        if (busy) return
        val email = binding.editSignupEmail.text.toString().trim()
        val password = binding.editSignupPassword.text.toString()
        val confirm = binding.editSignupPasswordConfirm.text.toString()
        if (!validateEmail(email)) return
        if (password.length < 6) { toast(getString(R.string.password_too_short)); return }
        if (password != confirm) { toast(getString(R.string.password_mismatch)); return }
        setButtonsEnabled(false)
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (!task.isSuccessful) {
                setButtonsEnabled(true)
                showError(task.exception)
                return@addOnCompleteListener
            }
            val user = auth.currentUser
            if (user == null) {
                setButtonsEnabled(true)
                toast(getString(R.string.user_unavailable))
                return@addOnCompleteListener
            }
            user.sendEmailVerification().addOnCompleteListener(this) { verification ->
                auth.signOut()
                binding.editLoginEmail.setText(email)
                binding.editLoginPassword.text.clear()
                binding.editSignupPassword.text.clear()
                binding.editSignupPasswordConfirm.text.clear()
                showLoginScreen()
                if (verification.isSuccessful) toast(getString(R.string.verification_sent))
                else {
                    toast(getString(R.string.verification_send_failed))
                    showError(verification.exception)
                }
            }
        }
    }

    private fun loginWithEmail() {
        if (busy) return
        val email = binding.editLoginEmail.text.toString().trim()
        val password = binding.editLoginPassword.text.toString()
        if (!validateEmail(email)) return
        if (password.isBlank()) { toast(getString(R.string.password_required)); return }
        setButtonsEnabled(false)
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (!task.isSuccessful) {
                setButtonsEnabled(true)
                showError(task.exception)
                return@addOnCompleteListener
            }
            val user = auth.currentUser
            if (user == null) {
                setButtonsEnabled(true)
                toast(getString(R.string.user_unavailable))
            } else if (!user.isEmailVerified) {
                // 인증 메일 발송 실패/분실 시 다시 로그인하면 재전송할 수 있다.
                user.sendEmailVerification().addOnCompleteListener(this) { verification ->
                    auth.signOut()
                    setButtonsEnabled(true)
                    toast(getString(if (verification.isSuccessful) R.string.verification_resent else R.string.email_not_verified))
                    if (!verification.isSuccessful) showError(verification.exception)
                }
            } else {
                binding.editLoginPassword.text.clear()
                showHomeScreen(user)
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast(getString(R.string.email_invalid))
            return false
        }
        return true
    }

    private fun showLoginScreen() = showScreen(binding.layoutLogin)
    private fun showSignupScreen() = showScreen(binding.layoutSignup)
    private fun showHomeScreen(user: FirebaseUser) {
        binding.textUserEmail.text = user.email
        showScreen(binding.layoutHome)
    }

    private fun showScreen(screen: View) {
        binding.layoutLogin.visibility = if (screen === binding.layoutLogin) View.VISIBLE else View.GONE
        binding.layoutSignup.visibility = if (screen === binding.layoutSignup) View.VISIBLE else View.GONE
        binding.layoutHome.visibility = if (screen === binding.layoutHome) View.VISIBLE else View.GONE
        setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        busy = !enabled
        listOf(binding.buttonLogin, binding.btnGoogleLogin, binding.buttonGoSignup,
            binding.buttonSignup, binding.buttonBackLogin, binding.buttonLogout,
            binding.editLoginEmail, binding.editLoginPassword, binding.editSignupEmail,
            binding.editSignupPassword, binding.editSignupPasswordConfirm).forEach { it.isEnabled = enabled }
        binding.progressAuth.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun showError(error: Exception?) {
        Log.e("MaruAuth", "Authentication failed", error)
        toast(error?.localizedMessage ?: getString(R.string.auth_error))
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}