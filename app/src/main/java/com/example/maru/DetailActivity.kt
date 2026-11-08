package com.example.maru

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
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
        const val EXTRA_CATEGORY = "extra_category"       // CODENAME
    }

    private lateinit var tabInfoText: TextView
    private lateinit var tabInfoLine: View
    private lateinit var tabReviewText: TextView
    private lateinit var tabReviewLine: View
    private lateinit var layoutInfo: LinearLayout
    private lateinit var layoutReview: LinearLayout

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
        showReviews(sampleReviews)

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

    // 하단 네비게이션 바 (홈 / 검색 / 커뮤니티 / 마이페이지)
    // TODO: 병합할 때 각 화면 Activity가 생기면 showPreparing() 대신 startActivity(...)로 바꾸기
    //       예) startActivity(Intent(this, HomeActivity::class.java))
    private fun setupBottomNav() {
        findViewById<View>(R.id.navHome).setOnClickListener { showPreparing() }
        findViewById<View>(R.id.navEvent).setOnClickListener {
            finish()   // 체험·행사 목록에서 들어왔으므로 닫으면 목록 화면으로 돌아감
        }
        findViewById<View>(R.id.navCommunity).setOnClickListener { showPreparing() }
        findViewById<View>(R.id.navMypage).setOnClickListener { showPreparing() }
    }

    private fun showPreparing() {
        Toast.makeText(this, "준비 중인 화면입니다.", Toast.LENGTH_SHORT).show()
    }
}
