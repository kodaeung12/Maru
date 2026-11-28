package com.project.maru.experience

import com.project.maru.experience.location.putExperienceMapCoordinates
import com.project.maru.experience.recommendation.RecentExperienceStore

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.project.maru.DetailActivity
import com.project.maru.R

internal fun openExperienceDetail(context: Context, experience: Experience) {
    RecentExperienceStore.recordView(context, experience)
    context.startActivity(experienceDetailIntent(context, experience))
}

internal fun experienceDetailIntent(context: Context, experience: Experience): Intent =
    Intent(context, DetailActivity::class.java).apply {
        putExperienceDetailSource(context)
        experience.coordinates?.let { putExperienceMapCoordinates(it) }
        putExtra(DetailActivity.EXTRA_TITLE, experience.title)
        putExtra(DetailActivity.EXTRA_DATE, experience.date)
        putExtra(DetailActivity.EXTRA_PLACE, experience.location)
        putExtra(DetailActivity.EXTRA_REGION, experience.region)
        putExtra(DetailActivity.EXTRA_IMAGE_URL, experience.imageUrl.orEmpty())
        putExtra(DetailActivity.EXTRA_TARGET_AGE, experience.targetAge)
        putExtra(DetailActivity.EXTRA_CATEGORY, experience.category)
        putExtra(DetailActivity.EXTRA_START_DATE, experience.startDate?.toString().orEmpty())
        putExtra(DetailActivity.EXTRA_END_DATE, experience.endDate?.toString().orEmpty())
    }


private const val EXPERIENCE_DETAIL_SOURCE = "com.project.maru.EXPERIENCE_DETAIL_SOURCE"

private val detailNavViews = mapOf(
    R.id.homeFragment to R.id.navHome,
    R.id.experienceFragment to R.id.navEvent,
    R.id.ageEduFragment to R.id.navStudy,
    R.id.communityFragment to R.id.navCommunity,
    R.id.myPageFragment to R.id.navMypage
)

internal fun Intent.putExperienceDetailSource(context: Context) {
    val activity = context.hostActivity()
    val selectedTab = activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)?.selectedItemId
    val inheritedTab = activity?.intent?.getIntExtra(EXPERIENCE_DETAIL_SOURCE, R.id.homeFragment)
    val source = selectedTab?.takeIf { it in detailNavViews }
        ?: inheritedTab?.takeIf { it in detailNavViews }
        ?: R.id.homeFragment
    putExtra(EXPERIENCE_DETAIL_SOURCE, source)
}

private tailrec fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.hostActivity()
    else -> null
}

internal fun bindExperienceDetailNavSelection(activity: DetailActivity) {
    val source = activity.intent.getIntExtra(EXPERIENCE_DETAIL_SOURCE, R.id.homeFragment)
        .takeIf { it in detailNavViews } ?: R.id.homeFragment
    val active = ContextCompat.getColor(activity, R.color.maru_pink)
    val inactive = Color.rgb(102, 102, 102)
    detailNavViews.forEach { (tabId, viewId) ->
        val tab = activity.findViewById<ViewGroup>(viewId)
        tab.isSelected = tabId == source
        val color = if (tab.isSelected) active else inactive
        for (index in 0 until tab.childCount) {
            when (val child = tab.getChildAt(index)) {
                is ImageView -> child.imageTintList = ColorStateList.valueOf(color)
                is TextView -> child.setTextColor(color)
            }
        }
    }
}
