package com.project.maru.community

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.project.maru.R
import com.project.maru.databinding.ItemCommunityImageBinding

// 리소스 숫자 ID 대신 이 문자열을 Firestore에 저장한다.
object CommunityImages {
    val keys = listOf("child", "meal", "health", "play", "sleep", "growth", "emotion", "social", "school", "daily")
    val labels = listOf("아이", "식습관", "건강·질병", "놀이·체험", "수면·생활습관", "발달·성장", "훈육·감정", "사회성·친구관계", "어린이집·학교", "일상공유")

    fun show(view: ImageView, key: String) {
        val actualKey = if (keys.contains(key)) key else "child"
        val image = when (actualKey) {
            "meal" -> R.drawable.community_rep_meal
            "health" -> R.drawable.community_rep_health_card
            "play" -> R.drawable.community_rep_play_card
            "sleep" -> R.drawable.community_rep_sleep_card
            "growth" -> R.drawable.community_rep_growth_card
            "emotion" -> R.drawable.community_rep_emotion_card
            "social" -> R.drawable.community_rep_social_card
            "school" -> R.drawable.community_rep_school_card
            "daily" -> R.drawable.community_rep_daily_card
            else -> R.drawable.community_rep_child
        }
        view.setImageResource(image)
        if (actualKey == "child" || actualKey == "meal") {
            view.setBackgroundResource(if (actualKey == "meal") R.drawable.community_rep_meal_background else R.drawable.community_rep_child_background)
            val pad = (8 * view.resources.displayMetrics.density).toInt()
            view.setPadding(pad, pad, pad, pad)
        } else {
            view.background = null
            view.setPadding(0, 0, 0, 0)
        }
    }
}

class CommunityImageAdapter(var selectedKey: String) : BaseAdapter() {
    override fun getCount() = CommunityImages.keys.size
    override fun getItem(position: Int) = CommunityImages.keys[position]
    override fun getItemId(position: Int) = position.toLong()
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null) {
            ItemCommunityImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        } else {
            ItemCommunityImageBinding.bind(convertView)
        }
        val key = getItem(position)
        if (binding.ivImage.tag != key) CommunityImages.show(binding.ivImage, key)
        binding.root.background = null
        binding.selectionOutline.visibility = if (selectedKey == key) View.VISIBLE else View.GONE
        binding.root.contentDescription = CommunityImages.labels[position] + if (selectedKey == key) " 선택됨" else ""
        binding.tvSelected.visibility = if (selectedKey == key) View.VISIBLE else View.GONE
        // 원본 카드 아래의 글씨 영역은 뷰에서 잘라 내고 같은 이름을 읽기 쉬운 텍스트로 표시한다.
        binding.tvLabel.visibility = View.VISIBLE
        binding.tvLabel.text = CommunityImages.labels[position]
        binding.ivImage.tag = key
        fun fitArt() {
            val image = binding.ivImage
            if (image.tag != key || image.width <= 0) return
            val density = image.resources.displayMetrics.density
            if (position < 2) {
                val height = (56 * density).toInt()
                if (image.layoutParams.height != height) image.layoutParams = image.layoutParams.apply { this.height = height }
                image.scaleType = ImageView.ScaleType.FIT_CENTER
                val horizontal = (image.width * 0.25f).toInt()
                val vertical = (11 * density).toInt()
                if (image.paddingLeft != horizontal || image.paddingTop != vertical) image.setPadding(horizontal, vertical, horizontal, vertical)
            } else {
                if (image.paddingLeft != 0 || image.paddingTop != 0) image.setPadding(0, 0, 0, 0)
                image.scaleType = ImageView.ScaleType.FIT_XY
                val height = (image.width * 139f / 130f).toInt()
                if (image.layoutParams.height != height) image.layoutParams = image.layoutParams.apply { this.height = height }
            }
        }
        binding.ivImage.post { fitArt() }
        return binding.root
    }
}
