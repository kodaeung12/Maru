package com.project.maru

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.project.maru.databinding.FragmentMyInfoBinding
import com.project.maru.databinding.DialogChildFormBinding
import com.project.maru.databinding.ItemMyInfoChildBinding
import java.util.Calendar
import java.util.Locale

class MyInfoFragment : Fragment(R.layout.fragment_my_info) {
    private var _binding: FragmentMyInfoBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var profileReady = false
    private var profileRequest = 0
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyInfoBinding.bind(view)
        view.findViewById<TextView>(R.id.tvTopTitle).text = getString(R.string.my_info_title)
        view.findViewById<View>(R.id.btnBack).setOnClickListener { findNavController().navigateUp() }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            binding.editMyInfoName.setText("")
            binding.tvMyInfoEmail.text = ""
            binding.tvMyInfoChildrenEmpty.visibility = View.VISIBLE
            return
        }

        binding.editMyInfoName.setText(user.displayName.orEmpty())
        binding.tvMyInfoEmail.text = user.email.orEmpty()
        binding.buttonSaveMyInfo.setOnClickListener { saveAccountName(user.uid) }
        binding.buttonAddMyChild.setOnClickListener { showAddChildDialog(user.uid) }
        loadChildren(user.uid)
        loadProfileGender(user.uid)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(bars.left, 0, bars.right, 0)
            insets
        }
    }

    override fun onDestroyView() {
        profileRequest++
        _binding = null
        super.onDestroyView()
    }

    private fun setProfileFormEnabled(enabled: Boolean) {
        val current = _binding ?: return
        current.buttonSaveMyInfo.isEnabled = enabled
        current.radioProfileFemale.isEnabled = enabled
        current.radioProfileMale.isEnabled = enabled
        current.radioProfileUnspecified.isEnabled = enabled
    }

    private fun loadProfileGender(uid: String) {
        val current = _binding ?: return
        val request = ++profileRequest
        profileReady = false
        setProfileFormEnabled(false)
        current.textProfileStatus.text = "프로필을 불러오는 중…"
        current.textProfileStatus.setOnClickListener { if (!profileReady) loadProfileGender(uid) }
        firestore.collection("users").document(uid).get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { document ->
                if (_binding !== current || request != profileRequest || FirebaseAuth.getInstance().currentUser?.uid != uid) return@addOnSuccessListener
                val image = ProfileAvatar.image(document.getString("gender"))
                current.radioProfileGender.check(when (image) {
                    R.drawable.ic_profile_female -> R.id.radioProfileFemale
                    R.drawable.ic_profile_male -> R.id.radioProfileMale
                    else -> R.id.radioProfileUnspecified
                })
                profileReady = true
                current.textProfileStatus.text = "선택한 성별에 맞는 프로필 이미지가 표시돼요."
                setProfileFormEnabled(true)
            }.addOnFailureListener {
                if (_binding === current && request == profileRequest) {
                    current.textProfileStatus.text = "프로필을 불러오지 못했어요. 여기를 눌러 다시 시도해 주세요."
                }
            }
    }

    private fun saveAccountName(userId: String) {
        if (!profileReady || !binding.buttonSaveMyInfo.isEnabled) return
        val current = binding
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (user.uid != userId) return
        val gender = when (current.radioProfileGender.checkedRadioButtonId) {
            R.id.radioProfileFemale -> "female"
            R.id.radioProfileMale -> "male"
            else -> "unspecified"
        }
        val name = binding.editMyInfoName.text.toString().trim()
        if (name.isBlank()) {
            binding.editMyInfoName.error = getString(R.string.signup_username_required)
            return
        }
        setProfileFormEnabled(false)
        val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        user.updateProfile(request).addOnCompleteListener { profileTask ->
            if (_binding !== current || FirebaseAuth.getInstance().currentUser?.uid != userId) return@addOnCompleteListener
            if (!profileTask.isSuccessful) {
                finishAccountSave(false, name)
                return@addOnCompleteListener
            }
            firestore.collection("users").document(userId)
                .set(mapOf("displayName" to name, "gender" to gender), SetOptions.merge())
                .addOnCompleteListener { firestoreTask ->
                    if (_binding === current && FirebaseAuth.getInstance().currentUser?.uid == userId)
                        finishAccountSave(firestoreTask.isSuccessful, name)
                }
        }
    }

    private fun finishAccountSave(success: Boolean, name: String) {
        val currentBinding = _binding ?: return
        setProfileFormEnabled(true)
        Toast.makeText(
            context,
            if (success) "내 정보를 저장했어요." else getString(R.string.settings_save_failed),
            Toast.LENGTH_SHORT
        ).show()
        if (success) currentBinding.editMyInfoName.setText(name)
    }

    private fun loadChildren(userId: String) {
        firestore.collection("users").document(userId).collection("children").get()
            .addOnSuccessListener { snapshot ->
                val currentBinding = _binding ?: return@addOnSuccessListener
                currentBinding.layoutMyInfoChildren.removeAllViews()
                currentBinding.tvMyInfoChildrenEmpty.visibility =
                    if (snapshot.isEmpty) View.VISIBLE else View.GONE
                snapshot.documents.forEach { addChildRow(it) }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    binding.tvMyInfoChildrenEmpty.text = getString(R.string.settings_save_failed)
                    binding.tvMyInfoChildrenEmpty.visibility = View.VISIBLE
                }
            }
    }

    private fun showAddChildDialog(userId: String) {
        val form = DialogChildFormBinding.inflate(layoutInflater)
        val nameInput = form.editChildName
        var birthDate = ""
        form.buttonBirthDate.apply {
            setOnClickListener {
                val today = Calendar.getInstance()
                DatePickerDialog(
                    requireContext(),
                    { _, year, month, day ->
                        birthDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                        text = String.format(Locale.KOREA, "%d년 %d월 %d일", year, month + 1, day)
                    },
                    today.get(Calendar.YEAR),
                    today.get(Calendar.MONTH),
                    today.get(Calendar.DAY_OF_MONTH)
                ).apply { datePicker.maxDate = today.timeInMillis }.show()
            }
        }
        val genderGroup = form.radioChildGender
        val girl = form.radioGirl
        val boy = form.radioBoy

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_child)
            .setView(form.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isBlank()) {
                    nameInput.error = getString(R.string.child_name_required)
                    return@setOnClickListener
                }
                if (birthDate.isBlank()) {
                    Toast.makeText(context, getString(R.string.child_birthdate_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val gender = when (genderGroup.checkedRadioButtonId) {
                    girl.id -> "여아"
                    boy.id -> "남아"
                    else -> ""
                }
                if (gender.isBlank()) {
                    Toast.makeText(context, getString(R.string.child_gender_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val childRef = firestore.collection("users").document(userId)
                    .collection("children").document()
                val childData = mapOf(
                    "name" to name,
                    "birthDate" to birthDate,
                    "gender" to gender,
                    "interests" to emptyList<String>(),
                    "traits" to emptyList<String>(),
                    "traitsNote" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                childRef.set(childData).addOnSuccessListener {
                    Toast.makeText(context, "아이 정보를 추가했어요.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadChildren(userId)
                }.addOnFailureListener {
                    Toast.makeText(context, getString(R.string.settings_save_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun addChildRow(document: DocumentSnapshot) {
        val item = ItemMyInfoChildBinding.inflate(layoutInflater, binding.layoutMyInfoChildren, false)
        item.textChildName.text = document.getString("name").orEmpty().ifBlank { "아이" }
        val birthDate = document.getString("birthDate").orEmpty()
        val gender = document.getString("gender").orEmpty()
        val genderImage = when (gender) {
            "남아" -> R.drawable.ic_child_boy
            "여아" -> R.drawable.ic_child_girl
            else -> null
        }
        item.imageChildGender.visibility = if (genderImage == null) View.GONE else View.VISIBLE
        if (genderImage != null) item.imageChildGender.setImageResource(genderImage)
        item.textChildDetails.text = listOf(birthDate, gender).filter { it.isNotBlank() }.joinToString(" · ")
        item.buttonEditChild.setOnClickListener { showChildEditDialog(document) }
        binding.layoutMyInfoChildren.addView(item.root)
    }

    private fun showChildEditDialog(document: DocumentSnapshot) {
        val childRef = document.reference
        val form = DialogChildFormBinding.inflate(layoutInflater)
        val nameInput = form.editChildName
        nameInput.setText(document.getString("name").orEmpty())
        var birthDate = document.getString("birthDate").orEmpty()
        form.buttonBirthDate.apply {
            text = birthDate.ifBlank { getString(R.string.child_birthdate) }
            setOnClickListener {
                val today = Calendar.getInstance()
                val parts = birthDate.split("-").mapNotNull { it.toIntOrNull() }
                DatePickerDialog(
                    requireContext(),
                    { _, year, month, day ->
                        birthDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                        text = String.format(Locale.KOREA, "%d년 %d월 %d일", year, month + 1, day)
                    },
                    parts.getOrNull(0) ?: today.get(Calendar.YEAR),
                    (parts.getOrNull(1)?.minus(1)) ?: today.get(Calendar.MONTH),
                    parts.getOrNull(2) ?: today.get(Calendar.DAY_OF_MONTH)
                ).apply { datePicker.maxDate = today.timeInMillis }.show()
            }
        }
        val genderGroup = form.radioChildGender
        val girl = form.radioGirl
        val boy = form.radioBoy
        when (document.getString("gender")) {
            "여아" -> genderGroup.check(girl.id)
            "남아" -> genderGroup.check(boy.id)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.my_info_edit_child)
            .setView(form.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val childName = nameInput.text.toString().trim()
                if (childName.isBlank()) {
                    nameInput.error = getString(R.string.child_name_required)
                    return@setOnClickListener
                }
                if (birthDate.isBlank()) {
                    Toast.makeText(context, getString(R.string.child_birthdate_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val updates = mutableMapOf<String, Any>("name" to childName, "birthDate" to birthDate)
                when (genderGroup.checkedRadioButtonId) {
                    girl.id -> updates["gender"] = "여아"
                    boy.id -> updates["gender"] = "남아"
                }
                childRef.update(updates).addOnSuccessListener {
                    Toast.makeText(context, "아이 정보를 수정했어요.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    FirebaseAuth.getInstance().currentUser?.uid?.let(::loadChildren)
                }.addOnFailureListener {
                    Toast.makeText(context, getString(R.string.settings_save_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

}
