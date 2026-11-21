package com.project.maru.experience

import android.app.AlertDialog
import android.widget.Button
import com.project.maru.R

internal object ExperienceCategoryPicker {
    fun bind(button: Button, adapter: ExperienceAdapter) {
        val context = button.context
        val options = listOf(
            R.string.experience_list_category_all to "전체",
            R.string.experience_list_category_experience to "체험",
            R.string.experience_list_category_education to "교육",
            R.string.experience_list_category_culture to "문화/예술",
            R.string.experience_list_category_nature to "자연"
        )
        val labels = options.map { (label, _) -> context.getString(label) }.toTypedArray()
        var selected = 0

        button.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(R.string.experience_list_category_title)
                .setSingleChoiceItems(labels, selected) { dialog, index ->
                    selected = index
                    button.text = context.getString(R.string.experience_period_button, labels[index])
                    adapter.filterCategory(options[index].second)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
