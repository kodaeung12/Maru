package com.project.maru.community

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.MyApplication.Companion.db

data class MyCommentItem(val comment: CommunityComment, val postTitle: String, val category: String)

object MyCommentsRepository {
    // 기존 게시글/댓글 읽기 규칙을 사용한다. collectionGroup용 규칙이나 색인이 필요 없다.
    fun load(uid: String, isActive: () -> Boolean, onSuccess: (List<MyCommentItem>) -> Unit,
             onFailure: (Exception) -> Unit) {
        val items = mutableListOf<MyCommentItem>()
        fun active() = isActive() && auth.currentUser?.uid == uid
        fun loadPage(after: DocumentSnapshot?) {
            if (!active()) return
            var query = db.collection(CommunityFirestore.POSTS).orderBy(FieldPath.documentId()).limit(50)
            if (after != null) query = query.startAfter(after)
            query.get(Source.SERVER).addOnSuccessListener { page ->
                if (!active()) return@addOnSuccessListener
                val posts = page.documents.filter { it.getBoolean("deleting") != true }
                fun loadComments(start: Int) {
                    if (!active()) return
                    if (start >= posts.size) {
                        if (page.size() == 50) loadPage(page.documents.last())
                        else onSuccess(items.sortedWith(compareByDescending<MyCommentItem> { it.comment.createdAt }
                            .thenBy { it.comment.postId }.thenBy { it.comment.id }))
                        return
                    }
                    val batch = posts.drop(start).take(10)
                    val requests = batch.map { post ->
                        post.reference.collection(CommunityFirestore.COMMENTS)
                            .whereEqualTo("ownerId", uid).get(Source.SERVER)
                    }
                    Tasks.whenAllSuccess<QuerySnapshot>(requests).addOnSuccessListener { results ->
                        if (!active()) return@addOnSuccessListener
                        try {
                            for ((index, snapshot) in results.withIndex()) {
                                val post = batch[index]
                                for (document in snapshot) {
                                    val comment = document.toObject(CommunityComment::class.java)
                                    if (comment.ownerId != uid) continue
                                    comment.id = document.id
                                    comment.postId = post.id
                                    items.add(MyCommentItem(comment, post.getString("title").orEmpty(),
                                        CommunityPostFilters.category(post.getString("category").orEmpty())))
                                }
                            }
                            loadComments(start + batch.size)
                        } catch (error: Exception) { if (active()) onFailure(error) }
                    }.addOnFailureListener { if (active()) onFailure(it) }
                }
                loadComments(0)
            }.addOnFailureListener { if (active()) onFailure(it) }
        }
        loadPage(null)
    }
}
