package com.project.maru   // ← 내 프로젝트 패키지명과 다르면 이 줄만 바꿔주세요

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

// 6. 마이페이지 화면 (빈 화면)
class MyPageFragment : Fragment(R.layout.fragment_mypage) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tvTopTitle).setText(R.string.title_mypage)
        view.findViewById<View>(R.id.btnBack).setOnClickListener { findNavController().navigateUp() }
        view.findViewById<View>(R.id.buttonLogoutFromHome).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }
}
