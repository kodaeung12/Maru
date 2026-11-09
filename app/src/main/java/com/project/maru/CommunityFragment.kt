package com.project.maru

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.project.maru.community.CommunityDetailActivity
import com.project.maru.community.CommunityPostAdapter
import com.project.maru.community.CommunityWriteActivity
import com.project.maru.databinding.FragmentCommunityBinding

class CommunityFragment : Fragment(R.layout.fragment_community) {
    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = requireNotNull(_binding)
    private lateinit var adapter: CommunityPostAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCommunityBinding.bind(view)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "local"
        adapter = CommunityPostAdapter(currentUserId) { post ->
            startActivity(Intent(requireContext(), CommunityDetailActivity::class.java).apply {
                putExtra("postId", post.id)
            })
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnWrite.setOnClickListener {
            startActivity(Intent(requireContext(), CommunityWriteActivity::class.java))
        }
        binding.btnRefresh.setOnClickListener { loadPosts() }
        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "알림 기능은 준비 중이에요.", Toast.LENGTH_SHORT).show()
        }
        binding.categoryAll.setOnClickListener { selectCategory("전체") }
        binding.categoryParenting.setOnClickListener { selectCategory("육아정보") }
        binding.categoryDaily.setOnClickListener { selectCategory("일상공유") }
        binding.categoryConcern.setOnClickListener { selectCategory("고민상담") }
        binding.categoryLocal.setOnClickListener { selectCategory("지역정보") }

        selectCategory("전체")
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && ::adapter.isInitialized) loadPosts()
    }

    override fun onDestroyView() {
        _binding?.recyclerView?.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun selectCategory(category: String) {
        val tabs = listOf(
            binding.categoryAll,
            binding.categoryParenting,
            binding.categoryDaily,
            binding.categoryConcern,
            binding.categoryLocal
        )
        tabs.forEach { tab: TextView ->
            val selected = tab.text.toString() == category
            tab.setBackgroundResource(
                if (selected) R.drawable.community_chip_on else R.drawable.community_chip_off
            )
            tab.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) android.R.color.white else R.color.community_pink
                )
            )
        }
        showCount(adapter.applyFilter(category))
    }

    private fun loadPosts() {
        val currentBinding = _binding ?: return
        setButtonsEnabled(false)
        currentBinding.layoutEmpty.visibility = View.GONE
        currentBinding.tvStatus.text = "게시글을 불러오는 중…"
        adapter.loadItems(requireContext().applicationContext, { count ->
            if (_binding == null) return@loadItems
            setButtonsEnabled(true)
            showCount(count)
        }, {
            if (_binding == null) return@loadItems
            setButtonsEnabled(true)
            binding.tvStatus.text = "기기에 저장된 글을 불러오지 못했어요. 새로고침해 주세요."
        })
    }

    private fun showCount(count: Int) {
        val currentBinding = _binding ?: return
        currentBinding.layoutEmpty.visibility = if (count == 0) View.VISIBLE else View.GONE
        currentBinding.tvStatus.text = if (count == 0) {
            "기기에 저장된 게시글이 없어요. 첫 글을 작성해 보세요."
        } else {
            "게시글 ${count}개"
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        val currentBinding = _binding ?: return
        currentBinding.btnRefresh.isEnabled = enabled
        currentBinding.categoryAll.isEnabled = enabled
        currentBinding.categoryParenting.isEnabled = enabled
        currentBinding.categoryDaily.isEnabled = enabled
        currentBinding.categoryConcern.isEnabled = enabled
        currentBinding.categoryLocal.isEnabled = enabled
    }
}
