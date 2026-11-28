package com.project.maru

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.MyApplication.Companion.db
import com.google.firebase.firestore.Source
import com.project.maru.community.CommunityFirestore
import com.project.maru.community.CommunityPost
import com.project.maru.community.CommunityRepository
import com.project.maru.community.CommunityDetailActivity
import com.project.maru.community.CommunityPostAdapter
import com.project.maru.databinding.FragmentMypageBinding
import com.project.maru.community.MyCommentsAdapter
import com.project.maru.community.MyCommentsRepository

class MyPageFragment : Fragment(R.layout.fragment_mypage) {
    private var _binding: FragmentMypageBinding? = null
    private val binding get() = requireNotNull(_binding)
    private lateinit var adapter: CommunityPostAdapter
    private lateinit var commentsAdapter: MyCommentsAdapter
    private var selectedTab = TAB_POSTS
    private var loadNumber = 0
    private var profileRequest = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMypageBinding.bind(view)
        ScheduleUi.bindBellBadge(this, binding.viewBellDot)

        val user = auth.currentUser
        val currentUserId = user?.uid ?: ""
        binding.tvProfileName.text = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")
            ?: "마루맘"

        adapter = CommunityPostAdapter(currentUserId) { post ->
            startActivity(Intent(requireContext(), CommunityDetailActivity::class.java).apply {
                putExtra("postId", post.id)
            })
        }
        commentsAdapter = MyCommentsAdapter { item ->
            startActivity(Intent(requireContext(), CommunityDetailActivity::class.java)
                .putExtra("postId", item.comment.postId))
        }
        binding.recyclerMyPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMyPosts.adapter = adapter

        binding.tabMyPosts.setOnClickListener { selectTab(TAB_POSTS) }
        binding.tabSavedPosts.setOnClickListener {
            startActivity(Intent(requireContext(), com.project.maru.diary.DiaryHomeActivity::class.java))
        }
        binding.tabMyComments.setOnClickListener { selectTab(TAB_COMMENTS) }
        binding.btnNotification.setOnClickListener {
            ScheduleUi.showNotifications(requireActivity())
        }
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_myPage_to_myInfo)
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        selectTab(TAB_POSTS, false)
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && ::adapter.isInitialized) {
            loadProfileAvatar()
            if (selectedTab == TAB_COMMENTS) loadMyComments() else loadMyPosts()
        }
    }

    override fun onStop() {
        profileRequest++
        loadNumber++
        super.onStop()
    }

    override fun onDestroyView() {
        if (_binding != null) binding.recyclerMyPosts.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun loadProfileAvatar() {
        val current = _binding ?: return
        val request = ++profileRequest
        current.ivProfileAvatar.imageTintList = null
        current.ivProfileAvatar.setImageResource(R.drawable.ic_person)
        val user = auth.currentUser ?: return
        current.tvProfileName.text = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@") ?: "마루 이용자"
        db.collection("users").document(user.uid).get(Source.SERVER).addOnSuccessListener { document ->
            if (_binding !== current || request != profileRequest || auth.currentUser?.uid != user.uid) return@addOnSuccessListener
            current.ivProfileAvatar.setImageResource(ProfileAvatar.image(document.getString("gender")))
        }
    }

    private fun selectTab(tab: String, loadPosts: Boolean = true) {
        loadNumber++
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
            TAB_POSTS -> {
                binding.recyclerMyPosts.adapter = adapter
                if (loadPosts) loadMyPosts()
            }
            TAB_SAVED -> showEmpty(getString(R.string.mypage_empty_saved))
            TAB_COMMENTS -> {
                binding.recyclerMyPosts.adapter = commentsAdapter
                loadMyComments()
            }
        }
    }

    private fun loadMyPosts() {
        val currentBinding = _binding ?: return
        loadNumber++
        val requestNumber = loadNumber
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showEmpty("로그인 후 내 글을 확인해 주세요.")
            return
        }
        currentBinding.recyclerMyPosts.adapter = adapter
        adapter.submitList(emptyList())
        currentBinding.layoutMyPageEmpty.visibility = View.GONE
        currentBinding.tvMyPageStatus.text = "게시글을 불러오는 중…"

        // 조회한 문서를 게시글 모델로 변환한 뒤 어댑터를 갱신한다.
        db.collection(CommunityFirestore.POSTS)
            .whereEqualTo("ownerId", userId)
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                if (_binding !== currentBinding || selectedTab != TAB_POSTS ||
                    requestNumber != loadNumber || auth.currentUser?.uid != userId) return@addOnSuccessListener
                try {
                    val posts = mutableListOf<CommunityPost>()
                    for (document in snapshot) {
                        val post = CommunityRepository.readPost(document)
                        post.id = document.id
                        posts.add(post)
                    }
                    // 내 글만 받은 뒤 최신순 정렬: 별도의 복합 색인이 필요하지 않다.
                    posts.sortByDescending { it.createdAt }
                    adapter.submitList(posts)
                    currentBinding.layoutMyPageEmpty.visibility =
                        if (posts.isEmpty()) View.VISIBLE else View.GONE
                    currentBinding.tvMyPageEmpty.text = getString(R.string.mypage_empty_posts)
                    currentBinding.tvMyPageStatus.text =
                        if (posts.isEmpty()) "" else "내가 작성한 글 ${posts.size}개"
                } catch (error: Exception) {
                    showEmpty(CommunityFirestore.errorMessage(error))
                }
            }
            .addOnFailureListener { error ->
                if (_binding !== currentBinding || selectedTab != TAB_POSTS ||
                    requestNumber != loadNumber || auth.currentUser?.uid != userId) return@addOnFailureListener
                showEmpty(CommunityFirestore.errorMessage(error))
            }
    }

    private fun loadMyComments() {
        val current = _binding ?: return
        val request = ++loadNumber
        val uid = auth.currentUser?.uid
        current.recyclerMyPosts.adapter = commentsAdapter
        commentsAdapter.submitList(emptyList())
        if (uid == null) {
            showEmpty("로그인 후 내 댓글을 확인해 주세요.")
            return
        }
        current.layoutMyPageEmpty.visibility = View.GONE
        current.tvMyPageStatus.text = "댓글을 불러오는 중…"
        fun active() = _binding === current && selectedTab == TAB_COMMENTS &&
            request == loadNumber && auth.currentUser?.uid == uid
        MyCommentsRepository.load(uid, ::active, { items ->
            if (!active()) return@load
            commentsAdapter.submitList(items)
            current.layoutMyPageEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            current.tvMyPageEmpty.text = "아직 작성한 댓글이 없어요."
            current.tvMyPageStatus.text = if (items.isEmpty()) "" else "내가 작성한 댓글 ${items.size}개"
        }, { error ->
            if (active()) showEmpty(CommunityFirestore.errorMessage(error) + " 내 댓글을 눌러 다시 시도해 주세요.")
        })
    }

    private fun showEmpty(message: String) {
        adapter.submitList(emptyList())
        commentsAdapter.submitList(emptyList())
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
