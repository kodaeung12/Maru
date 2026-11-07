package com.project.maru   // ← 내 프로젝트 패키지명과 다르면 이 줄만 바꿔주세요

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

// 4. 검색 화면 (빈 화면)
class SearchFragment : Fragment(R.layout.fragment_search) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tvTopTitle).setText(R.string.title_search)
        view.findViewById<View>(R.id.btnBack).setOnClickListener { findNavController().navigateUp() }
    }
}
