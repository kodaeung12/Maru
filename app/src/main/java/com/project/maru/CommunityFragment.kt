package com.project.maru   // ← 내 프로젝트 패키지명과 다르면 이 줄만 바꿔주세요

import android.os.Bundle
import android.content.Intent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.maru.community.CommunityActivity

// 5. 커뮤니티 화면 (빈 화면)
class CommunityFragment : Fragment(R.layout.fragment_community) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startActivity(Intent(requireContext(), CommunityActivity::class.java))
        findNavController().popBackStack()
    }
}
