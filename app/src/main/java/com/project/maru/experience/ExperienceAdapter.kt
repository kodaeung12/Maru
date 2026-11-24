package com.project.maru.experience

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.content.Intent
import com.project.maru.DetailActivity
import com.project.maru.R

internal class ExperienceAdapter(
    experienceList: List<Experience>,
    private val favorites: FavoriteStore
) : RecyclerView.Adapter<ExperienceAdapter.ExperienceViewHolder>() {

    private val originalList = experienceList.toMutableList()

    private val filteredList = experienceList.toMutableList()

    private var currentQuery = ""

    private var selectedCategory = "전체"

    private var selectedRegion = "전체 지역"

    private var selectedPeriod: ExperiencePeriod? = null

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

    internal fun filterPeriod(period: ExperiencePeriod?) {
        selectedPeriod = period
        updateFilteredList()
    }

    private fun updateFilteredList() {
        filteredList.clear()

        filteredList.addAll(
            originalList.filter { experience ->
                val matchesQuery =
                    experience.matchesSearch(currentQuery)

                val matchesCategory =
                    selectedCategory == "전체" ||
                            experience.category == selectedCategory

                val matchesRegion =
                    selectedRegion == "전체 지역" ||
                            experience.region.trim().equals(selectedRegion, ignoreCase = true)

                matchesQuery && matchesCategory && matchesRegion &&
                    (selectedPeriod?.includes(experience) ?: true)
            }
        )

        notifyDataSetChanged()
    }

    class ExperienceViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val favoriteButton: ImageButton = itemView.findViewById(R.id.buttonFavoriteEvent)

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
        holder.favoriteButton.isSelected = favorites.isSaved(experience)
        holder.favoriteButton.isEnabled = !favorites.isPending(experience)
        holder.favoriteButton.alpha = if (favorites.ready && !favorites.isPending(experience)) 1f else 0.5f
        holder.favoriteButton.contentDescription = holder.itemView.context.getString(
            if (favorites.isSaved(experience)) R.string.favorite_remove else R.string.favorite_add
        )
        holder.favoriteButton.setOnClickListener { favorites.toggle(experience) }

        holder.itemView.setOnClickListener {
            RecentExperienceStore.recordView(holder.itemView.context, experience)
            val intent = Intent(holder.itemView.context, DetailActivity::class.java).apply {
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

            holder.itemView.context.startActivity(intent)
        }

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
