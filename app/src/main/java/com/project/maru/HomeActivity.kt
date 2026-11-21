package com.project.maru   // ← 내 프로젝트 패키지명과 다르면 이 줄만 바꿔주세요

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    private val navController: NavController
        get() = (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // 상태바 높이만큼 위쪽 여백 (아래쪽은 하단 네비게이션 바가 알아서 처리)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // 하단 네비게이션 바 ↔ nav_graph.xml 연결
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)
        handleHomeRequest(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleHomeRequest(intent)
    }

    private fun handleHomeRequest(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_SHOW_HOME, false)) {
            intent.removeExtra(EXTRA_SHOW_HOME)
            navController.showHome()
        }
    }

    companion object {
        private const val EXTRA_SHOW_HOME = "com.project.maru.SHOW_HOME"

        internal fun homeIntent(context: Context): Intent = Intent(context, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_SHOW_HOME, true)
    }
}

internal fun NavController.showHome() {
    if (currentDestination?.id != R.id.homeFragment && !popBackStack(R.id.homeFragment, false)) {
        navigate(R.id.homeFragment, null, navOptions {
            launchSingleTop = true
            popUpTo(graph.id)
        })
    }
}
