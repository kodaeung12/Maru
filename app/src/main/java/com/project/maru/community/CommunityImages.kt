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
        CommunityImages.show(binding.ivImage, key)
        binding.root.setBackgroundResource(if (selectedKey == key) R.drawable.community_image_selected else R.drawable.community_image_idle)
        binding.root.contentDescription = CommunityImages.labels[position] + if (selectedKey == key) " 선택됨" else ""
        // 나머지 여덟 원본에는 이름이 이미 들어 있어 중복 표기하지 않는다.
        binding.tvLabel.visibility = if (position < 2) View.VISIBLE else View.GONE
        binding.tvLabel.text = CommunityImages.labels[position]
        return binding.root
    }
}
