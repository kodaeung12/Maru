package com.project.maru

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RatingBar
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button
import android.widget.ImageView
import android.widget.ImageButton
import com.project.maru.experience.Experience
import com.project.maru.experience.ExperiencePeriod
import com.project.maru.experience.FavoriteStore
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    // 행사 목록 화면에서 데이터를 넘길 때 사용하는 키 (이름표)
    companion object {
        const val EXTRA_TITLE = "extra_title"             // TITLE
        const val EXTRA_DATE = "extra_date"               // DATE
        const val EXTRA_PLACE = "extra_place"             // PLACE
        const val EXTRA_REGION = "extra_region"           // GUNAME
        const val EXTRA_IMAGE_URL = "extra_image_url"     // MAIN_IMG
        const val EXTRA_TARGET_AGE = "extra_target_age"   // USE_TRGT
        const val EXTRA_FEE = "extra_fee"                 // USE_FEE
        const val EXTRA_HOMEPAGE = "extra_homepage"       // HMPG_ADDR
        const val EXTRA_START_DATE = "extra_start_date"
        const val EXTRA_END_DATE = "extra_end_date"
        const val EXTRA_CATEGORY = "extra_category"       // CODENAME
    }

    private lateinit var tabInfoText: TextView
    private lateinit var tabInfoLine: View
    private lateinit var tabReviewText: TextView
    private lateinit var tabReviewLine: View
    private lateinit var layoutInfo: LinearLayout
    private lateinit var layoutReview: LinearLayout

    private var reviewEventId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail)

        // 상태바/내비게이션바에 화면이 가려지지 않게 여백 주기
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ---------- 1. 목록 화면에서 넘어온 행사 데이터 받기 ----------
        // 목록 없이 이 화면만 실행하면(테스트용) 오른쪽의 샘플 데이터가 표시됨
        val title = getData(EXTRA_TITLE, "가을 가족 체험 축제")
        val date = getData(EXTRA_DATE, "2026. 10. 25(일) ~ 10. 26(월)")
        val place = getData(EXTRA_PLACE, "서울 어린이대공원")
        val region = getData(EXTRA_REGION, "광진구")
        val imageUrl = getData(EXTRA_IMAGE_URL, "")
        val targetAge = getData(EXTRA_TARGET_AGE, "3세 ~ 8세")
        val fee = getData(EXTRA_FEE, "무료")
        val homepage = getData(EXTRA_HOMEPAGE, "https://culture.seoul.go.kr")
        val category = getData(EXTRA_CATEGORY, "축제")
        val favorite = findViewById<ImageButton>(R.id.buttonDetailFavorite)
        favorite.visibility = if (isFromList()) View.VISIBLE else View.GONE
        val event = Experience(title, place, date, category,
            imageUrl = imageUrl.takeIf { it.isNotBlank() }, targetAge = targetAge, region = region,
            startDate = ExperiencePeriod.parseApiDate(intent.getStringExtra(EXTRA_START_DATE)),
            endDate = ExperiencePeriod.parseApiDate(intent.getStringExtra(EXTRA_END_DATE)))
        val scheduleButton = Button(this).apply {
            text = "내 일정에 추가"
            setOnClickListener { ScheduleUi.showEditor(this@DetailActivity,
                date = event.startDate?.takeIf { !it.isBefore(java.time.LocalDate.now()) } ?: java.time.LocalDate.now(),
                initialTitle = title, initialNote = place, eventId = eventReviewId(title, place, date)) }
        }
        val officialButton = findViewById<Button>(R.id.btnOfficialSite)
        (officialButton.parent as android.view.ViewGroup).addView(scheduleButton,
            (officialButton.parent as android.view.ViewGroup).indexOfChild(officialButton) + 1)
        val favorites = FavoriteStore(this) { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        favorites.onChanged = {
            favorite.isSelected = favorites.isSaved(event)
            favorite.isEnabled = !favorites.isPending(event)
            favorite.alpha = if (favorites.ready && !favorites.isPending(event)) 1f else 0.5f
            favorite.contentDescription = getString(if (favorite.isSelected) R.string.favorite_remove else R.string.favorite_add)
        }
        favorite.setOnClickListener { favorites.toggle(event) }

        // ---------- 2. 화면에 데이터 표시 ----------
        findViewById<TextView>(R.id.textDetailTitle).text = title

        // API에는 행사 소개글이 없어서 카테고리로 간단한 소개 문구를 만듦
        findViewById<TextView>(R.id.textDetailDescription).text =
            if (isFromList()) {
                "아이와 함께하는 특별한 하루!\n${orDefault(category)} 행사에 초대합니다."
            } else {
                "아이와 함께하는 가을의 특별한 하루!\n다양한 체험과 공연이 가득한 가족 축제에 초대합니다."
            }

        findViewById<TextView>(R.id.textDetailDate).text = orDefault(date)
        findViewById<TextView>(R.id.textDetailPlace).text = orDefault(place)
        findViewById<TextView>(R.id.textDetailAge).text = orDefault(targetAge)
        findViewById<TextView>(R.id.textDetailFee).text = orDefault(fee)

        // 시간 정보는 API에 없어서 샘플일 때만 표시
        setTextOrHide(findViewById(R.id.textDetailTime), if (isFromList()) "" else "10:00 ~ 17:00")
        setTextOrHide(findViewById(R.id.textDetailRegion), region)

        // 문의처도 API에 없어서 샘플일 때만 표시, 목록에서 왔으면 홈페이지 안내
        if (isFromList()) {
            findViewById<TextView>(R.id.textDetailContact).text = "공식 사이트를 확인해주세요"
            setTextOrHide(findViewById(R.id.textDetailPhone), "")
        } else {
            findViewById<TextView>(R.id.textDetailContact).text = "한국사회공헌 민원팀"
            setTextOrHide(findViewById(R.id.textDetailPhone), "02-123-4567")
        }

        // 대표 이미지: API 이미지 주소가 있으면 Glide로 불러오고, 없으면 drawable/detail_event 사용
        val imageView = findViewById<ImageView>(R.id.imageDetail)
        if (imageUrl.isNotBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.detail_event)
                .error(R.drawable.detail_event)
                .centerCrop()
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.detail_event)
        }

        // ---------- 3. 별점 + 리뷰 목록 ----------
        reviewEventId = if (isFromList()) eventReviewId(title, place, date) else ""
        showReviews(emptyList())
        findViewById<Button>(R.id.buttonRefreshReviews).setOnClickListener { loadReviews() }
        findViewById<Button>(R.id.buttonSaveReview).setOnClickListener { saveReview() }
        findViewById<Button>(R.id.buttonSaveReview).isEnabled = reviewEventId.isNotBlank()
        loadReviews()

        // ---------- 4. 탭 (행사 안내 / 행사 리뷰) ----------
        tabInfoText = findViewById(R.id.tabInfoText)
        tabInfoLine = findViewById(R.id.tabInfoLine)
        tabReviewText = findViewById(R.id.tabReviewText)
        tabReviewLine = findViewById(R.id.tabReviewLine)
        layoutInfo = findViewById(R.id.layoutInfo)
        layoutReview = findViewById(R.id.layoutReview)

        findViewById<View>(R.id.tabInfo).setOnClickListener { selectTab(true) }
        findViewById<View>(R.id.tabReview).setOnClickListener { selectTab(false) }
        selectTab(true)

        // ---------- 5. 버튼들 ----------
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnOfficialSite).setOnClickListener {
            openWebsite(homepage)
        }

        setupBottomNav()
    }

    private fun loadReviews() {
        val status = findViewById<TextView>(R.id.textReviewStatus)
        if (reviewEventId.isBlank()) {
            status.text = "행사 목록에서 선택한 행사의 리뷰를 볼 수 있어요."
            return
        }
        val refresh = findViewById<Button>(R.id.buttonRefreshReviews)
        refresh.isEnabled = false
        status.text = "온라인 리뷰를 불러오는 중…"
        ReviewRepository.load(reviewEventId).addOnCompleteListener(this) { task ->
            refresh.isEnabled = true
            if (task.isSuccessful) {
                showReviews(task.result)
                val ownReview = task.result.firstOrNull { it.ownerId == FirebaseAuth.getInstance().currentUser?.uid }
                val input = findViewById<EditText>(R.id.editReviewContent)
                if (ownReview != null && input.text.isBlank()) {
                    input.setText(ownReview.content)
                    findViewById<RatingBar>(R.id.ratingReviewInput).rating = ownReview.rating.toFloat()
                }
                status.text = if (task.result.isEmpty()) "첫 리뷰를 작성해 보세요." else "한 행사에 내 리뷰를 하나씩 작성·수정할 수 있어요."
            } else {
                status.text = "리뷰를 불러오지 못했어요. 로그인과 연결 상태를 확인하고 새로고침해 주세요."
            }
        }
    }

    private fun saveReview() {
        val status = findViewById<TextView>(R.id.textReviewStatus)
        if (FirebaseAuth.getInstance().currentUser == null) {
            status.text = "로그인 후 리뷰를 작성해 주세요."
            return
        }
        if (reviewEventId.isBlank()) return
        val input = findViewById<EditText>(R.id.editReviewContent)
        val ratingInput = findViewById<RatingBar>(R.id.ratingReviewInput)
        val content = input.text.toString().trim()
        if (content.isBlank()) {
            input.error = "리뷰 내용을 입력해 주세요."
            return
        }
        if (ContentPolicy.containsAbuse(content)) {
            input.error = ContentPolicy.MESSAGE
            return
        }
        val button = findViewById<Button>(R.id.buttonSaveReview)
        if (!button.isEnabled) return
        button.isEnabled = false
        input.isEnabled = false
        ratingInput.isEnabled = false
        status.text = "온라인으로 리뷰를 저장하는 중…"
        ReviewRepository.save(reviewEventId, ratingInput.rating.toInt().coerceIn(1, 5), content)
            .addOnCompleteListener(this) { task ->
                button.isEnabled = true
                input.isEnabled = true
                ratingInput.isEnabled = true
                if (task.isSuccessful) {
                    input.text.clear()
                    Toast.makeText(this, "리뷰를 온라인으로 저장했어요.", Toast.LENGTH_SHORT).show()
                    loadReviews()
                } else {
                    status.text = "리뷰 저장에 실패했어요. 연결 상태를 확인하고 다시 시도해 주세요."
                }
            }
    }

    // 목록 화면에서 넘어왔는지 확인 (행사명이 넘어왔으면 목록에서 온 것)
    private fun isFromList(): Boolean = intent.hasExtra(EXTRA_TITLE)

    // 목록에서 왔으면 넘어온 값을, 아니면 샘플 값을 돌려줌
    private fun getData(key: String, sample: String): String {
        return if (isFromList()) intent.getStringExtra(key).orEmpty().trim() else sample
    }

    private fun orDefault(text: String): String = text.ifBlank { "정보 없음" }

    private fun setTextOrHide(textView: TextView, text: String) {
        textView.text = text
        textView.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
    }

    // 탭 선택: true = 행사 안내, false = 행사 리뷰
    private fun selectTab(isInfo: Boolean) {
        val pink = 0xFFFF858A.toInt()
        val gray = 0xFF777777.toInt()

        layoutInfo.visibility = if (isInfo) View.VISIBLE else View.GONE
        layoutReview.visibility = if (isInfo) View.GONE else View.VISIBLE

        tabInfoText.setTextColor(if (isInfo) pink else gray)
        tabReviewText.setTextColor(if (isInfo) gray else pink)
        tabInfoText.setTypeface(null, if (isInfo) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        tabReviewText.setTypeface(null, if (isInfo) android.graphics.Typeface.NORMAL else android.graphics.Typeface.BOLD)
        tabInfoLine.visibility = if (isInfo) View.VISIBLE else View.INVISIBLE
        tabReviewLine.visibility = if (isInfo) View.INVISIBLE else View.VISIBLE
    }

    // 별점 평균 계산 + 리뷰 카드 추가
    private fun showReviews(reviews: List<Review>) {
        val average = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
        findViewById<TextView>(R.id.textRating).text = String.format(Locale.KOREA, "%.1f", average)
        findViewById<TextView>(R.id.textReviewCount).text = "(${reviews.size}개의 후기)"

        val reviewList = findViewById<LinearLayout>(R.id.layoutReviewList)
        reviewList.removeAllViews()

        for (review in reviews) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_review, reviewList, false)
            item.findViewById<TextView>(R.id.textReviewName).text = review.name
            item.findViewById<TextView>(R.id.textReviewDate).text = review.date
            item.findViewById<TextView>(R.id.textReviewStars).text =
                "★".repeat(review.rating) + "☆".repeat(5 - review.rating)
            item.findViewById<TextView>(R.id.textReviewContent).text = review.content
            reviewList.addView(item)
        }
    }

    // 공식 사이트를 인터넷 브라우저로 열기
    private fun openWebsite(address: String) {
        if (address.isBlank()) {
            Toast.makeText(this, "공식 사이트 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // "www.~" 처럼 http가 빠진 주소도 열리도록 앞에 붙여줌
        val url = if (address.startsWith("http")) address else "https://$address"

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "사이트를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 하단 메뉴에서 홈을 누르면 앱의 실제 홈 화면으로 돌아간다.
    private fun setupBottomNav() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(HomeActivity.homeIntent(this))
        }
        findViewById<View>(R.id.navEvent).setOnClickListener {
            finish()   // 체험·행사 목록에서 들어왔으므로 닫으면 목록 화면으로 돌아감
        }
        findViewById<View>(R.id.navStudy).setOnClickListener {
            startActivity(HomeActivity.sectionIntent(this, R.id.ageEduFragment))
        }
        findViewById<View>(R.id.navCommunity).setOnClickListener {
            startActivity(HomeActivity.sectionIntent(this, R.id.communityFragment))
        }
        findViewById<View>(R.id.navMypage).setOnClickListener {
            startActivity(HomeActivity.sectionIntent(this, R.id.myPageFragment))
        }
    }
}
