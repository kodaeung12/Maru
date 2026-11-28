package com.project.maru.experience.location

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.project.maru.R
import java.io.ByteArrayOutputStream

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
    private val point: EventCoordinates,
    private val interactive: Boolean = false,
    private val onUnavailable: () -> Unit,
    private val onReady: (ExperienceLocationMap) -> Unit = {}
) : DefaultLifecycleObserver {
    private var view: WebView? = null
    private var available = true
    private var started = false
    private val checkTiles = Runnable { checkTileStatus() }
    private var remainingChecks = 60

    init {
        owner.lifecycle.addObserver(this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun start() {
        if (started || owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        started = true
        try {
            val marker = requireNotNull(ContextCompat.getDrawable(host.context, R.drawable.ic_experience_map_marker))
            val markerBytes = ByteArrayOutputStream().apply {
                marker.toBitmap(31, 41).compress(Bitmap.CompressFormat.PNG, 100, this)
            }.toByteArray()
            val web = WebView(host.context)
            view = web
            web.settings.apply {
                javaScriptEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                userAgentString += " MARU/1.0 (+https://github.com/kodaeung12/Maru)"
            }
            web.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    val url = request.url
                    if (url.scheme == "https" && url.host == "tile.openstreetmap.org" && url.port == -1) {
                        return null
                    }
                    if (url.scheme == "https" && url.host == MAP_HOST && url.port == -1) {
                        val (name, mime) = when (url.path) {
                            "/index.html" -> "index.html" to "text/html"
                            "/map.js" -> "map.js" to "text/javascript"
                            "/leaflet.js" -> "leaflet.js" to "text/javascript"
                            "/leaflet.css" -> "leaflet.css" to "text/css"
                            "/marker.png" -> return WebResourceResponse("image/png", null, markerBytes.inputStream())
                            else -> return blockedResponse()
                        }
                        return WebResourceResponse(mime, "UTF-8", host.context.assets.open("experience_map/$name"))
                    }
                    return blockedResponse()
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (interactive && request.isForMainFrame && request.hasGesture() &&
                        request.url.toString() == "https://www.openstreetmap.org/copyright"
                    ) {
                        try {
                            host.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        } catch (_: ActivityNotFoundException) {
                        }
                    }
                    return true
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) unavailable()
                }

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    unavailable()
                    return true
                }
            }
            host.addView(web, FrameLayout.LayoutParams(-1, -1))
            web.loadUrl("https://$MAP_HOST/index.html#${point.latitude},${point.longitude},$interactive")
            host.postDelayed(checkTiles, 250)
        } catch (_: RuntimeException) {
            unavailable()
        }
    }

    fun zoomBy(step: Int) {
        view?.evaluateJavascript("window.maruZoom && window.maruZoom($step)", null)
    }

    private fun checkTileStatus() {
        val web = view ?: return
        web.evaluateJavascript("window.maruMapState || 0") { state ->
            if (view !== web) return@evaluateJavascript
            when {
                state == "1" -> onReady(this)
                --remainingChecks <= 0 -> unavailable()
                else -> host.postDelayed(checkTiles, 250)
            }
        }
    }

    private fun blockedResponse() = WebResourceResponse(
        "text/plain", "UTF-8", 403, "Forbidden", emptyMap(), ByteArray(0).inputStream()
    )

    private fun unavailable() {
        if (!available) return
        available = false
        releaseView()
        host.isVisible = false
        onUnavailable()
    }

    override fun onResume(owner: LifecycleOwner) {
        view?.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        view?.onPause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        releaseView()
    }

    private fun releaseView() {
        host.removeCallbacks(checkTiles)
        val web = view ?: return
        view = null
        host.removeView(web)
        web.destroy()
    }

    private companion object {
        const val MAP_HOST = "appassets.androidplatform.net"
    }
}
