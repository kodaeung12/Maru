package com.project.maru

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.project.maru.community.CommunityPost
import com.project.maru.community.CommunityRepository
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseOnlineInstrumentedTest {
    private fun uid(): String {
        val user = FirebaseAuth.getInstance().currentUser
        assertNotNull("실제 Firebase 검증에는 로그인된 에뮬레이터가 필요합니다.", user)
        return user!!.uid
    }

    @Test fun postIsStoredOnServerAndRetryDoesNotDuplicate() {
        val owner = uid()
        val id = "integration_" + UUID.randomUUID()
        val ref = FirebaseFirestore.getInstance().collection("communityPosts").document(id)
        val post = CommunityPost(id, "일상공유", "[연동 확인]", "Firebase 온라인 저장 검증용 글", owner, "연동 확인", System.currentTimeMillis())
        var created = false
        try {
            Tasks.await(CommunityRepository.createPost(post), 30, TimeUnit.SECONDS)
            created = true
            Tasks.await(CommunityRepository.createPost(post), 30, TimeUnit.SECONDS)
            val stored = Tasks.await(CommunityRepository.loadPost(id), 30, TimeUnit.SECONDS)
            assertEquals(id, stored?.id)
            assertEquals(post.content, stored?.content)
            assertTrue(Tasks.await(CommunityRepository.loadPosts(owner), 30, TimeUnit.SECONDS).any { it.id == id })
        } finally {
            // 테스트 글은 기존 게시판 방식처럼 숨김 처리하여 원격 원본을 보존한다.
            if (created) Tasks.await(ref.update("deleting", true), 30, TimeUnit.SECONDS)
        }
    }

    @Test fun reviewIsStoredAndUpdatedInSameDocument() {
        val owner = uid()
        val event = "integration_" + UUID.randomUUID()
        val ref = FirebaseFirestore.getInstance().collection("events").document(event).collection("reviews").document(owner)
        var created = false
        try {
            Tasks.await(ReviewRepository.save(event, 4, "연동 검증"), 30, TimeUnit.SECONDS)
            created = true
            val first = Tasks.await(ref.get(Source.SERVER), 30, TimeUnit.SECONDS)
            assertNotNull(first.getTimestamp("createdAt"))
            Tasks.await(ReviewRepository.save(event, 5, "수정 검증"), 30, TimeUnit.SECONDS)
            val reviews = Tasks.await(ReviewRepository.load(event), 30, TimeUnit.SECONDS)
            assertEquals(1, reviews.size)
            assertEquals(5, reviews.single().rating)
            assertEquals("수정 검증", reviews.single().content)
            assertEquals(owner, reviews.single().ownerId)
            assertEquals(first.getTimestamp("createdAt"), Tasks.await(ref.get(Source.SERVER), 30, TimeUnit.SECONDS).getTimestamp("createdAt"))
        } finally {
            // 이 테스트에서 생성한 임의 행사 아래의 테스트 리뷰만 정리한다.
            if (created) Tasks.await(ref.delete(), 30, TimeUnit.SECONDS)
        }
    }

    @Test fun serverBlocksAbusivePostsReviewsAndComments() {
        val owner = uid()
        val db = FirebaseFirestore.getInstance()
        val id = "integration_" + UUID.randomUUID()
        val post = db.collection("communityPosts").document(id)
        val review = db.collection("events").document(id).collection("reviews").document(owner)
        val comment = post.collection("comments").document(id)
        try {
            val postData = mapOf("id" to id, "category" to "일상공유", "title" to "연동 검증",
                "content" to "씨 발", "ownerId" to owner, "authorName" to "검증",
                "createdAt" to System.currentTimeMillis(), "imageKey" to "daily", "deleting" to false)
            assertDenied(post.set(postData))
            assertDenied(review.set(mapOf("ownerId" to owner, "authorName" to "검증", "rating" to 5,
                "content" to "normal\n개.새.끼", "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())))
            // 정상적인 단어의 일부인 '시발점'은 허용한다.
            Tasks.await(post.set(postData + ("content" to "변화의 시발점")), 30, TimeUnit.SECONDS)
            assertDenied(comment.set(mapOf("id" to id, "postId" to id, "ownerId" to owner,
                "authorName" to "검증", "content" to "병*신", "createdAt" to System.currentTimeMillis())))
        } finally {
            if (Tasks.await(comment.get(Source.SERVER), 30, TimeUnit.SECONDS).exists()) {
                Tasks.await(comment.delete(), 30, TimeUnit.SECONDS)
            }
            if (Tasks.await(post.get(Source.SERVER), 30, TimeUnit.SECONDS).exists()) {
                Tasks.await(post.update("deleting", true), 30, TimeUnit.SECONDS)
            }
            if (Tasks.await(review.get(Source.SERVER), 30, TimeUnit.SECONDS).exists()) {
                Tasks.await(review.delete(), 30, TimeUnit.SECONDS)
            }
        }
    }

    private fun assertDenied(task: com.google.android.gms.tasks.Task<*>) {
        try {
            Tasks.await(task, 30, TimeUnit.SECONDS)
            fail("서버에서 차단해야 하는 요청이 허용됐습니다.")
        } catch (error: java.util.concurrent.ExecutionException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,
                (error.cause as FirebaseFirestoreException).code)
        }
    }

    @Test fun cannotWriteReviewInAnotherUsersName() {
        uid()
        val event = "integration_" + UUID.randomUUID()
        val ref = FirebaseFirestore.getInstance().collection("events").document(event).collection("reviews").document("another-user")
        val data = mapOf("ownerId" to "another-user", "authorName" to "검증", "rating" to 5,
            "content" to "차단 검증", "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
        try {
            Tasks.await(ref.set(data), 30, TimeUnit.SECONDS)
            fail("다른 사용자 명의의 리뷰 쓰기가 허용되면 안 됩니다.")
        } catch (error: java.util.concurrent.ExecutionException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, (error.cause as FirebaseFirestoreException).code)
        }
    }
}
