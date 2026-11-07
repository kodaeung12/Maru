package com.project.maru

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ExperienceListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_experience_list)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerExperience)

        val searchEditText = findViewById<EditText>(R.id.editSearch)

        val categoryAll = findViewById<Button>(R.id.categoryAll)
        val categoryExperience = findViewById<Button>(R.id.categoryExperience)
        val categoryEducation = findViewById<Button>(R.id.categoryEducation)
        val categoryCulture = findViewById<Button>(R.id.categoryCulture)
        val categoryNature = findViewById<Button>(R.id.categoryNature)

        val filterRegion = findViewById<Button>(R.id.filterRegion)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val experienceAdapter = ExperienceAdapter(experienceList)
        recyclerView.adapter = experienceAdapter

        SeoulCultureRetrofit.apiService.getCultureEvents(
            BuildConfig.SEOUL_API_KEY,
            1,
            50
        ).enqueue(object : Callback<SeoulCultureResponse> {

            override fun onResponse(
                call: Call<SeoulCultureResponse>,
                response: Response<SeoulCultureResponse>
            ) {
                if (response.isSuccessful) {
                    val cultureData = response.body()?.aiViewCulture

                    if (cultureData != null) {

                        cultureData.row.forEach { event ->
                            Log.d(
                                "MARU_AGE",
                                "행사명: ${event.TITLE} / 대상: ${event.USE_TRGT}"
                            )
                        }

                        cultureData.row
                            .groupingBy { it.CODENAME }
                            .eachCount()
                            .forEach { (category, count) ->
                                Log.d("MARU_CATEGORY", "$category : ${count}개")
                            }

                        cultureData.row
                            .filter {
                                it.CODENAME !in listOf(
                                    "교육/체험",
                                    "축제-자연/경관",
                                    "전시/미술", "공연", "콘서트",
                                    "뮤지컬/오페라", "연극", "클래식",
                                    "무용", "독주/독창회", "국악", "영화"
                                )
                            }
                            .forEach { event ->
                                Log.d(
                                    "MARU_OTHER",
                                    "행사명: ${event.TITLE} / 분류: ${event.CODENAME} / 대상: ${event.USE_TRGT}"
                                )
                            }

                        cultureData.row
                            .filter { it.CODENAME == "교육/체험" }
                            .forEach { event ->
                                Log.d(
                                    "MARU_EDUCATION",
                                    "행사명: ${event.TITLE} / 대상: ${event.USE_TRGT}"
                                )
                            }

                        val apiExperienceList = cultureData.row.map { event ->
                            Experience(
                                title = event.TITLE,
                                location = event.PLACE,
                                date = event.DATE,
                                category = when (event.CODENAME) {

                                    "교육/체험" -> {
                                        if (
                                            event.TITLE.contains("교육", ignoreCase = true) ||
                                            event.TITLE.contains("강좌", ignoreCase = true) ||
                                            event.TITLE.contains("북토크", ignoreCase = true)
                                        ) {
                                            "교육"
                                        } else {
                                            "체험"
                                        }
                                    }

                                    "축제-자연/경관" -> "자연"

                                    "전시/미술", "공연", "콘서트", "뮤지컬/오페라",
                                    "연극", "클래식", "무용", "독주/독창회",
                                    "국악", "영화" -> "문화/예술"

                                    "축제-관광/체육" -> "체험"

                                    "축제-기타" -> "문화/예술"

                                    else -> "기타"
                                },
                                imageUrl = event.MAIN_IMG,
                                targetAge = event.USE_TRGT,
                                region = event.GUNAME
                            )
                        }

                        val filteredExperienceList = apiExperienceList.filter { experience ->
                            isSuitableForAge5To8(experience.targetAge)
                        }

                        filteredExperienceList.forEach { experience ->
                            Log.d(
                                "MARU_FILTER_RESULT",
                                "행사: ${experience.title} / 대상: ${experience.targetAge}"
                            )
                        }

                        experienceAdapter.updateData(filteredExperienceList)

                        Log.d(
                            "MARU_AGE_FILTER",
                            "전체 ${apiExperienceList.size}개 중 ${filteredExperienceList.size}개 선별"
                        )

                        Log.d(
                            "SeoulCultureAPI",
                            "실제 행사 ${apiExperienceList.size}개 화면 연결 완료"
                        )

                    } else {
                        Log.e(
                            "SeoulCultureAPI",
                            "API 응답 데이터가 비어 있습니다."
                        )
                    }
                } else {
                    Log.e(
                        "SeoulCultureAPI",
                        "서버 응답 오류: ${response.code()}"
                    )
                }
            }

            override fun onFailure(
                call: Call<SeoulCultureResponse>,
                t: Throwable
            ) {
                Log.e("SeoulCultureAPI", "API 연결 실패", t)
            }
        })

        searchEditText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                experienceAdapter.filterList(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        val categoryButtons = listOf(
            categoryAll,
            categoryExperience,
            categoryEducation,
            categoryCulture,
            categoryNature
        )

        categoryButtons.forEach { button ->
            button.setBackgroundResource(R.drawable.bg_category_unselected)
            button.backgroundTintList = null

            button.setTextColor(
                android.graphics.Color.parseColor("#FF858A")
            )
        }

        categoryAll.setBackgroundResource(R.drawable.bg_category_selected)
        categoryAll.backgroundTintList = null
        categoryAll.setTextColor(
            android.graphics.Color.WHITE
        )

        fun selectCategory(selectedButton: Button, category: String) {

            categoryButtons.forEach { button ->
                button.setBackgroundResource(R.drawable.bg_category_unselected)
                button.backgroundTintList = null

                button.setTextColor(
                    android.graphics.Color.parseColor("#FF858A")
                )
            }

            selectedButton.setBackgroundResource(R.drawable.bg_category_selected)
            selectedButton.backgroundTintList = null

            selectedButton.setTextColor(
                android.graphics.Color.parseColor("#FFFFFF")
            )

            experienceAdapter.filterCategory(category)
        }

        categoryAll.setOnClickListener {
            selectCategory(categoryAll, "전체")
        }

        categoryExperience.setOnClickListener {
            selectCategory(categoryExperience, "체험")
        }

        categoryEducation.setOnClickListener {
            selectCategory(categoryEducation, "교육")
        }

        categoryCulture.setOnClickListener {
            selectCategory(categoryCulture, "문화/예술")
        }

        categoryNature.setOnClickListener {
            selectCategory(categoryNature, "자연")
        }

        filterRegion.setOnClickListener {

            val regions = arrayOf(
                "전체 지역",
                "강남구", "강동구", "강북구", "강서구", "관악구",
                "광진구", "구로구", "금천구", "노원구", "도봉구",
                "동대문구", "동작구", "마포구", "서대문구", "서초구",
                "성동구", "성북구", "송파구", "양천구", "영등포구",
                "용산구", "은평구", "종로구", "중구", "중랑구"
            )

            AlertDialog.Builder(this)
                .setTitle("지역 선택")
                .setItems(regions) { _, which ->

                    filterRegion.text = "${regions[which]}  ⌄"

                    experienceAdapter.filterRegion(regions[which])
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }
}
