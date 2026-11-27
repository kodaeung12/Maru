package com.project.maru

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {
    private var onboardingShown = false
    private var navigating = false
    private var checkingSession = false

    private val preferences by lazy {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

    }

    override fun onStart() {
        super.onStart()
        if (!onboardingShown && !navigating) checkInitialDestination()
    }

    override fun onStop() {
        checkingSession = false
        super.onStop()
    }

    private fun checkInitialDestination() {
        if (checkingSession || navigating) return
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            if (preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)) showLoginScreen()
            else showOnboarding()
            return
        }
        checkingSession = true
        findViewById<View>(R.id.layoutSessionLoading).visibility = View.VISIBLE
        user.reload().addOnCompleteListener(this) { task ->
            checkingSession = false
            if (navigating || isFinishing || isDestroyed) return@addOnCompleteListener
            val refreshedUser = auth.currentUser
            if (task.isSuccessful && refreshedUser?.uid == user.uid && refreshedUser.isEmailVerified) {
                navigating = true
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                auth.signOut()
                showLoginScreen()
                if (!task.isSuccessful) android.widget.Toast.makeText(this,"로그인 상태를 확인하지 못했어요. 다시 로그인해 주세요.",android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showOnboarding() {
        onboardingShown = true
        setContentView(R.layout.activity_main)
        applySystemBarInsets(R.id.main)

        val viewPager = findViewById<ViewPager2>(R.id.onboardingViewPager)
        val nextButton = findViewById<Button>(R.id.btnNext)
        val skipText = findViewById<TextView>(R.id.tvSkip)
        val indicators = listOf(
            findViewById<View>(R.id.indicator1),
            findViewById<View>(R.id.indicator2),
            findViewById<View>(R.id.indicator3),
            findViewById<View>(R.id.indicator4)
        )

        viewPager.adapter = OnboardingPagerAdapter(this)

        fun updatePageControls(position: Int) {
            indicators.forEachIndexed { index, indicator ->
                indicator.setBackgroundResource(
                    if (index == position) R.drawable.bg_indicator_active
                    else R.drawable.bg_indicator_inactive
                )
            }
            nextButton.setText(
                if (position == (viewPager.adapter?.itemCount ?: 1) - 1) R.string.start
                else R.string.next
            )
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageControls(position)
            }
        })

        nextButton.setOnClickListener {
            if (viewPager.currentItem < (viewPager.adapter?.itemCount ?: 1) - 1) {
                viewPager.currentItem += 1
            } else {
                completeOnboarding()
            }
        }
        skipText.setOnClickListener { completeOnboarding() }

        updatePageControls(viewPager.currentItem)
    }

    private fun completeOnboarding() {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
        showLoginScreen()
    }

    private fun showLoginScreen() {
        if (navigating) return
        navigating = true
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun applySystemBarInsets(viewId: Int) {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(viewId)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "maru_preferences"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
