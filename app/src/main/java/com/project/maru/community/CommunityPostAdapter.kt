package com.project.maru.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.maru.databinding.ItemCommunityPostBinding
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.project.maru.MyApplication.Companion.db
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CommunityPostViewHolder(
    private val binding: ItemCommunityPostBinding,
    private val currentUser: String,
    private val onItemClick: (CommunityPost) -> Unit,
    private val commentCounts: MutableMap<String, Long>
) : RecyclerView.ViewHolder(binding.root) {
    private var bindNumber = 0
    fun bind(item: CommunityPost) = with(binding) {
        val number = ++bindNumber
        CommunityImages.show(ivRepresentative, item.imageKey)
        tvCategory.text = CommunityPostFilters.category(item.category)
        tvChildAge.visibility = if (item.childAge >= 0) android.view.View.VISIBLE else android.view.View.GONE
        tvChildAge.text = "만 ${item.childAge}세 자녀"
        tvStats.text = item.likeCount.toString()
        tvViews.text = "조회 ${item.viewCount}"
        tvComments.text = commentCounts[item.id]?.toString() ?: "댓글"
        if (currentUser.isNotEmpty() && item.id !in commentCounts) {
            db.collection(CommunityFirestore.POSTS).document(item.id)
                .collection(CommunityFirestore.COMMENTS).count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener {
                    commentCounts[item.id] = it.count
                    if (number == bindNumber) tvComments.text = it.count.toString()
                }
        }
        tvTitle.text = item.title
        val name = if (item.authorName.isBlank()) "마루 이용자" else item.authorName
        val author = if (currentUser.isNotEmpty() && item.ownerId == currentUser) "$name · 내 글" else name
        tvOwner.text = "$author · ${CommunityPresentation.time(item.createdAt)}"
        tvDate.text = if (item.createdAt > 0L) {
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(item.createdAt))
        } else "작성 시간 없음"
        root.setOnClickListener { onItemClick(item) }
    }
}

class CommunityPostAdapter(
    private val currentUser: String,
    private val onItemClick: (CommunityPost) -> Unit
) : RecyclerView.Adapter<CommunityPostViewHolder>() {

    private var allItems: MutableList<CommunityPost> = mutableListOf()
    private var items: MutableList<CommunityPost> = mutableListOf()
    private var categoryFilter = "전체"
    private var loadNumber = 0
    private var registration: com.google.firebase.firestore.ListenerRegistration? = null
    private var search = ""
    private var popular = false
    private val commentCounts = mutableMapOf<String, Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityPostViewHolder {
        val binding = ItemCommunityPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommunityPostViewHolder(binding, currentUser, onItemClick, commentCounts)
    }

    override fun onBindViewHolder(holder: CommunityPostViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun submitList(newItems: List<CommunityPost>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun applyFilter(category: String): Int {
        categoryFilter = category
        val filtered = CommunityPostFilters.apply(allItems, categoryFilter, search, popular)
        submitList(filtered)
        return items.size
    }


    fun searchAndSort(query: String, byLikes: Boolean): Int {
        search = query
        popular = byLikes
        return applyFilter(categoryFilter)
    }

    // 서버 조회 -> 모델 변환 -> 어댑터 갱신
    fun loadItems(onSuccess: (Int) -> Unit, onFailure: (Exception) -> Unit) {
        commentCounts.clear()
        loadNumber++
        val requestNumber = loadNumber
        registration?.remove()
        registration = db.collection(CommunityFirestore.POSTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (requestNumber != loadNumber) return@addSnapshotListener
                if (error != null || snapshot == null) {
                    onFailure(error ?: IllegalStateException("게시글 응답이 없어요."))
                    return@addSnapshotListener
                }
                try {
                    val postList = mutableListOf<CommunityPost>()
                    for (document in snapshot) {
                        val post = CommunityRepository.readPost(document)
                        if (!post.deleting || post.ownerId == currentUser) {
                            postList.add(post)
                        }
                    }
                    allItems = postList
                    onSuccess(applyFilter(categoryFilter))
                } catch (failure: Exception) { onFailure(failure) }
            }
    }

    fun stopLoading() { loadNumber++; registration?.remove(); registration = null }
}
