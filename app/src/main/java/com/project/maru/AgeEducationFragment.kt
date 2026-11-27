package com.project.maru

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class AgeEducationFragment : Fragment(R.layout.fragment_age_edu) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        view.findViewById<TextView>(R.id.tvTopTitle).text = getString(R.string.title_age_edu)
        view.findViewById<View>(R.id.btnBack).setOnClickListener { navController.navigateUp() }

        val title = view.findViewById<TextView>(R.id.tvAgeEducationTitle)
        val description = view.findViewById<TextView>(R.id.tvAgeEducationDescription)
        val tabs = mapOf(
            "6" to (R.id.tabAge6 to R.string.age6_text),
            "7" to (R.id.tabAge7 to R.string.age7_text),
            "8" to (R.id.tabAge8 to R.string.age8_text),
            "parent" to (R.id.tabAgeParent to R.string.guide_text)
        )

        fun selectCategory(category: String) {
            val normalized = category.takeIf { tabs.containsKey(it) } ?: "6"
            tabs.forEach { (key, pair) ->
                val (tabId, _) = pair
                val tab = view.findViewById<TextView>(tabId)
                val selected = key == normalized
                tab.setBackgroundResource(if (selected) R.drawable.community_chip_on else R.drawable.community_chip_off)
                tab.setTextColor(ContextCompat.getColor(
                    requireContext(),
                    if (selected) android.R.color.white else R.color.maru_pink
                ))
            }

            if (normalized == "parent") {
                title.text = getString(R.string.age_education_parent_title)
            } else {
                title.text = getString(R.string.age_education_title, normalized.toInt())
            }
            description.setText(tabs.getValue(normalized).second)
            view.findViewById<View>(R.id.tabAge6).contentDescription = getString(R.string.age_education_tab_description, 6)
            view.findViewById<View>(R.id.tabAge7).contentDescription = getString(R.string.age_education_tab_description, 7)
            view.findViewById<View>(R.id.tabAge8).contentDescription = getString(R.string.age_education_tab_description, 8)
        }

        tabs.forEach { (category, pair) ->
            view.findViewById<View>(pair.first).setOnClickListener { selectCategory(category) }
        }
        selectCategory(arguments?.getString("category").orEmpty())
    }
}
