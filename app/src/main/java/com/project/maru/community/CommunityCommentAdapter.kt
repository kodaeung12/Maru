package com.project.maru.community

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.maru.databinding.ItemCommunityCommentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommunityCommentViewHolder(
    private val binding: ItemCommunityCommentBinding,
    private val currentUser: String,
    private val onDelete: (CommunityComment) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: CommunityComment) = with(binding) {
        tvAuthor.text = if (item.authorName.isBlank()) "마루 이용자" else item.authorName
        tvContent.text = item.content
        tvDate.text = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(item.createdAt))
        btnDelete.visibility = if (currentUser.isNotBlank() && item.ownerId == currentUser) View.VISIBLE else View.GONE
        btnDelete.setOnClickListener { onDelete(item) }
    }
}

class CommunityCommentAdapter(
    private val currentUser: String,
    private val onDelete: (CommunityComment) -> Unit
) : RecyclerView.Adapter<CommunityCommentViewHolder>() {
    private var items: MutableList<CommunityComment> = mutableListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityCommentViewHolder {
        val binding = ItemCommunityCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommunityCommentViewHolder(binding, currentUser, onDelete)
    }
    override fun onBindViewHolder(holder: CommunityCommentViewHolder, position: Int) { holder.bind(items[position]) }
    override fun getItemCount() = items.size
    fun submitList(list: List<CommunityComment>) {
        items = list.toMutableList()
        notifyDataSetChanged()
    }
}
