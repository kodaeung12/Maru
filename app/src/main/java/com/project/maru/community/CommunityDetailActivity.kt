package com.project.maru.community

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.MyApplication.Companion.db
import com.project.maru.databinding.ActivityCommunityDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommunityDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommunityDetailBinding
    private lateinit var commentAdapter: CommunityCommentAdapter
    private var postId = ""
    private var currentPost: CommunityPost? = null
    private var postAvailable = false
    private var postLoadNumber = 0
    private var commentLoadNumber = 0
    private var pendingCommentId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCommunityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        postId = intent.getStringExtra("postId") ?: ""
        commentAdapter = CommunityCommentAdapter(auth.currentUser?.uid ?: "") { comment -> confirmCommentDelete(comment) }
        binding.commentList.layoutManager = LinearLayoutManager(this)
        binding.commentList.adapter = commentAdapter
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { loadPost() }
        binding.btnRefreshComments.setOnClickListener { if (postAvailable) loadComments() }
        binding.btnSendComment.setOnClickListener { saveComment() }
        binding.btnEdit.setOnClickListener {
            val post = currentPost
            if (post != null && auth.currentUser?.uid == post.ownerId) {
                val intent = Intent(this, CommunityWriteActivity::class.java)
                intent.putExtra("postId", postId)
                startActivity(intent)
            }
        }
        binding.btnDelete.setOnClickListener { confirmPostDelete() }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        // 수정 화면에서 돌아왔을 때 서버의 최신 글과 댓글을 다시 조회
        loadPost()
    }

    private fun loadPost() {
        postLoadNumber++
        val requestNumber = postLoadNumber
        commentLoadNumber++
        postAvailable = false
        currentPost = null
        binding.postBody.visibility = View.GONE
        binding.ownerActions.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
        if (postId.isBlank() || auth.currentUser == null) {
            binding.tvStatus.text = "로그인 상태와 게시글 정보를 확인해 주세요."
            return
        }
        binding.tvStatus.text = "게시글을 불러오는 중…"
        db.collection(CommunityFirestore.POSTS).document(postId).get(Source.SERVER)
            .addOnSuccessListener { document ->
                if (isFinishing || isDestroyed || requestNumber != postLoadNumber) return@addOnSuccessListener
                try {
                    val post = document.toObject(CommunityPost::class.java)
                    if (post == null) {
                        binding.tvStatus.text = "삭제되었거나 존재하지 않는 게시글이에요."
                        return@addOnSuccessListener
                    }
                    post.id = document.id
                    currentPost = post
                    postAvailable = true
                    binding.postBody.visibility = View.VISIBLE
                    binding.tvCategory.text = post.category
                    binding.tvTitle.text = post.title
                    binding.tvOwner.text = if (post.authorName.isBlank()) "마루 이용자" else post.authorName
                    binding.tvDate.text = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(post.createdAt))
                    binding.tvContent.text = post.content
                    CommunityImages.show(binding.ivRepresentative, post.imageKey)
                    binding.ownerActions.visibility = if (post.ownerId == auth.currentUser?.uid) View.VISIBLE else View.GONE
                    binding.btnEdit.isEnabled = !post.deleting
                    binding.btnDelete.isEnabled = true
                    binding.etComment.isEnabled = !post.deleting
                    binding.btnSendComment.isEnabled = !post.deleting
                    binding.tvStatus.text = if (post.deleting) "삭제 처리 중인 글이에요. 작성자는 삭제 버튼으로 다시 시도할 수 있어요." else ""
                    loadComments()
                } catch (error: Exception) { showPostError(error) }
            }
            .addOnFailureListener { error ->
                if (!isFinishing && !isDestroyed && requestNumber == postLoadNumber) showPostError(error)
            }
    }

    private fun showPostError(error: Exception) {
        binding.tvStatus.text = CommunityFirestore.errorMessage(error)
        binding.btnRetry.visibility = View.VISIBLE
    }

    private fun confirmPostDelete() {
        val post = currentPost ?: return
        if (auth.currentUser?.uid != post.ownerId) { toast("작성자만 삭제할 수 있어요."); return }
        AlertDialog.Builder(this).setTitle("게시글을 삭제할까요?")
            .setMessage("삭제한 게시글은 되돌릴 수 없어요.")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ -> deletePost() }.show()
    }

    private fun deletePost() {
        val post = currentPost ?: return
        if (auth.currentUser?.uid != post.ownerId) return
        binding.btnDelete.isEnabled = false
        binding.btnEdit.isEnabled = false
        binding.btnSendComment.isEnabled = false
        binding.tvStatus.text = "삭제하는 중…"
        // 새 댓글이 들어오지 않게 한 뒤 댓글과 글을 순서대로 삭제한다.
        db.collection(CommunityFirestore.POSTS).document(postId).update("deleting", true)
            .addOnSuccessListener {
                if (!isFinishing && !isDestroyed) {
                    currentPost?.deleting = true
                    binding.etComment.isEnabled = false
                    deleteCommentBatch()
                }
            }
            .addOnFailureListener { error -> showDeleteError(error) }
    }

    private fun deleteCommentBatch() {
        val postRef = db.collection(CommunityFirestore.POSTS).document(postId)
        // 댓글이 많아도 한 번의 batch 크기를 넘지 않게 나누어 삭제
        postRef.collection(CommunityFirestore.COMMENTS).limit(400).get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                if (snapshot.isEmpty) {
                    postRef.delete()
                        .addOnSuccessListener {
                            if (!isFinishing && !isDestroyed) {
                                postAvailable = false
                                toast("게시글과 댓글을 삭제했어요.")
                                finish()
                            }
                        }
                        .addOnFailureListener { error -> showDeleteError(error) }
                } else {
                    val batch = db.batch()
                    for (document in snapshot) batch.delete(document.reference)
                    batch.commit()
                        .addOnSuccessListener { if (!isFinishing && !isDestroyed) deleteCommentBatch() }
                        .addOnFailureListener { error -> showDeleteError(error) }
                }
            }
            .addOnFailureListener { error -> showDeleteError(error) }
    }

    private fun showDeleteError(error: Exception) {
        if (isFinishing || isDestroyed) return
        binding.btnDelete.isEnabled = true
        binding.btnEdit.isEnabled = currentPost?.deleting == false
        binding.etComment.isEnabled = currentPost?.deleting == false
        binding.btnSendComment.isEnabled = currentPost?.deleting == false
        binding.tvStatus.text = CommunityFirestore.errorMessage(error) + " 삭제를 다시 눌러 시도할 수 있어요."
    }

    private fun loadComments() {
        commentLoadNumber++
        val requestNumber = commentLoadNumber
        binding.btnRefreshComments.isEnabled = false
        binding.tvCommentStatus.text = "댓글을 불러오는 중…"
        db.collection(CommunityFirestore.POSTS).document(postId)
            .collection(CommunityFirestore.COMMENTS)
            .orderBy("createdAt", Query.Direction.ASCENDING).get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                if (isFinishing || isDestroyed || !postAvailable || requestNumber != commentLoadNumber) return@addOnSuccessListener
                try {
                    val list = mutableListOf<CommunityComment>()
                    for (document in snapshot) {
                        val comment = document.toObject(CommunityComment::class.java)
                        comment.id = document.id
                        list.add(comment)
                    }
                    commentAdapter.submitList(list)
                    binding.tvCommentCount.text = "댓글 ${list.size}"
                    binding.tvCommentStatus.text = if (list.isEmpty()) "첫 댓글을 남겨 보세요." else ""
                    binding.btnRefreshComments.isEnabled = true
                } catch (error: Exception) { showCommentError(error) }
            }
            .addOnFailureListener { error ->
                if (!isFinishing && !isDestroyed && requestNumber == commentLoadNumber) showCommentError(error)
            }
    }

    private fun showCommentError(error: Exception) {
        binding.btnRefreshComments.isEnabled = true
        binding.tvCommentStatus.text = CommunityFirestore.errorMessage(error)
    }

    private fun saveComment() {
        if (!postAvailable || currentPost?.deleting == true || !binding.btnSendComment.isEnabled) return
        val user = auth.currentUser
        if (user == null || !user.isEmailVerified) { toast("이메일 인증 후 로그인해 주세요."); return }
        val text = binding.etComment.text.toString().trim()
        if (text.isBlank()) { binding.etComment.error = "댓글을 입력해 주세요."; return }
        val colref = db.collection(CommunityFirestore.POSTS).document(postId).collection(CommunityFirestore.COMMENTS)
        if (pendingCommentId.isBlank()) pendingCommentId = colref.document().id
        val name = user.displayName
        val comment = CommunityComment(
            id = pendingCommentId, postId = postId, ownerId = user.uid,
            authorName = if (name.isNullOrBlank()) "마루 이용자" else name,
            content = text, createdAt = System.currentTimeMillis()
        )
        binding.btnSendComment.isEnabled = false
        binding.etComment.isEnabled = false
        binding.tvCommentStatus.text = "댓글을 저장하는 중…"
        colref.document(comment.id).set(comment)
            .addOnSuccessListener {
                if (!isFinishing && !isDestroyed && postAvailable) {
                    pendingCommentId = ""
                    binding.etComment.text.clear()
                    binding.etComment.isEnabled = true
                    binding.btnSendComment.isEnabled = true
                    loadComments()
                }
            }
            .addOnFailureListener { error ->
                if (!isFinishing && !isDestroyed) {
                    binding.etComment.isEnabled = true
                    binding.btnSendComment.isEnabled = postAvailable
                    showCommentError(error)
                }
            }
    }

    private fun confirmCommentDelete(comment: CommunityComment) {
        if (!postAvailable || auth.currentUser?.uid != comment.ownerId) return
        AlertDialog.Builder(this).setTitle("댓글을 삭제할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                db.collection(CommunityFirestore.POSTS).document(postId)
                    .collection(CommunityFirestore.COMMENTS).document(comment.id).delete()
                    .addOnSuccessListener { if (!isFinishing && !isDestroyed && postAvailable) loadComments() }
                    .addOnFailureListener { error ->
                        if (!isFinishing && !isDestroyed) showCommentError(error)
                    }
            }.show()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
