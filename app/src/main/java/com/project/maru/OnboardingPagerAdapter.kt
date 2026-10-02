package com.project.maru

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

private data class OnboardingPage(
    val imageRes: Int,
    val titleRes: Int,
    val descriptionRes: Int
)

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val pages = listOf(
        OnboardingPage(R.drawable.test1, R.string.onboarding_title_1, R.string.onboarding_description_1),
        OnboardingPage(R.drawable.test1, R.string.onboarding_title_2, R.string.onboarding_description_2),
        OnboardingPage(R.drawable.test1, R.string.onboarding_title_3, R.string.onboarding_description_3),
        OnboardingPage(R.drawable.test1, R.string.onboarding_title_4, R.string.onboarding_description_4)
    )

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        val page = pages[position]
        return OnboardingFragment.newInstance(page.imageRes, page.titleRes, page.descriptionRes)
    }
}
