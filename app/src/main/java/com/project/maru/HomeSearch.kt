package com.project.maru

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

internal object HomeSearch {
    fun bind(fragment: Fragment, view: View) {
        val context=fragment.requireContext()
        val input=view.findViewById<EditText>(R.id.editHomeSearch)
        val panel=view.findViewById<View>(R.id.panelPopularSearches)
        val rows=view.findViewById<LinearLayout>(R.id.layoutPopularSearches)
        val status=view.findViewById<TextView>(R.id.textPopularSearchStatus)
        var registration: ListenerRegistration?=null
        var authListener: FirebaseAuth.AuthStateListener?=null
        var ranking: List<RankedSearch> = emptyList()
        var busy=false
        fun hideKeyboard() { (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken,0) }
        fun submit(raw: String) {
            if(busy) return
            val query=normalizeSearchTerm(raw)
            if(query.isBlank()) { input.error="검색어를 입력해 주세요."; return }
            if(query.length > 50) { input.error="검색어는 50자까지 입력해 주세요."; return }
            if(ContentPolicy.containsAbuse(query)) { input.error=ContentPolicy.MESSAGE; return }
            busy=true
            SearchRanking.record(query).addOnFailureListener {
                if(fragment.view === view) Toast.makeText(context,"검색은 진행하지만 인기 검색어 집계에 반영하지 못했어요.",Toast.LENGTH_SHORT).show()
            }
            hideKeyboard()
            input.clearFocus()
            panel.visibility=View.GONE
            fragment.findNavController().navigate(R.id.action_home_to_search,Bundle().apply { putString("query",query) })
        }
        fun render() {
            rows.removeAllViews()
            ranking.forEachIndexed { index,item -> rows.addView(Button(context).apply {
                text="${index+1}.  ${item.term}    ${item.count}회"
                isAllCaps=false
                gravity=android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                setOnClickListener { input.setText(item.term); submit(item.term) }
            }) }
        }
        fun showRanking() {
            panel.visibility=View.VISIBLE
        }
        input.setOnFocusChangeListener { _, focused -> if(focused) showRanking() else panel.visibility=View.GONE }
        input.setOnClickListener { showRanking() }
        input.doAfterTextChanged { input.error=null }
        input.setOnEditorActionListener { _, action,event ->
            if(action == EditorInfo.IME_ACTION_SEARCH || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) { submit(input.text.toString()); true } else false
        }
        view.findViewById<View>(R.id.buttonHomeSearch).setOnClickListener { submit(input.text.toString()) }
        view.findViewById<View>(R.id.buttonClosePopularSearches).setOnClickListener { input.clearFocus(); panel.visibility=View.GONE; hideKeyboard() }
        fragment.viewLifecycleOwner.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                busy=false
                authListener=FirebaseAuth.AuthStateListener { auth ->
                    registration?.remove(); registration=null; ranking=emptyList(); render()
                    if(auth.currentUser == null) status.text="로그인 후 전체 인기 검색어를 볼 수 있어요."
                    else {
                        status.text="전체 사용자 · 누적 검색 횟수 · 불러오는 중"
                        registration=SearchRanking.top().addSnapshotListener { snapshot,error ->
                            if(fragment.view !== view) return@addSnapshotListener
                            if(error != null || snapshot == null) status.text="인기 검색어를 불러오지 못했어요. 연결 상태를 확인해 주세요."
                            else {
                                ranking=snapshot.documents.map { RankedSearch(it.getString("term").orEmpty(),it.getLong("count") ?: 0L) }
                                status.text=if(ranking.isEmpty()) "아직 집계된 검색어가 없어요. 첫 검색을 해 보세요." else "전체 사용자 · 누적 검색 횟수" + if(snapshot.metadata.isFromCache) " · 저장된 순위" else ""
                                render()
                            }
                        }
                    }
                }
                FirebaseAuth.getInstance().addAuthStateListener(authListener!!)
            }
            override fun onStop(owner: LifecycleOwner) {
                registration?.remove(); registration=null
                authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }; authListener=null
            }
        })
    }
}
