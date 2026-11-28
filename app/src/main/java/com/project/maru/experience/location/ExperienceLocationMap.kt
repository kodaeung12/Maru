package com.project.maru.experience.location

import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.project.maru.BuildConfig
import com.project.maru.R

private const val MAP_LATITUDE = "experience_map_latitude"
private const val MAP_LONGITUDE = "experience_map_longitude"

internal fun Intent.putExperienceMapCoordinates(point: EventCoordinates) {
    putExtra(MAP_LATITUDE, point.latitude)
    putExtra(MAP_LONGITUDE, point.longitude)
}

internal fun Intent.experienceMapCoordinates(): EventCoordinates? =
    EventCoordinates.fromApi(
        getDoubleExtra(MAP_LATITUDE, Double.NaN).toString(),
        getDoubleExtra(MAP_LONGITUDE, Double.NaN).toString()
    )

internal fun bindExperienceLocationPreview(activity: AppCompatActivity, title: String, place: String) {
    val point = activity.intent.experienceMapCoordinates() ?: return
    if (BuildConfig.KAKAO_MAP_API_KEY.isBlank()) return

    val section = activity.findViewById<View>(R.id.experienceLocationPreview)
    val host = section.findViewById<FrameLayout>(R.id.experienceLocationPreviewHost)
    val open = section.findViewById<View>(R.id.buttonOpenExperienceLocation)
    section.isVisible = true
    var opening = false
    val openMap = View.OnClickListener {
        if (!opening && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            opening = true
            activity.startActivity(ExperienceLocationActivity.intent(activity, title, place, point))
        }
    }
    open.setOnClickListener(openMap)
    section.findViewById<View>(R.id.buttonOpenExperienceLocationPreview).setOnClickListener(openMap)
    val locationMap = ExperienceLocationMap(activity, host, point,
        onUnavailable = { section.isVisible = false })
    val visible = Rect()
    val listener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (host.isShown && host.getGlobalVisibleRect(visible) && visible.height() >= host.height / 2) {
                host.viewTreeObserver.removeOnPreDrawListener(this)
                locationMap.start()
            }
            return true
        }
    }
    host.viewTreeObserver.addOnPreDrawListener(listener)
    activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            opening = false
        }

        override fun onDestroy(owner: LifecycleOwner) {
            if (host.viewTreeObserver.isAlive) host.viewTreeObserver.removeOnPreDrawListener(listener)
        }
    })
}

internal class ExperienceLocationMap(
    private val owner: LifecycleOwner,
    private val host: FrameLayout,
    point: EventCoordinates,
    private val interactive: Boolean = false,
    private val onUnavailable: () -> Unit,
    private val onReady: (KakaoMap) -> Unit = {}
) : DefaultLifecycleObserver {
    private val location = LatLng.from(point.latitude, point.longitude)
    private var view: MapView? = null
    private var destroyed = false
    private var available = true
    private var resumed = false

    init {
        owner.lifecycle.addObserver(this)
    }

    fun start() {
        if (view != null || destroyed || !available) return
        val context = host.context
        if (BuildConfig.KAKAO_MAP_API_KEY.isBlank()) {
            unavailable()
            return
        }
        try {
            if (!KakaoMapSdk.isInitialized()) {
                KakaoMapSdk.init(context.applicationContext, BuildConfig.KAKAO_MAP_API_KEY)
            }
            val mapView = MapView(context)
            view = mapView
            host.addView(mapView, FrameLayout.LayoutParams(-1, -1))
            mapView.start(object : MapLifeCycleCallback() {
                override fun onMapDestroy() = Unit

                override fun onMapError(error: Exception) {
                    if (!destroyed) unavailable()
                }
            }, object : KakaoMapReadyCallback() {
                override fun getPosition(): LatLng = location

                override fun getZoomLevel(): Int = 16

                override fun onMapReady(kakaoMap: KakaoMap) {
                    if (destroyed || !available) return
                    val icon = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_experience_map_marker))
                    val style = LabelStyle.from(icon.toBitmap(31, 41)).setApplyDpScale(true).setAnchorPoint(0.5f, 1f)
                    if (interactive) {
                        mapView.reverseMouseWheelScroll(false)
                        kakaoMap.setGestureEnable(GestureType.Zoom, true)
                        kakaoMap.setGestureEnable(GestureType.Pan, true)
                    }
                    kakaoMap.labelManager?.layer?.addLabel(LabelOptions.from(location).setStyles(style))
                    onReady(kakaoMap)
                }
            })
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) onResume(owner)
        } catch (_: Exception) {
            unavailable()
        } catch (_: LinkageError) {
            unavailable()
        }
    }

    private fun unavailable() {
        available = false
        host.isVisible = false
        onUnavailable()
    }

    override fun onResume(owner: LifecycleOwner) {
        val mapView = view ?: return
        if (!available || resumed) return
        mapView.resume()
        resumed = true
    }

    override fun onPause(owner: LifecycleOwner) {
        if (!resumed) return
        view?.pause()
        resumed = false
    }

    override fun onDestroy(owner: LifecycleOwner) {
        destroyed = true
        view?.finish()
        view = null
        host.removeAllViews()
    }
}

internal fun bindExperienceLocationZoom(map: KakaoMap, plus: View, minus: View) {
    fun updateButtons() {
        plus.isEnabled = map.zoomLevel < map.cameraMaxLevel
        minus.isEnabled = map.zoomLevel > map.cameraMinLevel
        plus.alpha = if (plus.isEnabled) 1f else 0.4f
        minus.alpha = if (minus.isEnabled) 1f else 0.4f
    }
    fun zoomBy(step: Int) {
        val target = (map.zoomLevel + step).coerceIn(map.cameraMinLevel, map.cameraMaxLevel)
        if (target != map.zoomLevel) {
            map.moveCamera(CameraUpdateFactory.zoomTo(target), CameraAnimation.from(270, true, false))
        }
    }
    plus.setOnClickListener { zoomBy(1) }
    minus.setOnClickListener { zoomBy(-1) }
    map.setOnCameraMoveEndListener { _, _, _ -> updateButtons() }
    updateButtons()
}
