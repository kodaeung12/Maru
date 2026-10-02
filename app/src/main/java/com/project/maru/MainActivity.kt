package com.project.maru

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {

    private val preferences by lazy {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)) {
            showLoginScreen()
        } else {
            showOnboarding()
        }
    }

    private fun showOnboarding() {
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
        setContentView(R.layout.activity_login)
        applySystemBarInsets(R.id.loginRoot)
        findViewById<Button>(R.id.btnGoogleLogin).setOnClickListener {
            Toast.makeText(this, "로그인 연결 전", Toast.LENGTH_SHORT).show()
        }
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
