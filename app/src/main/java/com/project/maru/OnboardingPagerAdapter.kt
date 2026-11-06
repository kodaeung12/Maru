package com.project.maru

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

private data class OnboardingPage(
    val imageRes: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val accent: String
)

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val pages = listOf(
        OnboardingPage(R.drawable.boarding1, R.string.onboarding_title_1, R.string.onboarding_description_1, "마루"),
        OnboardingPage(R.drawable.boarding2, R.string.onboarding_title_2, R.string.onboarding_description_2, "맞춤 교육"),
        OnboardingPage(R.drawable.boarding4, R.string.onboarding_title_3, R.string.onboarding_description_3, "실전 가이드"),
        OnboardingPage(R.drawable.boarding3, R.string.onboarding_title_4, R.string.onboarding_description_4, "안전정보")
    )

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        val page = pages[position]
        return OnboardingFragment.newInstance(page.imageRes, page.titleRes, page.descriptionRes, page.accent)
    }
}
