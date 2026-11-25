package com.project.maru

import android.view.View
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SettingsInstrumentedTest {
    private fun <T> await(task: com.google.android.gms.tasks.Task<T>): T=Tasks.await(task,60,TimeUnit.SECONDS)
    @Test fun nameOpensProfileAndGearOpensSettingsWithSafeCancel() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host=activity.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                host.navController.navigate(R.id.myPageFragment)
            }
            scenario.onActivity { activity ->
                assertNull(activity.resources.getIdentifier("buttonLogoutFromHome","id",activity.packageName).takeIf { it!=0 }?.let { activity.findViewById<View>(it) })
                activity.findViewById<View>(R.id.tvProfileName).performClick()
                val host=activity.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                assertEquals(R.id.myInfoFragment,host.navController.currentDestination?.id)
                host.navController.popBackStack()
            }
            scenario.onActivity { activity -> activity.findViewById<View>(R.id.btnSettings).performClick() }
            scenario.onActivity { activity ->
                val host=activity.supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
                assertEquals(R.id.settingsFragment,host.navController.currentDestination?.id)
                activity.findViewById<View>(R.id.buttonServiceTerms).performClick()
            }
            onView(withText("서비스 이용약관")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).check(matches(isDisplayed()))
            onView(withText("확인")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
            scenario.onActivity { it.findViewById<View>(R.id.buttonSettingsLogout).performClick() }
            onView(withText("로그아웃할까요?")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).check(matches(isDisplayed()))
            onView(withText("취소")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
            scenario.onActivity { it.findViewById<View>(R.id.buttonDeleteAccount).performClick() }
            onView(withText("회원탈퇴 안내")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).check(matches(isDisplayed()))
            onView(withText("취소")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
        }
    }

    @Test fun disposableAccountCleanupDeletesOwnDataAndAuthentication() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val defaultUid=FirebaseAuth.getInstance().currentUser?.uid
        val originalDb=FirebaseFirestore.getInstance()
        val oldArtifacts=await(originalDb.collection("communityPosts").whereEqualTo("title","회원탈퇴 기능 테스트").get(Source.SERVER))
        oldArtifacts.documents.filter { it.id.startsWith("withdrawal-test-") }.forEach {
            println("DISPOSABLE_ARTIFACT post="+it.id+" uid="+it.getString("ownerId"))
        }
        await(originalDb.collectionGroup("reviews").whereEqualTo("ownerId",defaultUid ?: "no-user").limit(1).get(Source.SERVER))
        val app=FirebaseApp.initializeApp(context,FirebaseApp.getInstance().options,"withdrawal-test-"+UUID.randomUUID())
        val auth=FirebaseAuth.getInstance(app)
        val db=FirebaseFirestore.getInstance(app)
        var user: com.google.firebase.auth.FirebaseUser?=null
        val token=UUID.randomUUID().toString()
        val postId="withdrawal-test-"+token
        val eventId="withdrawal-test-"+token
        try {
            user=await(auth.createUserWithEmailAndPassword("maru-withdrawal-"+token+"@example.com",UUID.randomUUID().toString()+"Aa!7")).user!!
            val uid=user.uid
            val profile=db.collection("users").document(uid)
            await(profile.set(mapOf("displayName" to "탈퇴 테스트")))
            await(profile.collection("children").document("test").set(mapOf("name" to "테스트 아이")))
            await(profile.collection("schedules").document("test").set(mapOf("title" to "테스트 일정","note" to "","startsAt" to (System.currentTimeMillis()+86400000),"reminderMinutes" to -1,"revision" to UUID.randomUUID().toString(),"eventId" to "","createdAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp())))
            val post=db.collection("communityPosts").document(postId)
            await(post.set(mapOf("id" to postId,"ownerId" to uid,"authorName" to "탈퇴 테스트","title" to "회원탈퇴 기능 테스트","content" to "기능 검증 후 삭제되는 테스트 글입니다.","category" to "육아정보","createdAt" to System.currentTimeMillis(),"imageKey" to "child","deleting" to false)))
            await(post.collection("comments").document("test").set(mapOf("id" to "test","postId" to postId,"ownerId" to uid,"authorName" to "탈퇴 테스트","content" to "테스트 댓글","createdAt" to System.currentTimeMillis())))
            val review=db.collection("events").document(eventId).collection("reviews").document(uid)
            await(review.set(mapOf("ownerId" to uid,"authorName" to "탈퇴 테스트","rating" to 5,"content" to "테스트 리뷰","createdAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp())))
            AccountDataCleanup.remove(context,db,uid)
            assertFalse(await(profile.get(Source.SERVER)).exists())
            assertTrue(await(profile.collection("children").get(Source.SERVER)).isEmpty)
            assertTrue(await(profile.collection("schedules").get(Source.SERVER)).isEmpty)
            assertFalse(await(post.get(Source.SERVER)).exists())
            assertFalse(await(review.get(Source.SERVER)).exists())
            await(user.delete())
            user=null
            assertNull(auth.currentUser)
            assertEquals(defaultUid,FirebaseAuth.getInstance().currentUser?.uid)
        } finally {
            if(user!=null) {
                runCatching { AccountDataCleanup.remove(context,db,user!!.uid) }
                // A failed preflight must not strand the synthetic documents when its auth account is removed.
                val uid=user!!.uid
                runCatching { await(db.collection("communityPosts").document(postId).update("deleting",true)) }
                runCatching { await(db.collection("communityPosts").document(postId).collection("comments").document("test").delete()) }
                runCatching { await(db.collection("communityPosts").document(postId).delete()) }
                runCatching { await(db.collection("events").document(eventId).collection("reviews").document(uid).delete()) }
                runCatching { await(db.collection("users").document(uid).collection("children").document("test").delete()) }
                runCatching { await(db.collection("users").document(uid).collection("schedules").document("test").delete()) }
                runCatching { await(db.collection("users").document(uid).delete()) }
                runCatching { await(user!!.delete()) }
            }
            runCatching { await(db.terminate()) }
            runCatching { await(db.clearPersistence()) }
            app.delete()
        }
    }
}
