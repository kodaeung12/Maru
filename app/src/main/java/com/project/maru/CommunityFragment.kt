package com.project.maru

import android.content.Intent
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import com.project.maru.community.CommunityDatabaseHelper
import com.project.maru.community.CommunityPost
import com.project.maru.community.CommunityRepository
import kotlin.concurrent.thread
import java.util.UUID
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
        binding.buttonUploadLocalPosts.setOnClickListener { offerLocalPostUpload() }
        binding.btnNotification.setOnClickListener {
            ScheduleUi.showNotifications(requireActivity())
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
        adapter.loadItems({ count ->
            if (_binding !== currentBinding) return@loadItems
            setButtonsEnabled(true)
            showCount(count)
        }, {
            if (_binding !== currentBinding) return@loadItems
            setButtonsEnabled(true)
            binding.tvStatus.text = "온라인 게시글을 불러오지 못했어요. 인터넷 연결과 로그인 상태를 확인해 주세요."
        })
    }

    private fun showCount(count: Int) {
        val currentBinding = _binding ?: return
        currentBinding.layoutEmpty.visibility = if (count == 0) View.VISIBLE else View.GONE
        currentBinding.tvStatus.text = if (count == 0) {
            "등록된 게시글이 없어요. 첫 글을 작성해 보세요."
        } else {
            "게시글 ${count}개"
        }
    }

    private fun offerLocalPostUpload() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val currentBinding = _binding ?: return
        val appContext = requireContext().applicationContext
        currentBinding.buttonUploadLocalPosts.isEnabled = false
        thread {
            val result = runCatching {
                CommunityDatabaseHelper(appContext).use { helper ->
                    helper.getAllPosts().filter { it.ownerId == user.uid }
                }
            }
            Handler(Looper.getMainLooper()).post {
                if (_binding !== currentBinding) return@post
                currentBinding.buttonUploadLocalPosts.isEnabled = true
                if (result.isFailure) {
                    Toast.makeText(context, "기기 글을 읽지 못했어요.", Toast.LENGTH_SHORT).show()
                    return@post
                }
                val posts = result.getOrThrow()
                if (posts.isEmpty()) {
                    Toast.makeText(context, "현재 계정으로 작성한 기기 글이 없어요.", Toast.LENGTH_SHORT).show()
                    return@post
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("내 기기 글 온라인으로 올리기")
                    .setMessage("현재 계정으로 작성한 글 ${posts.size}개를 커뮤니티 이용자에게 공유합니다. 기기 원본은 보관됩니다.")
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton("올리기") { _, _ ->
                        val prefs = appContext.getSharedPreferences("community_migration", 0)
                        val device = prefs.getString("device", null) ?: UUID.randomUUID().toString().also {
                            prefs.edit().putString("device", it).commit()
                        }
                        uploadLocalPosts(posts, device, 0)
                    }.show()
            }
        }
    }

    private fun uploadLocalPosts(posts: List<CommunityPost>, device: String, index: Int) {
        val currentBinding = _binding ?: return
        if (index == posts.size) {
            currentBinding.buttonUploadLocalPosts.isEnabled = true
            loadPosts()
            Toast.makeText(context, "기기 글을 온라인으로 올렸어요.", Toast.LENGTH_SHORT).show()
            return
        }
        currentBinding.buttonUploadLocalPosts.isEnabled = false
        currentBinding.tvStatus.text = "기기 글 올리는 중 ${index + 1}/${posts.size}"
        val post = posts[index]
        val uploadId = UUID.nameUUIDFromBytes("$device/${post.ownerId}/${post.id}".toByteArray()).toString()
        if (post.title.length !in 1..50 || post.content.length !in 1..5000) {
            currentBinding.buttonUploadLocalPosts.isEnabled = true
            currentBinding.tvStatus.text = "${index}개 완료. 다음 글은 제목 50자·내용 5000자 제한을 확인해 주세요. 원본은 기기에 남아 있어요."
            return
        }
        CommunityRepository.createPost(post.copy(id = uploadId)).addOnCompleteListener { task ->
            if (_binding !== currentBinding) return@addOnCompleteListener
            if (task.isSuccessful) uploadLocalPosts(posts, device, index + 1)
            else {
                currentBinding.buttonUploadLocalPosts.isEnabled = true
                currentBinding.tvStatus.text = "${index}개 완료. 다음 글은 올리지 못했어요. 욕설 포함 여부와 연결 상태를 확인해 주세요. 원본은 기기에 남아 있어요."
            }
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
