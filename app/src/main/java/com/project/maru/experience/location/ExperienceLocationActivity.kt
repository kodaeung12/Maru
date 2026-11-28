package com.project.maru.experience.location

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.kakao.vectormap.KakaoMap
import com.project.maru.R

class ExperienceLocationActivity : AppCompatActivity() {
    private var map: KakaoMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val point = intent.experienceMapCoordinates()
        if (point == null) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_experience_location)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.experienceLocationRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<View>(R.id.buttonBackExperienceLocation).setOnClickListener { finish() }
        findViewById<TextView>(R.id.textExperienceLocationTitle).text = intent.getStringExtra(TITLE)
        findViewById<TextView>(R.id.textExperienceLocationPlace).text = intent.getStringExtra(PLACE)

        val host = findViewById<FrameLayout>(R.id.experienceLocationHost)
        val controls = findViewById<View>(R.id.experienceMapZoomControls)
        val error = findViewById<View>(R.id.textExperienceLocationError)
        ExperienceLocationMap(this, host, point, interactive = true, onUnavailable = {
            controls.isVisible = false
            error.isVisible = true
        }) { readyMap ->
            map = readyMap
            controls.isVisible = true
            bindExperienceLocationZoom(readyMap, findViewById(R.id.buttonExperienceZoomIn),
                findViewById(R.id.buttonExperienceZoomOut))
        }.start()
    }

    override fun onDestroy() {
        map?.setOnCameraMoveEndListener(null)
        map = null
        super.onDestroy()
    }

    companion object {
        private const val TITLE = "experience_location_title"
        private const val PLACE = "experience_location_place"

        internal fun intent(context: Context, title: String, place: String, point: EventCoordinates) =
            Intent(context, ExperienceLocationActivity::class.java).apply {
                putExtra(TITLE, title)
                putExtra(PLACE, place)
                putExperienceMapCoordinates(point)
            }
    }
}
