package com.project.maru

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.project.maru.community.CommunityDatabaseHelper
import com.project.maru.community.CommunityDetailActivity
import com.project.maru.community.CommunityPostAdapter
import com.project.maru.databinding.FragmentMypageBinding
import kotlin.concurrent.thread

class MyPageFragment : Fragment(R.layout.fragment_mypage) {
    private var _binding: FragmentMypageBinding? = null
    private val binding get() = requireNotNull(_binding)
    private lateinit var adapter: CommunityPostAdapter
    private var selectedTab = TAB_POSTS

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMypageBinding.bind(view)

        val user = FirebaseAuth.getInstance().currentUser
        val currentUserId = user?.uid ?: "local"
        binding.tvProfileName.text = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")
            ?: "마루맘"

        adapter = CommunityPostAdapter(currentUserId) { post ->
            startActivity(Intent(requireContext(), CommunityDetailActivity::class.java).apply {
                putExtra("postId", post.id)
            })
        }
        binding.recyclerMyPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMyPosts.adapter = adapter

        binding.tabMyPosts.setOnClickListener { selectTab(TAB_POSTS) }
        binding.tabSavedPosts.setOnClickListener { selectTab(TAB_SAVED) }
        binding.tabMyComments.setOnClickListener { selectTab(TAB_COMMENTS) }
        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "알림 기능은 준비 중이에요.", Toast.LENGTH_SHORT).show()
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_myPage_to_myInfo)
        }
        binding.buttonLogoutFromHome.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        selectTab(TAB_POSTS)
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && ::adapter.isInitialized && selectedTab == TAB_POSTS) {
            loadMyPosts()
        }
    }

    override fun onDestroyView() {
        if (_binding != null) binding.recyclerMyPosts.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun selectTab(tab: String) {
        selectedTab = tab
        val tabs = listOf(
            binding.tabMyPosts to TAB_POSTS,
            binding.tabSavedPosts to TAB_SAVED,
            binding.tabMyComments to TAB_COMMENTS
        )
        tabs.forEach { (view, tabName) ->
            val selected = tabName == tab
            view.setBackgroundResource(
                if (selected) R.drawable.community_chip_on else R.drawable.community_chip_off
            )
            view.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) android.R.color.white else R.color.community_pink
                )
            )
        }

        when (tab) {
            TAB_POSTS -> loadMyPosts()
            TAB_SAVED -> showEmpty(getString(R.string.mypage_empty_saved))
            TAB_COMMENTS -> showEmpty(getString(R.string.mypage_empty_comments))
        }
    }

    private fun loadMyPosts() {
        val context = context?.applicationContext ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "local"
        binding.layoutMyPageEmpty.visibility = View.GONE
        binding.tvMyPageStatus.text = "게시글을 불러오는 중…"

        thread {
            val helper = CommunityDatabaseHelper(context)
            val result = runCatching {
                helper.getAllPosts().filter { post ->
                    post.ownerId == userId || post.ownerId == "local"
                }
            }
            helper.close()

            Handler(Looper.getMainLooper()).post {
                val currentBinding = _binding ?: return@post
                if (selectedTab != TAB_POSTS) return@post
                result.onSuccess { posts ->
                    adapter.submitList(posts)
                    currentBinding.layoutMyPageEmpty.visibility =
                        if (posts.isEmpty()) View.VISIBLE else View.GONE
                    currentBinding.tvMyPageEmpty.text = getString(R.string.mypage_empty_posts)
                    currentBinding.tvMyPageStatus.text =
                        if (posts.isEmpty()) "" else "내가 작성한 글 ${posts.size}개"
                }.onFailure {
                    adapter.submitList(emptyList())
                    currentBinding.layoutMyPageEmpty.visibility = View.VISIBLE
                    currentBinding.tvMyPageEmpty.text = "게시글을 불러오지 못했어요."
                    currentBinding.tvMyPageStatus.text = ""
                }
            }
        }
    }

    private fun showEmpty(message: String) {
        adapter.submitList(emptyList())
        binding.layoutMyPageEmpty.visibility = View.VISIBLE
        binding.tvMyPageEmpty.text = message
        binding.tvMyPageStatus.text = ""
    }

    companion object {
        private const val TAB_POSTS = "posts"
        private const val TAB_SAVED = "saved"
        private const val TAB_COMMENTS = "comments"
    }
}
