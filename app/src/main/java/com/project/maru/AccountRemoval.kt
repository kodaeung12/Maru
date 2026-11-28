package com.project.maru

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// Firestore 하위 문서는 부모/인증 계정 삭제만으로 지워지지 않으므로 인증을 유지한 채 먼저 정리한다.
internal class AccountRemoval(app: Application) : AndroidViewModel(app) {
    data class State(val busy: Boolean = false, val message: String = "", val complete: Boolean = false)
    val state = MutableLiveData(State())
    private val worker = Executors.newSingleThreadExecutor()
    private val db get() = FirebaseFirestore.getInstance()
    private fun <T> waitFor(task: com.google.android.gms.tasks.Task<T>): T = Tasks.await(task,60,TimeUnit.SECONDS)

    fun delete(password: String) {
        if (state.value?.busy == true) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val email = user.email ?: run { state.value=State(message="이메일 계정으로 다시 로그인해 주세요."); return }
        state.value=State(busy=true,message="비밀번호를 확인하는 중이에요.")
        worker.execute {
            var started=false
            try {
                waitFor(user.reauthenticate(EmailAuthProvider.getCredential(email,password)))
                started=true
                AccountDataCleanup.remove(getApplication(),db,user.uid) { message -> state.postValue(State(true,message)) }
                // 이 단계가 실패하면 재로그인/재시도로 남은 인증 계정을 정리할 수 있다.
                waitFor(user.delete())
                ScheduleAlarms.clearOtherAccount(getApplication(),null)
                getApplication<Application>().getSharedPreferences("recent_experiences",Context.MODE_PRIVATE).edit().clear().commit()
                state.postValue(State(message="회원탈퇴가 완료되었어요.",complete=true))
            } catch(error: Exception) {
                val reason=if(started) "탈퇴 처리가 완료되지 않았어요. 일부 데이터는 이미 삭제되었을 수 있어요. 연결 상태를 확인하고 다시 시도해 주세요."
                    else "비밀번호 확인에 실패했어요. 비밀번호와 인터넷 연결을 확인해 주세요."
                state.postValue(State(message=reason))
            }
        }
    }

    override fun onCleared() { worker.shutdown(); super.onCleared() }
}

// The cleanup routine takes explicit Firebase dependencies so tests can use an isolated disposable account.
internal object AccountDataCleanup {
    private fun <T> waitFor(task: com.google.android.gms.tasks.Task<T>): T = Tasks.await(task,60,TimeUnit.SECONDS)
    fun remove(context: Context, db: FirebaseFirestore, uid: String, onProgress: (String)->Unit = {}) {
                val profile=db.collection("users").document(uid)
                // Fail before deleting anything if the required server query/index is unavailable.
                waitFor(db.collectionGroup("reviews").whereEqualTo("ownerId",uid).limit(1).get(Source.SERVER))
                waitFor(profile.get(Source.SERVER))
                waitFor(profile.set(mapOf("accountDeletionRequested" to true),com.google.firebase.firestore.SetOptions.merge()))
                onProgress("작성한 글·댓글·리뷰를 삭제하는 중이에요. 앱을 종료하지 마세요.")
                // 본인 글은 삭제 중으로 표시해 새 댓글을 막고, 댓글까지 정리한 뒤 글을 지운다.
                val ownPosts=db.collection("communityPosts").whereEqualTo("ownerId",uid).limit(100)
                while(true) {
                    val posts=waitFor(ownPosts.get(Source.SERVER)).documents
                    if(posts.isEmpty()) break
                    for(post in posts) {
                        waitFor(post.reference.update("deleting",true))
                        listOf("comments","reactions","views").forEach { deleteQuery(db, post.reference.collection(it)) }
                        waitFor(post.reference.delete())
                    }
                }
                // 다른 사람이 쓴 글에 남긴 본인 댓글도 정리한다.
                scan(db.collection("communityPosts")) { post ->
                    deleteQuery(db, post.reference.collection("comments").whereEqualTo("ownerId",uid))
                    val reaction=post.reference.collection("reactions").document(uid)
                    // Undo the like count with the same atomic contract as the community screen.
                    waitFor(db.runTransaction { tx ->
                        val snapshot=tx.get(reaction)
                        val parent=tx.get(post.reference)
                        if(snapshot.exists() && snapshot.getBoolean("liked")==true && parent.exists() && parent.getBoolean("deleting")!=true) {
                            tx.update(reaction,mapOf("liked" to false,"updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                            tx.update(post.reference,"likeCount",((parent.getLong("likeCount") ?: 0L)-1L).coerceAtLeast(0L))
                        }
                        null
                    })
                    if(waitFor(reaction.get(Source.SERVER)).exists()) waitFor(reaction.delete())
                    val viewed=post.reference.collection("views").document(uid)
                    if(waitFor(viewed.get(Source.SERVER)).exists()) waitFor(viewed.delete())
                }
                // reviews의 상위 events 문서가 없는 경우도 포함해 조회한다.
                scan(db.collectionGroup("reviews").whereEqualTo("ownerId",uid)) { review ->
                    if(review.getString("ownerId")==uid && review.reference.path.matches(Regex("events/[^/]+/reviews/[^/]+"))) waitFor(review.reference.delete())
                }
                onProgress("아이 정보·즐겨찾기·일정과 계정 정보를 삭제하는 중이에요.")
                // 공개 순위 집계는 유지하고 계정과 연결된 중복 집계 방지 기록만 제거한다.
                scan(db.collection("searchTerms")) { term ->
                    val contribution=term.reference.collection("contributors").document(uid)
                    if(waitFor(contribution.get(Source.SERVER)).exists()) waitFor(contribution.delete())
                }
                deleteQuery(db, db.collection("communityNotifications").whereEqualTo("recipientId",uid))
                deleteQuery(db, db.collection("communityNotifications").whereEqualTo("actorId",uid))
                com.project.maru.community.CommunityDatabaseHelper(context).use { helper ->
                    helper.writableDatabase.delete("community_posts","owner_id = ?",arrayOf(uid))
                }
                listOf("children","favorites","schedules","diaries").forEach { deleteQuery(db, profile.collection(it)) }
                waitFor(profile.delete())
    }
    private fun deleteQuery(db: FirebaseFirestore, query: Query) {
        while(true) {
            val docs=waitFor(query.limit(400).get(Source.SERVER)).documents
            if(docs.isEmpty()) return
            val batch=db.batch()
            docs.forEach { batch.delete(it.reference) }
            waitFor(batch.commit())
        }
    }
    private fun scan(query: Query, action: (com.google.firebase.firestore.DocumentSnapshot)->Unit) {
        var cursor: com.google.firebase.firestore.DocumentSnapshot?=null
        while(true) {
            var page=query.limit(200)
            cursor?.let { page=page.startAfter(it) }
            val docs=waitFor(page.get(Source.SERVER)).documents
            docs.forEach(action)
            if(docs.size<200) return
            cursor=docs.last()
        }
    }
}
