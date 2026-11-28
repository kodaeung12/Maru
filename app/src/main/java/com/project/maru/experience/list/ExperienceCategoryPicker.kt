package com.project.maru.experience.list

import android.app.AlertDialog
import android.widget.Button
import com.project.maru.R
import com.project.maru.experience.Experience

internal enum class ExperienceCategory(val label: Int) {
    ALL(R.string.experience_list_category_all),
    EXPERIENCE(R.string.experience_list_category_experience),
    EDUCATION(R.string.experience_list_category_education),
    CULTURE(R.string.experience_list_category_culture),
    NATURE(R.string.experience_list_category_nature),
    FESTIVAL(R.string.cat_festival);

    fun matches(event: Experience): Boolean = when (this) {
        ALL -> true
        EXPERIENCE -> event.category == "체험"
        EDUCATION -> event.category == "교육"
        CULTURE -> event.sourceCategory == "축제-문화/예술" ||
            (event.category == "문화/예술" && event.sourceCategory != "축제-기타")
        NATURE -> event.category == "자연"
        FESTIVAL -> event.sourceCategory.startsWith("축제-") || event.sourceCategory == "축제"
    }
}

internal object ExperienceCategoryPicker {
    fun bind(button: Button, adapter: ExperienceAdapter, initialKey: String?, favoritesMode: Boolean) {
        val context = button.context
        val options = ExperienceCategory.entries.filter { !favoritesMode || it != ExperienceCategory.FESTIVAL }
        val labels = options.map { context.getString(it.label) }.toTypedArray()
        var selected = 0

        fun select(index: Int) {
            selected = index
            button.text = context.getString(R.string.experience_period_button, labels[index])
            adapter.filterCategory(options[index])
        }

        val initial = options.indexOfFirst { it.name == initialKey }
        if (initial > 0) select(initial)
        button.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(R.string.experience_list_category_title)
                .setSingleChoiceItems(labels, selected) { dialog, index ->
                    select(index)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
