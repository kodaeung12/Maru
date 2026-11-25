package com.project.maru

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.Stage
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class SessionStartupInstrumentedTest {
    @Test fun verifiedSessionOpensHomeWithoutShowingAuthenticationChoice() {
        val uid=FirebaseAuth.getInstance().currentUser?.uid
        assumeTrue(FirebaseAuth.getInstance().currentUser?.isEmailVerified==true)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val promptShown=AtomicBoolean(false)
        val callback=ActivityLifecycleCallback { activity,stage ->
            if(activity is LoginActivity && stage==Stage.CREATED) promptShown.set(true)
        }
        val lifecycle=ActivityLifecycleMonitorRegistry.getInstance()
        instrumentation.runOnMainSync { lifecycle.addLifecycleCallback(callback) }
        val monitor=instrumentation.addMonitor(HomeActivity::class.java.name,null,false)
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                val home=monitor.waitForActivityWithTimeout(20000)
                assertNotNull("저장된 로그인으로 홈 진입",home)
                assertFalse("로그인된 사용자는 중간 LoginActivity 없이 홈으로 이동해야 함",promptShown.get())
                assertEquals(uid,FirebaseAuth.getInstance().currentUser?.uid)
                instrumentation.runOnMainSync { home.finish() }
            }
        } finally {
            instrumentation.runOnMainSync { lifecycle.removeLifecycleCallback(callback) }
            instrumentation.removeMonitor(monitor)
        }
    }
}
