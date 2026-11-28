package com.project.maru

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
        // 검색 같은 하위 화면에서는 홈 탭이 이미 선택되어 있으므로 재선택도 처리한다.
        bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.homeFragment) {
                navController.showHome()
            }
        }
        handleHomeRequest(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleHomeRequest(intent)
        window.decorView.post { openNotificationSchedule() }
    }

    override fun onPostResume() {
        super.onPostResume()
        openNotificationSchedule()
    }

    private fun openNotificationSchedule() {
        val id = intent.getStringExtra("openScheduleId") ?: return
        val uid = intent.getStringExtra("scheduleUid")
        intent.removeExtra("openScheduleId")
        if (uid == com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid) {
            val item = ScheduleSync.items.firstOrNull { it.id == id }
            if (item != null) ScheduleUi.showEditor(this, item = item) else ScheduleUi.showNotifications(this)
        }
    }

    private fun handleHomeRequest(intent: Intent) {
        val destinationId = intent.getIntExtra(EXTRA_NAV_DESTINATION, INVALID_DESTINATION)
        if (destinationId != INVALID_DESTINATION) {
            intent.removeExtra(EXTRA_NAV_DESTINATION)
            navController.showDestination(destinationId)
            return
        }
        if (intent.getBooleanExtra(EXTRA_SHOW_HOME, false)) {
            intent.removeExtra(EXTRA_SHOW_HOME)
            navController.showHome()
        }
    }

    companion object {
        private const val EXTRA_SHOW_HOME = "com.project.maru.SHOW_HOME"
        private const val EXTRA_NAV_DESTINATION = "com.project.maru.NAV_DESTINATION"
        private const val INVALID_DESTINATION = -1

        internal fun homeIntent(context: Context): Intent = Intent(context, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_SHOW_HOME, true)

        internal fun sectionIntent(context: Context, destinationId: Int): Intent =
            Intent(context, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_NAV_DESTINATION, destinationId)
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

internal fun NavController.showDestination(destinationId: Int) {
    if (currentDestination?.id == destinationId) return
    if (!popBackStack(destinationId, false)) {
        navigate(destinationId, null, navOptions {
            launchSingleTop = true
            popUpTo(graph.id)
        })
    }
}
