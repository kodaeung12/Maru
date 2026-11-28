package com.project.maru.experience.recommendation

import com.project.maru.experience.Experience
import com.project.maru.experience.data.ExperienceCandidateCache
import com.project.maru.experience.ExperiencePeriod
import com.project.maru.experience.data.familyExperiences
import com.project.maru.experience.FavoriteStore
import com.project.maru.experience.openExperienceDetail
import com.project.maru.experience.data.SeoulCultureFirstPage

import android.content.res.ColorStateList
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.project.maru.R
import com.project.maru.databinding.ItemHomeExperienceRecommendationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun bindHomeExperienceRecommendations(fragment: Fragment, view: View) {
    val row = view.findViewById<LinearLayout>(R.id.layoutHomeExperienceRecommendations)
    val scroll = view.findViewById<HorizontalScrollView>(R.id.scrollHomeExperienceRecommendations)
    val heading = view.findViewById<TextView>(R.id.textHomeExperienceRecommendationHeading)
    val status = view.findViewById<TextView>(R.id.textHomeExperienceRecommendationStatus)
    val context = view.context.applicationContext
    row.children.filter { it !== status }.forEach { it.isVisible = false }
    val owner = fragment.viewLifecycleOwner
    val favorites = FavoriteStore(owner) { message ->
        Toast.makeText(view.context, message, Toast.LENGTH_SHORT).show()
    }
    val cards = mutableListOf<Pair<Experience, ItemHomeExperienceRecommendationBinding>>()
    favorites.onChanged = {
        cards.forEach { (event, card) -> bindFavorite(card.buttonHomeRecommendedFavorite, event, favorites) }
    }

    fun render(state: HomeExperienceRecommendationState) {
        heading.setText(if (state.personalized) R.string.section_recommend_personalized else R.string.section_recommend)
        status.isVisible = state.events.isEmpty()
        if (state.events == cards.map { it.first }) return

        cards.forEach { (_, card) ->
            Glide.with(fragment).clear(card.imageHomeRecommendedExperience)
            row.removeView(card.root)
        }
        cards.clear()
        state.events.forEach { event ->
            val card = ItemHomeExperienceRecommendationBinding.inflate(fragment.layoutInflater, row, false)
            bindHomeCard(fragment, card, event, favorites)
            row.addView(card.root)
            cards.add(event to card)
        }
        scroll.scrollTo(0, 0)
    }

    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val history = RecentExperienceStore.readRecent(context)
            val lastView = history.firstOrNull()?.viewedAt ?: 0L
            val today = ExperiencePeriod.todayInSeoul()
            val saved = HomeExperienceRecommendationStore.read(context)
            val restored = saved?.restore(history, today)

            if (ExperienceCandidateCache.candidates.value.isEmpty()) {
                status.setText(R.string.home_recommend_loading)
                render(restored ?: HomeExperienceRecommendationState(false, emptyList()))
                if (restored == null || saved.lastView != lastView || restored.events.size != saved.state.events.size) {
                    launch {
                        seedHomeCandidates()
                        if (ExperienceCandidateCache.candidates.value.isEmpty()) {
                            status.setText(R.string.home_recommend_empty)
                        }
                    }
                }
            }

            ExperienceCandidateCache.candidates.collectLatest { candidates ->
                if (candidates.isEmpty()) return@collectLatest
                val state = withContext(Dispatchers.Default) { homeExperienceRecommendations(history, candidates, today) }
                status.setText(R.string.home_recommend_empty)
                render(state)
                HomeExperienceRecommendationStore.write(context, state, lastView)
            }
        }
    }
}


private suspend fun seedHomeCandidates() {
    if (ExperienceCandidateCache.candidates.value.isNotEmpty()) return
    val page = SeoulCultureFirstPage.load() ?: return
    val firstCandidates = withContext(Dispatchers.Default) { page.familyExperiences() }
    if (firstCandidates.isNotEmpty()) ExperienceCandidateCache.seed(firstCandidates)
}

private fun bindHomeCard(
    fragment: Fragment,
    card: ItemHomeExperienceRecommendationBinding,
    event: Experience,
    favorites: FavoriteStore
) {
    val context = card.root.context
    card.textHomeRecommendedTitle.text = event.title
    card.textHomeRecommendedDate.text = event.date
    card.textHomeRecommendedPlace.text = event.location
    card.textHomeRecommendedCategory.text = event.category
    if (event.category == "문화/예술") {
        card.textHomeRecommendedCategory.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.tag_blue_bg))
        card.textHomeRecommendedCategory.setTextColor(ContextCompat.getColor(context, R.color.tag_blue))
    }
    Glide.with(fragment)
        .load(event.imageUrl?.takeIf { it.isNotBlank() })
        .placeholder(R.drawable.img_card_art)
        .error(R.drawable.img_card_art)
        .centerCrop()
        .into(card.imageHomeRecommendedExperience)
    bindFavorite(card.buttonHomeRecommendedFavorite, event, favorites)
    card.buttonHomeRecommendedFavorite.setOnClickListener { favorites.toggle(event) }
    card.root.setOnClickListener {
        fragment.viewLifecycleOwner.lifecycleScope.launch { seedHomeCandidates() }
        openExperienceDetail(context, event)
    }
}

private fun bindFavorite(button: ImageButton, event: Experience, favorites: FavoriteStore) {
    val saved = favorites.isSaved(event)
    val pending = favorites.isPending(event)
    button.isSelected = saved
    button.isEnabled = !pending
    button.alpha = if (favorites.ready && !pending) 1f else 0.5f
    button.contentDescription = button.context.getString(if (saved) R.string.favorite_remove else R.string.favorite_add)
}
