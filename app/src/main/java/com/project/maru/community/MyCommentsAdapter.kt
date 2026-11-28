package com.project.maru.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.maru.databinding.ItemMyCommentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyCommentViewHolder(private val binding: ItemMyCommentBinding,
                          private val onClick: (MyCommentItem) -> Unit) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: MyCommentItem) = with(binding) {
        textPostTitle.text = item.postTitle.ifBlank { "게시글 보기" }
        textComment.text = item.comment.content
        textDate.text = item.category + " · " + SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
            .format(Date(item.comment.createdAt))
        root.setOnClickListener { onClick(item) }
    }
}

class MyCommentsAdapter(private val onClick: (MyCommentItem) -> Unit) : RecyclerView.Adapter<MyCommentViewHolder>() {
    private var items: List<MyCommentItem> = emptyList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyCommentViewHolder {
        val binding = ItemMyCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyCommentViewHolder(binding, onClick)
    }
    override fun onBindViewHolder(holder: MyCommentViewHolder, position: Int) { holder.bind(items[position]) }
    override fun getItemCount() = items.size
    fun submitList(list: List<MyCommentItem>) {
        items = list.toList()
        notifyDataSetChanged()
    }
}
