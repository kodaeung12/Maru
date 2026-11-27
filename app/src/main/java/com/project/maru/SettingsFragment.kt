package com.project.maru

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private lateinit var removal: AccountRemoval
    override fun onViewCreated(view: View,savedInstanceState: Bundle?) {
        super.onViewCreated(view,savedInstanceState)
        removal=ViewModelProvider(requireActivity())[AccountRemoval::class.java]
        view.findViewById<TextView>(R.id.tvTopTitle).text="설정"
        fun goBack() {
            if(removal.state.value?.busy==true) Toast.makeText(context,"탈퇴 처리 중이에요. 잠시 기다려 주세요.",Toast.LENGTH_SHORT).show()
            else findNavController().navigateUp()
        }
        view.findViewById<View>(R.id.btnBack).setOnClickListener { goBack() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goBack() }
        })
        listOf(R.id.buttonServiceTerms to "서비스 이용약관",R.id.buttonPrivacyPolicy to "개인정보 처리방침",
            R.id.buttonLocationTerms to "위치기반 서비스 이용약관",R.id.buttonLocationPrivacy to "개인위치정보 처리방침").forEach { (id,title) ->
            view.findViewById<View>(id).setOnClickListener {
                AlertDialog.Builder(requireContext()).setTitle(title).setMessage("문서 준비 중이에요. 정식 문서가 등록되면 여기에서 확인할 수 있어요.")
                    .setPositiveButton("확인",null).show()
            }
        }
        view.findViewById<View>(R.id.buttonPushSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,requireContext().packageName))
        }
        view.findViewById<View>(R.id.buttonSettingsLogout).setOnClickListener {
            AlertDialog.Builder(requireContext()).setTitle("로그아웃할까요?").setMessage("저장한 정보는 유지돼요. 다시 로그인하면 확인할 수 있어요.")
                .setNegativeButton("취소",null).setPositiveButton("로그아웃") { _,_ ->
                    FirebaseAuth.getInstance().signOut()
                    ScheduleAlarms.clearOtherAccount(requireContext(),null)
                    leaveAccount()
                }.show()
        }
        view.findViewById<View>(R.id.buttonDeleteAccount).setOnClickListener {
            if(FirebaseAuth.getInstance().currentUser==null) {
                Toast.makeText(context,"로그인 후 이용해 주세요.",Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            AlertDialog.Builder(requireContext()).setTitle("회원탈퇴 안내")
                .setMessage("계정과 아이 정보·즐겨찾기·일정, 작성한 글·댓글·리뷰를 삭제해요. 삭제된 데이터는 복구할 수 없어요. 본인 글의 댓글도 함께 삭제돼요.\n\n비밀번호 확인 후 탈퇴가 진행돼요. 탈퇴 도중에는 앱을 종료하지 마세요.")
                .setNegativeButton("취소",null).setPositiveButton("계속") { _,_ -> confirmPassword() }.show()
        }
        removal.state.observe(viewLifecycleOwner) { state ->
            listOf(R.id.buttonSettingsLogout,R.id.buttonDeleteAccount).forEach { view.findViewById<View>(it).isEnabled=!state.busy }
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)?.menu?.let { menu ->
                for(index in 0 until menu.size()) menu.getItem(index).isEnabled=!state.busy
            }
            val status=view.findViewById<TextView>(R.id.textAccountDeletionStatus)
            status.text=state.message
            status.visibility=if(state.message.isBlank()) View.GONE else View.VISIBLE
            if(state.complete && !requireActivity().isFinishing) {
                Toast.makeText(context,state.message,Toast.LENGTH_LONG).show()
                leaveAccount()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.textPushSettingsStatus)?.text=
            if(NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) "휴대폰 앱 알림: 켜짐 · 눌러 알림별 설정 변경"
            else "휴대폰 앱 알림: 꺼짐 · 눌러 알림 허용 변경"
    }
    private fun confirmPassword() {
        val input=EditText(requireContext()).apply {
            hint="현재 비밀번호"; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSaveEnabled=false
        }
        val dialog=AlertDialog.Builder(requireContext()).setTitle("비밀번호 확인 후 탈퇴")
            .setMessage("비밀번호는 본인 확인에만 사용돼요.").setView(input)
            .setNegativeButton("취소",null).setPositiveButton("영구 탈퇴",null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val password=input.text.toString()
                if(password.isBlank()) input.error="비밀번호를 입력해 주세요."
                else { input.text.clear(); dialog.dismiss(); removal.delete(password) }
            }
        }
        dialog.show()
    }
    private fun leaveAccount() {
        startActivity(Intent(requireContext(),LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        requireActivity().finish()
    }
}
