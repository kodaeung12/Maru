package com.project.maru.experience

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.widget.Button
import com.project.maru.R
import java.time.LocalDate
import java.time.YearMonth

internal object ExperiencePeriodPicker {
    fun bind(button: Button, adapter: ExperienceAdapter) {
        val context = button.context
        val options = listOf(
            R.string.experience_period_all,
            R.string.experience_period_this_week,
            R.string.experience_period_this_month,
            R.string.experience_period_next_month,
            R.string.experience_period_custom
        )
        var customPeriod: ExperiencePeriod? = null

        fun applyPeriod(label: Int, period: ExperiencePeriod?) {
            button.text = context.getString(R.string.experience_period_button, context.getString(label))
            adapter.filterPeriod(period)
        }

        button.setOnClickListener {
            val labels = options.map { context.getString(it) }.toTypedArray()
            AlertDialog.Builder(context)
                .setTitle(R.string.experience_period_title)
                .setItems(labels) { _, index ->
                    val today = ExperiencePeriod.todayInSeoul()
                    val month = YearMonth.from(today)
                    when (val option = options[index]) {
                        R.string.experience_period_all -> applyPeriod(option, null)
                        R.string.experience_period_this_week -> applyPeriod(option, ExperiencePeriod.weekOf(today))
                        R.string.experience_period_this_month -> applyPeriod(option, ExperiencePeriod.monthOf(month))
                        R.string.experience_period_next_month -> applyPeriod(option, ExperiencePeriod.monthOf(month.plusMonths(1)))
                        R.string.experience_period_custom -> {
                            chooseDate(context, R.string.experience_period_start, customPeriod?.start ?: today) { start ->
                                val initialEnd = customPeriod?.end?.coerceAtLeast(start) ?: start
                                chooseDate(context, R.string.experience_period_end, initialEnd) { end ->
                                    if (start.isAfter(end)) {
                                        AlertDialog.Builder(context)
                                            .setMessage(R.string.experience_period_invalid_range)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .show()
                                    } else {
                                        val period = ExperiencePeriod(start, end)
                                        customPeriod = period
                                        applyPeriod(R.string.experience_period_custom_label, period)
                                    }
                                }
                            }
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun chooseDate(context: Context, title: Int, initial: LocalDate, onSelected: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day -> onSelected(LocalDate.of(year, month + 1, day)) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).apply {
            setTitle(title)
            show()
        }
    }
}
