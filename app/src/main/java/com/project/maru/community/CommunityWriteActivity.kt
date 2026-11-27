package com.project.maru.community

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.Source
import com.project.maru.MyApplication.Companion.auth
import com.project.maru.MyApplication.Companion.db
import com.project.maru.R
import com.project.maru.databinding.ActivityCommunityWriteBinding

class CommunityWriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommunityWriteBinding
    private lateinit var imageAdapter: CommunityImageAdapter
    private lateinit var childPicker: CommunityChildPicker
    private var postId = ""
    private var draftId = ""
    private var editingPost: CommunityPost? = null
    private var selectedImage = "child"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCommunityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        postId = intent.getStringExtra("postId") ?: ""
        childPicker = CommunityChildPicker(binding.spinnerChildAge)
        if (postId.isBlank()) childPicker.load(savedInstanceState?.getInt("childAge", -1) ?: -1)
        draftId = savedInstanceState?.getString("draftId")
            ?: db.collection(CommunityFirestore.POSTS).document().id
        selectedImage = savedInstanceState?.getString("selectedImage") ?: "child"

        val categories = resources.getStringArray(R.array.community_categories)
        val aadapter = ArrayAdapter(this, R.layout.community_spinner_item, categories)
        aadapter.setDropDownViewResource(R.layout.community_spinner_item)
        binding.spinnerCategory.adapter = aadapter
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { updatePreview() }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        imageAdapter = CommunityImageAdapter(selectedImage)
        binding.imageGrid.adapter = imageAdapter
        binding.imageGrid.setOnItemClickListener { _, _, position, _ ->
            selectedImage = CommunityImages.keys[position]
            imageAdapter.selectedKey = selectedImage
            imageAdapter.notifyDataSetChanged()
            updatePreview()
        }
        val textListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updatePreview() }
            override fun afterTextChanged(s: Editable?) { }
        }
        binding.etTitle.addTextChangedListener(textListener)
        binding.etContent.addTextChangedListener(textListener)
        binding.btnSave.setOnClickListener { savePost() }
        binding.btnSaveBottom.setOnClickListener { savePost() }
        binding.btnBack.setOnClickListener { finish() }
        updatePreview()

        if (postId.isNotBlank()) {
            binding.tvScreenTitle.text = "게시글 수정"
            binding.btnSave.text = "저장"
            binding.btnSaveBottom.text = "수정 저장"
            loadEditingPost()
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("childAge", childPicker.selectedAge)
        outState.putString("draftId", draftId)
        outState.putString("selectedImage", selectedImage)
        super.onSaveInstanceState(outState)
    }

    private fun updatePreview() {
        binding.tvTitleCount.text = "${binding.etTitle.text.length}/50"
        binding.tvContentCount.text = "${binding.etContent.text.length}/5000"
        binding.tvPreviewCategory.text = binding.spinnerCategory.selectedItem?.toString() ?: "육아정보"
        val title = binding.etTitle.text.toString().trim()
        binding.tvPreviewTitle.text = if (title.isBlank()) "제목을 입력해 주세요." else title
        CommunityImages.show(binding.ivPreview, selectedImage)
    }

    private fun loadEditingPost() {
        setFormEnabled(false)
        binding.tvStatus.text = "수정할 글을 불러오는 중…"
        db.collection(CommunityFirestore.POSTS).document(postId).get(Source.SERVER)
            .addOnSuccessListener { document ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                try {
                    val post = if (document.exists()) CommunityRepository.readPost(document) else null
                    val user = auth.currentUser
                    if (post == null || user == null || post.ownerId != user.uid || post.deleting) {
                        binding.tvStatus.text = "글이 없거나 수정 권한이 없어요. 목록으로 돌아가 주세요."
                        return@addOnSuccessListener
                    }
                    if (post.title.length > 50 || post.content.length > 5000) {
                        binding.tvStatus.text = "기존 글이 현재 입력 제한보다 길어요. 내용을 잘라 저장하지 않고 수정을 중단했어요."
                        return@addOnSuccessListener
                    }
                    post.id = document.id
                    editingPost = post
                    childPicker.load(post.childAge)
                    binding.etTitle.setText(post.title)
                    binding.etContent.setText(post.content)
                    val categories = resources.getStringArray(R.array.community_categories)
                    val position = categories.indexOf(post.category)
                    binding.spinnerCategory.setSelection(if (position < 0) 0 else position)
                    selectedImage = if (CommunityImages.keys.contains(post.imageKey)) post.imageKey else "child"
                    imageAdapter.selectedKey = selectedImage
                    imageAdapter.notifyDataSetChanged()
                    updatePreview()
                    binding.tvStatus.text = ""
                    setFormEnabled(true)
                } catch (error: Exception) { binding.tvStatus.text = CommunityFirestore.errorMessage(error) }
            }
            .addOnFailureListener { error ->
                if (!isFinishing && !isDestroyed) binding.tvStatus.text = CommunityFirestore.errorMessage(error)
            }
    }

    private fun savePost() {
        if (!binding.btnSave.isEnabled) return
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        if (title.isBlank()) {
            binding.etTitle.error = "제목을 입력해 주세요."
            binding.etTitle.requestFocus()
            return
        }
        if (content.isBlank()) {
            binding.etContent.error = "내용을 입력해 주세요."
            binding.etContent.requestFocus()
            return
        }
        if (com.project.maru.ContentPolicy.containsAbuse(title) || com.project.maru.ContentPolicy.containsAbuse(content)) {
            binding.tvStatus.text = com.project.maru.ContentPolicy.MESSAGE
            return
        }
        val user = auth.currentUser
        if (user == null || !user.isEmailVerified) {
            binding.tvStatus.text = "이메일 인증 후 로그인한 계정으로 글을 작성해 주세요."
            return
        }
        val category = binding.spinnerCategory.selectedItem.toString()
        val colref = db.collection(CommunityFirestore.POSTS)
        val oldPost = editingPost
        if (oldPost != null && oldPost.ownerId != user.uid) {
            binding.tvStatus.text = "작성자만 수정할 수 있어요."
            return
        }
        setFormEnabled(false)
        binding.tvStatus.text = "서버에 저장하는 중… 인터넷 연결을 유지해 주세요."
        val task = if (oldPost == null) {
            if (postId.isBlank()) postId = draftId
            val displayName = user.displayName
            val post = CommunityPost(
                id = postId, category = category, title = title, content = content,
                ownerId = user.uid,
                authorName = if (displayName.isNullOrBlank()) "마루 이용자" else displayName,
                createdAt = System.currentTimeMillis(), imageKey = selectedImage, childAge = childPicker.selectedAge
            )
            // 팀 코드의 중복 등록 방지 transaction을 사용하고 대표 이미지도 저장한다.
            CommunityRepository.createPost(post)
        } else {
            // 삭제된 글을 다시 만드는 일이 없도록 수정은 update()를 사용한다.
            // 작성자, ID, 작성 시각은 건드리지 않는다.
            colref.document(postId).update(mapOf(
                "title" to title, "content" to content,
                "category" to category, "imageKey" to selectedImage, "childAge" to childPicker.selectedAge
            ))
        }
        task.addOnSuccessListener {
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            Toast.makeText(this, if (oldPost == null) "게시글을 등록했어요." else "게시글을 수정했어요.", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
        task.addOnFailureListener { error ->
            if (!isFinishing && !isDestroyed) {
                setFormEnabled(true)
                binding.tvStatus.text = CommunityFirestore.errorMessage(error)
            }
        }
    }

    override fun onDestroy() { childPicker.close(); super.onDestroy() }

    private fun setFormEnabled(enabled: Boolean) {
        binding.btnSave.isEnabled = enabled
        binding.btnSaveBottom.isEnabled = enabled
        binding.etTitle.isEnabled = enabled
        binding.etContent.isEnabled = enabled
        binding.spinnerCategory.isEnabled = enabled
        binding.imageGrid.isEnabled = enabled
        binding.spinnerChildAge.isEnabled = enabled
    }
}
