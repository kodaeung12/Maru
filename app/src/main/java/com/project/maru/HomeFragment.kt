package com.project.maru

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.maru.experience.list.ExperienceCategory
import com.project.maru.experience.recommendation.bindHomeExperienceRecommendations

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nav = findNavController()

        val userName = getString(R.string.user_name)
        val age = resources.getInteger(R.integer.child_age)

        view.findViewById<TextView>(R.id.tvGreeting).text =
            HtmlCompat.fromHtml(getString(R.string.home_greeting, userName), HtmlCompat.FROM_HTML_MODE_LEGACY)
        view.findViewById<TextView>(R.id.tvRecommendBadge).text = getString(R.string.recommend_badge, userName, age)
        view.findViewById<TextView>(R.id.tvRecommendTitle).text = getString(R.string.recommend_title, age)
        view.findViewById<TextView>(R.id.tvAgeSectionTitle).text =
            HtmlCompat.fromHtml(getString(R.string.section_age, userName, age), HtmlCompat.FROM_HTML_MODE_LEGACY)

        ScheduleUi.bindHome(this, view)

        view.findViewById<View>(R.id.btnFavorite).setOnClickListener {
            nav.navigate(R.id.action_home_to_favorites)
        }

        HomeSearch.bind(this, view)

        view.findViewById<View>(R.id.catCulture).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience, bundleOf("category" to ExperienceCategory.CULTURE.name))
        }
        view.findViewById<View>(R.id.catNature).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience, bundleOf("category" to ExperienceCategory.NATURE.name))
        }
        view.findViewById<View>(R.id.catFestival).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience, bundleOf("category" to ExperienceCategory.FESTIVAL.name))
        }
        view.findViewById<View>(R.id.catFamily).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience, bundleOf("category" to ExperienceCategory.EDUCATION.name))
        }

        view.findViewById<View>(R.id.catExperience).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience, bundleOf("category" to ExperienceCategory.EXPERIENCE.name))
        }

        val defaultAgeCategory = resources.getInteger(R.integer.child_age).toString()
        view.findViewById<View>(R.id.bannerRecommend).setOnClickListener {
            nav.navigate(R.id.action_home_to_ageEdu, bundleOf("category" to defaultAgeCategory))
        }
        view.findViewById<View>(R.id.btnRecommendArrow).setOnClickListener {
            nav.navigate(R.id.action_home_to_ageEdu, bundleOf("category" to defaultAgeCategory))
        }

        view.findViewById<View>(R.id.tvMoreExperience).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience)
        }

        listOf(R.id.btnHeart1, R.id.btnHeart2, R.id.btnHeart3).forEach { id ->
            view.findViewById<ImageButton>(id).setOnClickListener {
                Toast.makeText(requireContext(), R.string.favorite_choose_event, Toast.LENGTH_SHORT).show()
                nav.navigate(R.id.action_home_to_experience)
            }
        }

        bindHomeExperienceRecommendations(this, view)

        view.findViewById<View>(R.id.tvMoreAge).setOnClickListener {
            nav.navigate(R.id.action_home_to_ageEdu, bundleOf("category" to defaultAgeCategory))
        }
        listOf(R.id.age6 to "6", R.id.age7 to "7", R.id.age8 to "8", R.id.ageGuide to "parent").forEach { (id, category) ->
            view.findViewById<View>(id).setOnClickListener {
                nav.navigate(R.id.action_home_to_ageEdu, bundleOf("category" to category))
            }
        }

        bindCommunityPreview(view)
    }

    private fun bindCommunityPreview(view: View) {
        val rows = view.findViewById<android.widget.LinearLayout>(R.id.layoutHomeCommunityPosts)
        val status = view.findViewById<TextView>(R.id.textHomeCommunityStatus)
        val retry = view.findViewById<View>(R.id.buttonRetryHomeCommunity)
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        var requestNumber = 0
        fun load() {
            val request = ++requestNumber
            rows.removeAllViews()
            retry.visibility = View.GONE
            status.visibility = View.VISIBLE
            val uid = auth.currentUser?.uid
            if (uid == null) {
                status.setText(R.string.home_community_signin)
                return
            }
            status.setText(R.string.home_community_loading)
            com.project.maru.community.CommunityRepository.loadLatestPosts().addOnCompleteListener { task ->
                if (request != requestNumber || this.view !== view || auth.currentUser?.uid != uid) return@addOnCompleteListener
                if (!task.isSuccessful) {
                    status.setText(R.string.home_community_failed)
                    retry.visibility = View.VISIBLE
                    return@addOnCompleteListener
                }
                val posts = task.result
                status.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                status.setText(R.string.home_community_empty)
                val dateFormat = java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.KOREA)
                posts.forEach { post ->
                    val row = layoutInflater.inflate(R.layout.item_home_community_post, rows, false)
                    row.findViewById<TextView>(R.id.textHomePostCategory).text = post.category
                    row.findViewById<TextView>(R.id.textHomePostTitle).text = post.title
                    row.findViewById<TextView>(R.id.textHomePostTime).text = dateFormat.format(java.util.Date(post.createdAt))
                    row.setOnClickListener {
                        startActivity(android.content.Intent(requireContext(), com.project.maru.community.CommunityDetailActivity::class.java)
                            .putExtra("postId", post.id))
                    }
                    rows.addView(row)
                }
            }
        }
        retry.setOnClickListener { load() }
        view.findViewById<View>(R.id.buttonMoreHomeCommunity).setOnClickListener {
            findNavController().navigate(R.id.communityFragment)
        }
        val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { load() }
        viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) { auth.addAuthStateListener(authListener) }
            override fun onPause(owner: androidx.lifecycle.LifecycleOwner) {
                auth.removeAuthStateListener(authListener)
                requestNumber++
            }
        })
    }
}
