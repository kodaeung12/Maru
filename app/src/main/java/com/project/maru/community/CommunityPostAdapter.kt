package com.project.maru.community

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.maru.databinding.ItemCommunityPostBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread


class CommunityPostViewHolder(
    private val binding: ItemCommunityPostBinding,
    private val currentUser: String,
    private val onItemClick: (CommunityPost) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: CommunityPost) = with(binding) {
        tvCategory.text = item.category
        tvTitle.text = item.title
        val name = if (item.authorName.isBlank()) "마루 이용자" else item.authorName
        tvOwner.text = if (currentUser.isNotEmpty() && item.ownerId == currentUser) "$name · 내 글" else name
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityPostViewHolder {
        val binding = ItemCommunityPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommunityPostViewHolder(binding, currentUser, onItemClick)
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
        val filtered = allItems.filter { item ->
            categoryFilter == "전체" || item.category == categoryFilter
        }
        submitList(filtered)
        return items.size
    }


    fun loadItems(context: Context, onSuccess: (Int) -> Unit, onFailure: () -> Unit) {
        thread {
            val helper = CommunityDatabaseHelper(context.applicationContext)
            try {
                val list = helper.getAllPosts()
                Handler(Looper.getMainLooper()).post {
                    allItems = list.toMutableList()
                    val count = applyFilter(categoryFilter)
                    onSuccess(count)
                }
            } catch (error: Exception) {
                Handler(Looper.getMainLooper()).post { onFailure() }
            } finally {
                helper.close()
            }
        }
    }
}
