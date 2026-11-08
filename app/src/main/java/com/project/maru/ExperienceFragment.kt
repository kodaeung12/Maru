package com.project.maru   // ← 내 프로젝트 패키지명과 다르면 이 줄만 바꿔주세요

// 통합 기능: 대표님의 행사 목록 Activity를 실행하기 위한 Intent
import android.content.Intent

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

// 1. 체험·행사 화면 (빈 화면)
class ExperienceFragment : Fragment(R.layout.fragment_experience) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 홈에서 문화·예술 등을 눌러서 왔다면 제목 옆에 표시 (예: 체험·행사 · 문화·예술)
        val category = arguments?.getString("category").orEmpty()
        val title = getString(R.string.title_experience)
        view.findViewById<TextView>(R.id.tvTopTitle).text = if (category.isEmpty()) title else "$title · $category"

        view.findViewById<View>(R.id.btnBack).setOnClickListener { findNavController().navigateUp() }

        // 통합 기능: 기존 체험·행사 화면에서 대표님의 행사 목록 화면으로 이동
        val intent = Intent(requireContext(), ExperienceListActivity::class.java)

        // 통합 기능: 홈에서 선택한 카테고리 정보를 행사 목록 화면에 전달
        intent.putExtra("category", category)

        // 통합 기능: 행사 목록 Activity 실행
        startActivity(intent)
    }
}
