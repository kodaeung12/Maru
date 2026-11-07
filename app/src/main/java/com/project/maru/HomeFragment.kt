package com.project.maru   // ← 내 프로젝트 패키지명과 다르면 이 줄만 바꿔주세요

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nav = findNavController()

        // ---------- 이름 / 나이 넣기 (res/values/strings.xml 의 user_name, child_age) ----------
        val userName = getString(R.string.user_name)
        val age = resources.getInteger(R.integer.child_age)

        view.findViewById<TextView>(R.id.tvGreeting).text =
            HtmlCompat.fromHtml(getString(R.string.home_greeting, userName), HtmlCompat.FROM_HTML_MODE_LEGACY)
        view.findViewById<TextView>(R.id.tvRecommendBadge).text = getString(R.string.recommend_badge, userName, age)
        view.findViewById<TextView>(R.id.tvRecommendTitle).text = getString(R.string.recommend_title, age)
        view.findViewById<TextView>(R.id.tvAgeSectionTitle).text =
            HtmlCompat.fromHtml(getString(R.string.section_age, userName, age), HtmlCompat.FROM_HTML_MODE_LEGACY)

        // ---------- 알림 버튼: 누를 때마다 ON/OFF ----------
        val btnBell = view.findViewById<ImageButton>(R.id.btnBell)
        val bellDot = view.findViewById<View>(R.id.viewBellDot)
        btnBell.isSelected = AppState.notificationOn
        bellDot.visibility = if (AppState.notificationOn) View.VISIBLE else View.GONE

        btnBell.setOnClickListener {
            AppState.notificationOn = !AppState.notificationOn
            btnBell.isSelected = AppState.notificationOn
            bellDot.visibility = if (AppState.notificationOn) View.VISIBLE else View.GONE
            val msg = if (AppState.notificationOn) R.string.notification_on else R.string.notification_off
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // ---------- 하트(즐겨찾기) → 즐겨찾기 화면 ----------
        view.findViewById<View>(R.id.btnFavorite).setOnClickListener {
            nav.navigate(R.id.action_home_to_favorites)
        }

        // ---------- 검색바 → 검색 화면 ----------
        view.findViewById<View>(R.id.searchBar).setOnClickListener {
            nav.navigate(R.id.action_home_to_search)
        }

        // ---------- 카테고리 버튼 6개 ----------
        view.findViewById<View>(R.id.catExperience).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience)
        }
        view.findViewById<View>(R.id.catAgeEdu).setOnClickListener {
            nav.navigate(R.id.action_home_to_ageEdu)
        }
        // 문화·예술 / 자연·생태 / 축제·행사 / 가족 나들이 → 체험·행사 화면 (어떤 카테고리인지 같이 전달)
        view.findViewById<View>(R.id.catCulture).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience, bundleOf("category" to getString(R.string.cat_culture)))
        }
        view.findViewById<View>(R.id.catNature).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience, bundleOf("category" to getString(R.string.cat_nature)))
        }
        view.findViewById<View>(R.id.catFestival).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience, bundleOf("category" to getString(R.string.cat_festival)))
        }
        view.findViewById<View>(R.id.catFamily).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience, bundleOf("category" to getString(R.string.cat_family)))
        }

        // ---------- 연령 추천 배너 (>) → 연령별 교육 ----------
        view.findViewById<View>(R.id.bannerRecommend).setOnClickListener { nav.navigate(R.id.action_home_to_ageEdu) }
        view.findViewById<View>(R.id.btnRecommendArrow).setOnClickListener { nav.navigate(R.id.action_home_to_ageEdu) }

        // ---------- 추천 체험 "전체보기 >" → 체험·행사 ----------
        view.findViewById<View>(R.id.tvMoreExperience).setOnClickListener {
            nav.navigate(R.id.action_home_to_experience)
        }

        // ---------- 카드 하트: 누르면 채워지고, 다시 누르면 비워짐 ----------
        val heartIds = listOf(R.id.btnHeart1, R.id.btnHeart2, R.id.btnHeart3)
        heartIds.forEachIndexed { index, id ->
            val heart = view.findViewById<ImageButton>(id)
            heart.isSelected = AppState.likedCards[index]   // selected=true 이면 채워진 하트 (ic_heart_selector.xml)
            heart.setOnClickListener {
                AppState.likedCards[index] = !AppState.likedCards[index]
                heart.isSelected = AppState.likedCards[index]
            }
        }

        // ---------- 연령별 교육 "전체보기 >" 와 카드들 → 연령별 교육 ----------
        listOf(R.id.tvMoreAge, R.id.age6, R.id.age7, R.id.age8, R.id.ageGuide).forEach { id ->
            view.findViewById<View>(id).setOnClickListener { nav.navigate(R.id.action_home_to_ageEdu) }
        }

        // ---------- 준비물과 안전수칙 (>) → 준비물과 안전수칙 ----------
        view.findViewById<View>(R.id.bannerSafety).setOnClickListener { nav.navigate(R.id.action_home_to_safety) }
        view.findViewById<View>(R.id.btnSafetyArrow).setOnClickListener { nav.navigate(R.id.action_home_to_safety) }
    }
}
