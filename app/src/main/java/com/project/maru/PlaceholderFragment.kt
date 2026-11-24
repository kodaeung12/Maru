package com.project.maru

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

// 아직 내용이 없는 화면은 화면 지도에 지정한 제목으로 공통 화면을 표시한다.
class PlaceholderFragment : Fragment(R.layout.fragment_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        val title = navController.currentDestination?.label
        view.findViewById<TextView>(R.id.tvTopTitle).text = title
        view.findViewById<TextView>(R.id.tvPlaceholderTitle).text = title
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            navController.navigateUp()
        }
    }
}