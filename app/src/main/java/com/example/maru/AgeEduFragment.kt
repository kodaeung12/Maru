package com.example.maru   // ← 내 프로젝트 패키지명과 다르면 이 줄만 바꿔주세요

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

// 2. 연령별 교육 화면 (빈 화면)
class AgeEduFragment : Fragment(R.layout.fragment_age_edu) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tvTopTitle).setText(R.string.title_age_edu)
        view.findViewById<View>(R.id.btnBack).setOnClickListener { findNavController().navigateUp() }
    }
}
