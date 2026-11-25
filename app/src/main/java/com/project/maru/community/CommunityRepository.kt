package com.project.maru.community

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

// SQLite 원본은 유지하고, 공유 게시판의 저장과 조회는 Firestore에서 처리한다.
object CommunityRepository {
    private val posts get() = FirebaseFirestore.getInstance().collection("communityPosts")

    fun loadPosts(ownerId: String? = null): Task<List<CommunityPost>> {
        val query = if (ownerId == null) posts else posts.whereEqualTo("ownerId", ownerId)
        return query.get(Source.SERVER).continueWith { task ->
            val snapshot = task.result ?: throw IllegalStateException("게시글 응답이 없어요.")
            snapshot.documents.filter { it.getBoolean("deleting") != true }
                .map(::readPost).sortedByDescending { it.createdAt }
        }
    }

    fun loadPost(id: String): Task<CommunityPost?> = posts.document(id).get(Source.SERVER)
        .continueWith { task ->
            val document = task.result
            if (document.exists() && document.getBoolean("deleting") != true) readPost(document) else null
        }

    // 문서 ID를 재사용하여 재시도/화면 회전으로 같은 글이 중복 등록되지 않게 한다.
    fun createPost(post: CommunityPost): Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || uid != post.ownerId) {
            return com.google.android.gms.tasks.Tasks.forException(IllegalStateException("로그인 후 작성해 주세요."))
        }
        if (post.title.length !in 1..50 || post.content.length !in 1..5000) {
            return com.google.android.gms.tasks.Tasks.forException(IllegalArgumentException("제목은 50자, 내용은 5000자까지 입력해 주세요."))
        }
        if (com.project.maru.ContentPolicy.containsAbuse(post.title) || com.project.maru.ContentPolicy.containsAbuse(post.content)) {
            return com.google.android.gms.tasks.Tasks.forException(IllegalArgumentException(com.project.maru.ContentPolicy.MESSAGE))
        }
        val ref = posts.document(post.id)
        return FirebaseFirestore.getInstance().runTransaction { transaction ->
            val existing = transaction.get(ref)
            if (!existing.exists()) {
                transaction.set(ref, mapOf(
                    "id" to post.id, "category" to post.category, "title" to post.title,
                    "content" to post.content, "ownerId" to uid, "authorName" to post.authorName.ifBlank { "마루 이용자" }.take(100),
                    "createdAt" to (post.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()), "imageKey" to post.imageKey, "deleting" to false
                ))
            } else {
                check(existing.getString("ownerId") == uid) { "다른 사용자의 글이에요." }
            }
            null
        }
    }

    fun readPost(document: DocumentSnapshot): CommunityPost = CommunityPost(
        id = document.id,
        category = document.getString("category") ?: "육아정보",
        title = document.getString("title").orEmpty(),
        content = document.getString("content").orEmpty(),
        ownerId = document.getString("ownerId").orEmpty(),
        authorName = document.getString("authorName").orEmpty(),
        createdAt = when (val value = document.get("createdAt")) {
            is com.google.firebase.Timestamp -> value.toDate().time
            is Number -> value.toLong()
            else -> 0L
        },
        imageKey = document.getString("imageKey") ?: "child",
        deleting = document.getBoolean("deleting") ?: false
    )
}
