package com.project.maru.experience.recommendation

import com.project.maru.experience.Experience
import com.project.maru.experience.openExperienceDetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.maru.databinding.ItemDetailRecommendationBinding

internal class DetailRecommendationAdapter :
    ListAdapter<Experience, DetailRecommendationAdapter.Holder>(object : DiffUtil.ItemCallback<Experience>() {
        override fun areItemsTheSame(oldItem: Experience, newItem: Experience): Boolean =
            RecentExperienceStore.identityOf(oldItem) == RecentExperienceStore.identityOf(newItem)

        override fun areContentsTheSame(oldItem: Experience, newItem: Experience): Boolean =
            oldItem == newItem
    }) {
    class Holder(val binding: ItemDetailRecommendationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemDetailRecommendationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val event = getItem(position)
        with(holder.binding) {
            recommendationTitle.text = event.title
            recommendationRegion.text = event.region.ifBlank { event.location }
            recommendationDate.text = event.date
            Glide.with(recommendationImage).load(event.imageUrl).centerCrop().into(recommendationImage)
            root.setOnClickListener { openExperienceDetail(root.context, event) }
        }
    }

    override fun onViewRecycled(holder: Holder) {
        Glide.with(holder.binding.recommendationImage).clear(holder.binding.recommendationImage)
        super.onViewRecycled(holder)
    }
}
