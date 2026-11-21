package com.project.maru.experience

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.maru.R

class SimilarEventsActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CANDIDATES = "com.project.maru.RECOMMENDATION_CANDIDATES"

        internal fun createIntent(context: Context, candidates: List<Experience>?): Intent =
            Intent(context, SimilarEventsActivity::class.java).apply {
                if (candidates != null) {
                    putParcelableArrayListExtra(EXTRA_CANDIDATES, candidates.mapTo(ArrayList()) { event ->
                        Bundle().apply {
                            putString("title", event.title)
                            putString("location", event.location)
                            putString("date", event.date)
                            putString("category", event.category)
                            putString("imageUrl", event.imageUrl)
                            putString("targetAge", event.targetAge)
                            putString("region", event.region)
                        }
                    })
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_similar_events)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val recyclerSimilarEvents =
            findViewById<RecyclerView>(R.id.recyclerSimilarEvents)

        val textEmptyRecommendation =
            findViewById<TextView>(R.id.textEmptyRecommendation)

        val history = RecentExperienceStore.readRecent(this)
        val candidates = IntentCompat.getParcelableArrayListExtra(intent, EXTRA_CANDIDATES, Bundle::class.java)
            ?.map { event ->
                Experience(
                    title = event.getString("title").orEmpty(),
                    location = event.getString("location").orEmpty(),
                    date = event.getString("date").orEmpty(),
                    category = event.getString("category").orEmpty(),
                    imageUrl = event.getString("imageUrl"),
                    targetAge = event.getString("targetAge").orEmpty(),
                    region = event.getString("region").orEmpty()
                )
            }
        val recommendations = SimilarExperienceRecommender.recommend(history, candidates.orEmpty())
        recyclerSimilarEvents.layoutManager = GridLayoutManager(this, 2)
        recyclerSimilarEvents.adapter = ExperienceAdapter(recommendations)
        recyclerSimilarEvents.isVisible = recommendations.isNotEmpty()

        textEmptyRecommendation.isVisible = recommendations.isEmpty()
        if (recommendations.isEmpty()) {
            textEmptyRecommendation.setText(when {
                history.isEmpty() -> R.string.similar_events_no_history
                candidates == null -> R.string.similar_events_candidates_unavailable
                else -> R.string.similar_events_no_matches
            })
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
