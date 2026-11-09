package com.project.maru

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
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
import java.util.Calendar
import java.util.Locale

class MyInfoFragment : Fragment(R.layout.fragment_my_info) {
    private var _binding: FragmentMyInfoBinding? = null
    private val binding get() = requireNotNull(_binding)
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(bars.left, 0, bars.right, 0)
            insets
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun saveAccountName(userId: String) {
        val name = binding.editMyInfoName.text.toString().trim()
        if (name.isBlank()) {
            binding.editMyInfoName.error = getString(R.string.signup_username_required)
            return
        }
        binding.buttonSaveMyInfo.isEnabled = false
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        user.updateProfile(request).addOnCompleteListener { profileTask ->
            if (!profileTask.isSuccessful) {
                finishAccountSave(false, name)
                return@addOnCompleteListener
            }
            firestore.collection("users").document(userId)
                .set(mapOf("displayName" to name), SetOptions.merge())
                .addOnCompleteListener { firestoreTask ->
                    finishAccountSave(firestoreTask.isSuccessful, name)
                }
        }
    }

    private fun finishAccountSave(success: Boolean, name: String) {
        val currentBinding = _binding ?: return
        currentBinding.buttonSaveMyInfo.isEnabled = true
        Toast.makeText(
            context,
            if (success) "이름을 수정했어요." else getString(R.string.settings_save_failed),
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
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }
        val nameInput = EditText(requireContext()).apply {
            hint = getString(R.string.child_name_hint)
            maxLines = 1
        }
        content.addView(nameInput)

        var birthDate = ""
        val dateButton = Button(requireContext()).apply {
            text = getString(R.string.child_birthdate)
            isAllCaps = false
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
        content.addView(dateButton)

        val genderGroup = RadioGroup(requireContext()).apply { orientation = RadioGroup.HORIZONTAL }
        val girl = makeGenderOption("여아")
        val boy = makeGenderOption("남아")
        genderGroup.addView(girl, RadioGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        genderGroup.addView(boy, RadioGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(genderGroup)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_child)
            .setView(content)
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
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.bg_mypage_profile)
            setPadding(dp(14), dp(10), dp(10), dp(10))
        }
        val details = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        val name = document.getString("name").orEmpty().ifBlank { "아이" }
        val birthDate = document.getString("birthDate").orEmpty()
        val gender = document.getString("gender").orEmpty()
        details.addView(TextView(requireContext()).apply {
            text = name
            setTextColor(android.graphics.Color.rgb(48, 48, 48))
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        details.addView(TextView(requireContext()).apply {
            text = listOf(birthDate, gender).filter { it.isNotBlank() }.joinToString(" · ")
            setTextColor(android.graphics.Color.rgb(135, 128, 126))
            textSize = 12f
            val top = dp(4)
            setPadding(0, top, 0, 0)
        })
        row.addView(details, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Button(requireContext()).apply {
            text = getString(R.string.my_info_edit_button)
            isAllCaps = false
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            background = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.bg_choice_button_primary)
            setOnClickListener { showChildEditDialog(document) }
        }, LinearLayout.LayoutParams(dp(66), dp(40)))
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.bottomMargin = dp(10)
        binding.layoutMyInfoChildren.addView(row, params)
    }

    private fun showChildEditDialog(document: DocumentSnapshot) {
        val childRef = document.reference
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }
        val nameInput = EditText(requireContext()).apply {
            setText(document.getString("name").orEmpty())
            hint = getString(R.string.child_name_hint)
            maxLines = 1
        }
        content.addView(nameInput)
        var birthDate = document.getString("birthDate").orEmpty()
        val dateButton = Button(requireContext()).apply {
            text = birthDate.ifBlank { getString(R.string.child_birthdate) }
            isAllCaps = false
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
        content.addView(dateButton)
        val genderGroup = RadioGroup(requireContext()).apply { orientation = RadioGroup.HORIZONTAL }
        val girl = makeGenderOption("여아")
        val boy = makeGenderOption("남아")
        genderGroup.addView(girl, RadioGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        genderGroup.addView(boy, RadioGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        when (document.getString("gender")) {
            "여아" -> genderGroup.check(girl.id)
            "남아" -> genderGroup.check(boy.id)
        }
        content.addView(genderGroup)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.my_info_edit_child)
            .setView(content)
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

    private fun makeGenderOption(value: String) = RadioButton(requireContext()).apply {
        id = View.generateViewId()
        text = value
        buttonTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(255, 133, 139))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
