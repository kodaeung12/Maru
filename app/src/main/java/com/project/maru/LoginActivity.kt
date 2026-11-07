package com.project.maru

import android.app.DatePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.databinding.ActivityLoginBinding
import java.util.Calendar
import java.util.Locale
import java.util.UUID

// 수업 예제의 ViewBinding, 화면 전환, Firebase 비동기 인증을 Maru에 적용.
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var busy = false
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val pendingChildren = mutableListOf<ChildDraft>()

    private data class ChildDraft(
        val id: String,
        val name: String,
        val birthDate: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        binding.textSignupTitle.text = highlightLast(getString(R.string.signup_title), "마루")
        binding.textChildSignupTitle.text = highlightLast(getString(R.string.signup_title), "마루")
        binding.buttonGoSignup.setOnClickListener { showSignupScreen() }
        binding.buttonBackLogin.setOnClickListener { showSignupChoiceScreen() }
        binding.buttonSignup.setOnClickListener { showChildSignupScreen() }
        binding.buttonSignupComplete.setOnClickListener { createEmailAccount() }
        binding.buttonChildAdd.setOnClickListener { showAddChildDialog() }
        binding.buttonChildBack.setOnClickListener { showSignupScreen() }
        binding.buttonLogin.setOnClickListener { loginWithEmail() }
        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            binding.editLoginPassword.text.clear()
            showAuthChoiceScreen()
            toast(getString(R.string.logged_out))
        }
        // 기존 Google 버튼의 준비 중 상태를 유지한다.
        binding.btnGoogleLogin.setOnClickListener { toast(getString(R.string.google_login_pending)) }
        showInitialScreen()
    }

    private fun showInitialScreen() {
        val user = auth.currentUser
        if (user == null) {
            showAuthChoiceScreen()
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
                showAuthChoiceScreen()
                if (!task.isSuccessful) showError(task.exception)
            }
        }
    }

    private fun createEmailAccount() {
        if (busy) return
        val email = binding.editSignupEmail.text.toString().trim()
        val password = binding.editSignupPassword.text.toString()
        val confirm = binding.editSignupPasswordConfirm.text.toString()
        val username = binding.editSignupName.text.toString().trim()
        if (!validateEmail(email)) return
        if (password.length < 6) { toast(getString(R.string.password_too_short)); return }
        if (password != confirm) { toast(getString(R.string.password_mismatch)); return }
        if (username.isBlank()) { toast(getString(R.string.signup_username_required)); return }
        setButtonsEnabled(false)
        val existingUser = auth.currentUser
        if (existingUser != null && existingUser.email == email) {
            completeRegistration(existingUser, email, username)
            return
        }

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
            completeRegistration(user, email, username)
        }
    }

    private fun completeRegistration(user: FirebaseUser, email: String, username: String) {
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(username).build())
            .addOnCompleteListener(this) {
                saveSignupData(user, email, username)
            }
    }

    private fun saveSignupData(user: FirebaseUser, email: String, username: String) {
        val userRef = firestore.collection("users").document(user.uid)
        val batch = firestore.batch()
        batch.set(
            userRef,
            hashMapOf<String, Any>(
                "uid" to user.uid,
                "email" to email,
                "displayName" to username,
                "createdAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
        pendingChildren.forEach { child ->
            val childRef = userRef.collection("children").document(child.id)
            batch.set(
                childRef,
                hashMapOf<String, Any>(
                    "name" to child.name,
                    "birthDate" to child.birthDate,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().addOnCompleteListener(this) { saveTask ->
            if (!saveTask.isSuccessful) {
                setButtonsEnabled(true)
                Log.e("MaruFirestore", "Failed to save signup data", saveTask.exception)
                toast(getString(R.string.firestore_save_failed))
                return@addOnCompleteListener
            }
            sendVerificationEmail(user, email)
        }
    }

    private fun sendVerificationEmail(user: FirebaseUser, email: String) {
        user.sendEmailVerification().addOnCompleteListener(this) { verification ->
            auth.signOut()
            binding.editLoginEmail.setText(email)
            binding.editLoginPassword.text.clear()
            binding.editSignupPassword.text.clear()
            binding.editSignupPasswordConfirm.text.clear()
            binding.editSignupName.text.clear()
            pendingChildren.clear()
            binding.layoutPendingChildren.removeAllViews()
            showLoginScreen()
            if (verification.isSuccessful) toast(getString(R.string.verification_sent))
            else {
                toast(getString(R.string.verification_send_failed))
                showError(verification.exception)
            }
        }
    }

    private fun showAddChildDialog() {
        val padding = (20 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, 0)
        }
        val nameInput = EditText(this).apply {
            hint = getString(R.string.child_name_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            maxLines = 1
        }
        val birthDateButton = Button(this).apply {
            text = getString(R.string.child_birthdate)
            isAllCaps = false
        }
        var selectedBirthDate: String? = null
        birthDateButton.setOnClickListener {
            val today = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedBirthDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                    birthDateButton.text = selectedBirthDate
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        content.addView(nameInput)
        content.addView(birthDateButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_child)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener saveClick@{
                val name = nameInput.text.toString().trim()
                if (name.isBlank()) {
                    nameInput.error = getString(R.string.child_name_required)
                    return@saveClick
                }
                val birthDate = selectedBirthDate
                if (birthDate == null) {
                    toast(getString(R.string.child_birthdate_required))
                    return@saveClick
                }
                val child = ChildDraft(UUID.randomUUID().toString(), name, birthDate)
                pendingChildren.add(child)
                addChildSummary(child)
                dialog.dismiss()
                toast(getString(R.string.child_added))
            }
        }
        dialog.show()
    }

    private fun addChildSummary(child: ChildDraft) {
        val summary = TextView(this).apply {
            text = "${child.name} · ${child.birthDate}"
            textSize = 16f
            setTextColor(Color.rgb(114, 114, 114))
            val verticalPadding = (10 * resources.displayMetrics.density).toInt()
            setPadding(0, verticalPadding, 0, verticalPadding)
        }
        val position = binding.layoutPendingChildren.childCount
        binding.layoutPendingChildren.addView(
            summary,
            position,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
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

    private fun showLoginScreen() {
        setContentView(binding.root)
        applyFormInsets()
        showScreen(binding.layoutLogin)
    }

    private fun showSignupScreen() {
        setContentView(binding.root)
        applyFormInsets()
        showScreen(binding.layoutSignup)
    }

    private fun showChildSignupScreen() {
        val email = binding.editSignupEmail.text.toString().trim()
        val password = binding.editSignupPassword.text.toString()
        val confirm = binding.editSignupPasswordConfirm.text.toString()
        val username = binding.editSignupName.text.toString().trim()
        if (!validateEmail(email)) return
        if (password.length < 6) { toast(getString(R.string.password_too_short)); return }
        if (password != confirm) { toast(getString(R.string.password_mismatch)); return }
        if (username.isBlank()) { toast(getString(R.string.signup_username_required)); return }
        showScreen(binding.layoutChildSignup)
    }

    private fun showAuthChoiceScreen() {
        setContentView(R.layout.activity_auth_choice)
        applyOverlayInsets(R.id.authChoiceRoot, R.id.authChoicePanel)
        findViewById<android.widget.TextView>(R.id.textAuthChoiceTitle).text =
            highlightLast(getString(R.string.auth_choice_title), "마루")
        findViewById<View>(R.id.buttonAuthLogin).setOnClickListener { showLoginScreen() }
        findViewById<View>(R.id.buttonAuthSignup).setOnClickListener { showSignupChoiceScreen() }
    }

    private fun showSignupChoiceScreen() {
        setContentView(R.layout.activity_signup_choice)
        applyOverlayInsets(R.id.signupChoiceRoot, R.id.signupChoicePanel)
        findViewById<android.widget.TextView>(R.id.textSignupChoiceTitle).text =
            highlightLast(getString(R.string.signup_choice_title), "마루")
        findViewById<View>(R.id.buttonSignupEmail).setOnClickListener { showSignupScreen() }
        findViewById<View>(R.id.buttonSignupEmailAlternative).setOnClickListener { showSignupScreen() }
        findViewById<View>(R.id.buttonSignupChoiceBack).setOnClickListener { showAuthChoiceScreen() }
    }

    private fun applyFormInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.loginRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun applyOverlayInsets(rootId: Int, panelId: Int) {
        val panel = findViewById<View>(panelId)
        val horizontal = panel.paddingLeft
        val top = panel.paddingTop
        val bottom = panel.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(rootId)) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            panel.setPadding(horizontal, top, horizontal, bottom + navBottom)
            insets
        }
    }

    private fun highlightLast(text: String, phrase: String): SpannableString {
        return SpannableString(text).apply {
            val start = lastIndexOf(phrase)
            if (start >= 0) setSpan(
                ForegroundColorSpan(Color.rgb(255, 148, 148)),
                start,
                start + phrase.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun showHomeScreen(user: FirebaseUser) {
        setContentView(binding.root)
        applyFormInsets()
        binding.textUserEmail.text = user.email
        showScreen(binding.layoutHome)
    }

    private fun showScreen(screen: View) {
        binding.layoutLogin.visibility = if (screen === binding.layoutLogin) View.VISIBLE else View.GONE
        binding.layoutSignup.visibility = if (screen === binding.layoutSignup) View.VISIBLE else View.GONE
        binding.layoutChildSignup.visibility = if (screen === binding.layoutChildSignup) View.VISIBLE else View.GONE
        binding.layoutHome.visibility = if (screen === binding.layoutHome) View.VISIBLE else View.GONE
        setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        busy = !enabled
        listOf(binding.buttonLogin, binding.btnGoogleLogin, binding.buttonGoSignup,
            binding.buttonSignup, binding.buttonBackLogin, binding.buttonSignupComplete,
            binding.buttonChildAdd, binding.buttonChildBack, binding.buttonLogout,
            binding.editLoginEmail, binding.editLoginPassword, binding.editSignupEmail,
            binding.editSignupPassword, binding.editSignupPasswordConfirm, binding.editSignupName).forEach { it.isEnabled = enabled }
        binding.progressAuth.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun showError(error: Exception?) {
        Log.e("MaruAuth", "Authentication failed", error)
        toast(error?.localizedMessage ?: getString(R.string.auth_error))
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
