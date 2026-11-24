package com.project.maru

import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeNavigationInstrumentedTest {
    @Test
    fun reselectingHomeFromSearchReturnsToHome() {
        assertHomeReselection(R.id.action_home_to_search, R.id.searchFragment)
    }

    @Test
    fun reselectingHomeFromFavoritesReturnsToHome() {
        assertHomeReselection(R.id.action_home_to_favorites, R.id.favoritesFragment)
    }

    private fun assertHomeReselection(actionId: Int, destinationId: Int) {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                val nav = host.navController
                val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
                nav.navigate(actionId)
                assertEquals(destinationId, nav.currentDestination?.id)
                assertEquals(R.id.homeFragment, bottomNav.selectedItemId)
                bottomNav.selectedItemId = R.id.homeFragment
                assertEquals(R.id.homeFragment, nav.currentDestination?.id)
            }
        }
    }
}