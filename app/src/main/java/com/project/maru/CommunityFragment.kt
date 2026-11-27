package com.project.maru

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.firestore.ListenerRegistration
import com.project.maru.MyApplication.Companion.db
import com.project.maru.community.CommunitySocial
import com.project.maru.community.CommunityNotificationsActivity
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.community.CommunityDetailActivity
import com.project.maru.community.CommunityFirestore
import com.project.maru.community.CommunityPostAdapter
import com.project.maru.community.CommunityWriteActivity
import com.project.maru.databinding.FragmentCommunityBinding

class CommunityFragment : Fragment(R.layout.fragment_community) {
    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = requireNotNull(_binding)
    private lateinit var adapter: CommunityPostAdapter
    private var category = "전체"
    private var notifications: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCommunityBinding.bind(view)
        ScheduleUi.bindBellBadge(this, binding.viewBellDot)

        val currentUserId = auth.currentUser?.uid ?: ""
        adapter = CommunityPostAdapter(currentUserId) { post ->
            startActivity(Intent(requireContext(), CommunityDetailActivity::class.java).apply {
                putExtra("postId", post.id)
            })
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnWrite.setOnClickListener {
            if (auth.currentUser == null) {
                Toast.makeText(requireContext(), "로그인 후 글을 작성해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(requireContext(), CommunityWriteActivity::class.java))
        }
        binding.btnRefresh.setOnClickListener { loadPosts() }
        binding.btnNotification.setOnClickListener {
            startActivity(Intent(requireContext(), CommunityNotificationsActivity::class.java))
        }
        binding.categoryAll.setOnClickListener { selectCategory("전체") }
        binding.categoryParenting.setOnClickListener { selectCategory("육아정보") }
        binding.categoryDaily.setOnClickListener { selectCategory("일상공유") }
        binding.categoryConcern.setOnClickListener { selectCategory("고민상담") }
        binding.categoryLocal.setOnClickListener { selectCategory("꿀팁정보") }

        val prefs = requireContext().getSharedPreferences("community_ui", 0)
        binding.introBanner.visibility = if (prefs.getBoolean("intro_closed", false)) View.GONE else View.VISIBLE
        binding.btnCloseBanner.setOnClickListener {
            prefs.edit().putBoolean("intro_closed", true).apply()
            binding.introBanner.visibility = View.GONE
        }
        binding.spinnerSort.adapter = ArrayAdapter(requireContext(), R.layout.community_spinner_item, listOf("최신순", "공감순")).apply {
            setDropDownViewResource(R.layout.community_spinner_dropdown)
        }
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, id: Long) { applySearch() }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        binding.etSearch.doAfterTextChanged { applySearch() }
        binding.btnShowAll.setOnClickListener {
            binding.etSearch.text.clear()
            binding.etSearch.clearFocus()
            (requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
            binding.spinnerSort.setSelection(0)
            selectCategory("전체")
        }
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
        this.category = category
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
        adapter.applyFilter(category)
        applySearch()
    }

    private fun loadPosts() {
        val currentBinding = _binding ?: return
        if (auth.currentUser == null) {
            currentBinding.tvStatus.text = "로그인 후 커뮤니티를 이용해 주세요."
            currentBinding.layoutEmpty.visibility = View.GONE
            adapter.submitList(emptyList())
            return
        }
        setButtonsEnabled(false)
        currentBinding.layoutEmpty.visibility = View.GONE
        currentBinding.tvStatus.text = "게시글을 불러오는 중…"
        adapter.loadItems({ count ->
            if (_binding !== currentBinding) return@loadItems
            setButtonsEnabled(true)
            showCount(count)
        }, { error ->
            if (_binding !== currentBinding) return@loadItems
            setButtonsEnabled(true)
            currentBinding.tvStatus.text = CommunityFirestore.errorMessage(error)
        })
    }

    private fun showCount(count: Int) {
        val currentBinding = _binding ?: return
        currentBinding.layoutEmpty.visibility = if (count == 0) View.VISIBLE else View.GONE
        val filtered = currentBinding.etSearch.text.isNotBlank() || category != "전체"
        currentBinding.tvEmptyTitle.text = if (filtered) "검색 결과가 없어요" else "아직 게시글이 없어요"
        currentBinding.tvEmptyMessage.text = if (filtered) "다른 검색어로 다시 검색해 보세요." else "첫 번째 이야기를 작성해 보세요!"
        currentBinding.btnShowAll.visibility = if (filtered) View.VISIBLE else View.GONE
        currentBinding.tvStatus.text = if (count == 0) {
            ""
        } else {
            "게시글 ${count}개"
        }
    }

    private fun applySearch() {
        val current = _binding ?: return
        showCount(adapter.searchAndSort(current.etSearch.text.toString(), current.spinnerSort.selectedItemPosition == 1))
    }

    override fun onStart() {
        super.onStart()
        val current = _binding ?: return
        val uid = auth.currentUser?.uid ?: return
        notifications = db.collection(CommunitySocial.NOTIFICATIONS).whereEqualTo("recipientId", uid)
            .addSnapshotListener { snapshot, error ->
                if (_binding !== current) return@addSnapshotListener
                if (error == null && snapshot != null) {
                    val count = snapshot.documents.count { it.getBoolean("read") != true }
                    current.tvUnread.visibility = if (count > 0) View.VISIBLE else View.GONE
                    current.tvUnread.text = if (count > 99) "99+" else count.toString()
                    current.btnNotification.contentDescription = "커뮤니티 알림, 읽지 않은 알림 ${count}개"
                }
            }
    }

    override fun onStop() {
        notifications?.remove(); notifications = null
        adapter.stopLoading()
        super.onStop()
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
