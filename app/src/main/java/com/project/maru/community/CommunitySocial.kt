package com.project.maru.community

import com.google.android.gms.tasks.Task
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.MyApplication.Companion.db
import com.google.firebase.firestore.FieldValue
import java.time.LocalDate
import java.time.ZoneId

// 공감/조회 문서와 숫자를 함께 저장해 중복 클릭과 동시 접속에도 숫자가 맞게 한다.
object CommunitySocial {
    const val NOTIFICATIONS = "communityNotifications"
    const val REPORTS = "communityReports"
    val reportReasons = arrayOf("욕설 및 비방", "광고 및 홍보", "부적절한 콘텐츠", "허위 정보", "개인정보 노출", "기타")

    fun setLiked(postId: String, liked: Boolean): Task<Boolean> {
        val uid = requireNotNull(auth.currentUser).uid
        val postRef = db.collection(CommunityFirestore.POSTS).document(postId)
        val reactionRef = postRef.collection("reactions").document(uid)
        return db.runTransaction { tx ->
            val post = tx.get(postRef)
            val reaction = tx.get(reactionRef)
            check(post.exists() && post.getBoolean("deleting") != true) { "삭제되었거나 삭제 중인 글이에요." }
            val previous = reaction.getBoolean("liked") == true
            if (previous != liked) {
                val count = post.getLong("likeCount") ?: 0L
                tx.update(postRef, "likeCount", (count + if (liked) 1 else -1).coerceAtLeast(0))
                tx.set(reactionRef, mapOf("liked" to liked,
                    "createdAt" to (reaction.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()))
                val owner = post.getString("ownerId").orEmpty()
                // 공감 이력은 취소해도 남겨 최초 공감에만 알림을 만든다.
                if (liked && !reaction.exists() && owner != uid) {
                    val id = "like_${postId}_$uid"
                    tx.set(db.collection(NOTIFICATIONS).document(id), notification(id, owner, uid,
                        postId, post.getString("title").orEmpty(), "like", uid))
                }
            }
            liked
        }
    }

    fun recordView(postId: String): Task<Void> {
        val uid = requireNotNull(auth.currentUser).uid
        val ref = db.collection(CommunityFirestore.POSTS).document(postId)
        val viewRef = ref.collection("views").document(uid)
        val day = LocalDate.now(ZoneId.of("Asia/Seoul")).toEpochDay()
        return db.runTransaction { tx ->
            val post = tx.get(ref)
            val view = tx.get(viewRef)
            if (post.exists() && post.getBoolean("deleting") != true && post.getString("ownerId") != uid &&
                view.getLong("day") != day) {
                tx.set(viewRef, mapOf("day" to day, "viewedAt" to FieldValue.serverTimestamp()))
                tx.update(ref, "viewCount", (post.getLong("viewCount") ?: 0L) + 1L)
            }
            null
        }
    }

    fun addComment(comment: CommunityComment): Task<Void> {
        val uid = requireNotNull(auth.currentUser).uid
        val parent = db.collection(CommunityFirestore.POSTS).document(comment.postId)
        val ref = parent.collection(CommunityFirestore.COMMENTS).document(comment.id)
        return db.runTransaction { tx ->
            val post = tx.get(parent)
            val previous = tx.get(ref)
            check(post.exists() && post.getBoolean("deleting") != true) { "삭제되었거나 삭제 중인 글이에요." }
            if (!previous.exists()) {
                tx.set(ref, comment)
                val owner = post.getString("ownerId").orEmpty()
                if (owner != uid) {
                    val id = "comment_${comment.postId}_${comment.id}"
                    tx.set(db.collection(NOTIFICATIONS).document(id), notification(id, owner, uid,
                        comment.postId, post.getString("title").orEmpty(), "comment", comment.id))
                }
            } else check(previous.getString("ownerId") == uid) { "댓글 작성자를 확인해 주세요." }
            null
        }
    }

    fun report(postId: String, reason: String): Task<Boolean> {
        val uid = requireNotNull(auth.currentUser).uid
        val parent = db.collection(CommunityFirestore.POSTS).document(postId)
        val report = db.collection(REPORTS).document("${postId}_$uid")
        return db.runTransaction { tx ->
            val post = tx.get(parent)
            val previous = tx.get(report)
            check(post.exists() && post.getBoolean("deleting") != true) { "삭제되었거나 삭제 중인 글이에요." }
            check(post.getString("ownerId") != uid) { "본인 게시글은 신고할 수 없어요." }
            if (previous.exists()) false else {
                tx.set(report, mapOf("postId" to postId, "reporterId" to uid,
                    "postOwnerId" to post.getString("ownerId"), "postTitle" to post.getString("title"),
                    "reason" to reason, "status" to "pending", "createdAt" to FieldValue.serverTimestamp()))
                true
            }
        }
    }

    private fun notification(id: String, recipient: String, actor: String, postId: String,
                             title: String, type: String, source: String): Map<String, Any> = mapOf(
        "id" to id, "recipientId" to recipient, "actorId" to actor, "postId" to postId,
        "postTitle" to title, "type" to type, "sourceId" to source,
        "createdAt" to FieldValue.serverTimestamp(), "read" to false)
}
