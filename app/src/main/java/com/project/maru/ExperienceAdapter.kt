package com.project.maru

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ExperienceAdapter(
    experienceList: List<Experience>
) : RecyclerView.Adapter<ExperienceAdapter.ExperienceViewHolder>() {

    private val originalList = experienceList.toMutableList()

    private val filteredList = experienceList.toMutableList()

    private var currentQuery = ""

    private var selectedCategory = "전체"

    private var selectedRegion = "전체 지역"

    fun updateData(newList: List<Experience>) {
        originalList.clear()
        originalList.addAll(newList)

        updateFilteredList()
    }

    fun filterList(query: String) {
        currentQuery = query
        updateFilteredList()
    }

    fun filterCategory(category: String) {
        selectedCategory = category
        updateFilteredList()
    }

    fun filterRegion(region: String) {
        selectedRegion = region
        updateFilteredList()
    }

    private fun updateFilteredList() {
        filteredList.clear()

        filteredList.addAll(
            originalList.filter { experience ->
                val matchesQuery =
                    experience.title.contains(currentQuery, ignoreCase = true)

                val matchesCategory =
                    selectedCategory == "전체" ||
                            experience.category == selectedCategory

                val matchesRegion =
                    selectedRegion == "전체 지역" ||
                            experience.region.trim().equals(selectedRegion, ignoreCase = true)

                matchesQuery && matchesCategory && matchesRegion
            }
        )

        notifyDataSetChanged()
    }

    class ExperienceViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView =
            itemView.findViewById(R.id.imageExperience)

        val titleText: TextView =
            itemView.findViewById(R.id.textTitle)

        val locationText: TextView =
            itemView.findViewById(R.id.textLocation)

        val dateText: TextView =
            itemView.findViewById(R.id.textDate)

        val categoryText: TextView =
            itemView.findViewById(R.id.textCategory)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExperienceViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_experience, parent, false)

        return ExperienceViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ExperienceViewHolder,
        position: Int
    ) {
        val experience = filteredList[position]

        holder.titleText.text = experience.title
        holder.locationText.text = experience.location
        holder.dateText.text = experience.date
        holder.categoryText.text = experience.category

        if (!experience.imageUrl.isNullOrBlank()) {

            Glide.with(holder.itemView)
                .load(experience.imageUrl)
                .centerCrop()
                .into(holder.imageView)

        } else {

            Glide.with(holder.itemView).clear(holder.imageView)

            if (experience.imageResId != null) {
                holder.imageView.setImageResource(experience.imageResId)
            } else {
                holder.imageView.setImageDrawable(null)
            }
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }
}
