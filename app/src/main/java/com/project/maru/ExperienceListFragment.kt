package com.project.maru

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 통합 기능: 행사 목록을 하단 내비게이션 내부에서 표시하는 Fragment
class ExperienceListFragment : Fragment(R.layout.fragment_experience_list) {

    // Fragment의 화면이 생성된 후 행사 목록과 필터 기능을 초기화
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fragment 화면 내부의 UI 요소 연결
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerExperience)
        val searchEditText = view.findViewById<EditText>(R.id.editSearch)

        val categoryAll = view.findViewById<Button>(R.id.categoryAll)
        val categoryExperience = view.findViewById<Button>(R.id.categoryExperience)
        val categoryEducation = view.findViewById<Button>(R.id.categoryEducation)
        val categoryCulture = view.findViewById<Button>(R.id.categoryCulture)
        val categoryNature = view.findViewById<Button>(R.id.categoryNature)
        val filterRegion = view.findViewById<Button>(R.id.filterRegion)

        // 기존 행사 목록과 동일한 2열 카드형 RecyclerView 구성
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val experienceAdapter = ExperienceAdapter(experienceList)
        recyclerView.adapter = experienceAdapter

        // 서울시 문화행사 API 호출 및 5~8세 대상 행사 선별
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

                        // API 응답을 기존 Experience 데이터 모델로 변환
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

                        // 5~8세 대상 행사만 화면에 표시
                        val filteredExperienceList = apiExperienceList.filter { experience ->
                            isSuitableForAge5To8(experience.targetAge)
                        }

                        filteredExperienceList.forEach { experience ->
                            Log.d(
                                "MARU_FILTER_RESULT",
                                "행사: ${experience.title} / 대상: ${experience.targetAge}"
                            )
                        }

                        // API에서 선별한 행사 목록을 어댑터에 전달
                        experienceAdapter.updateData(filteredExperienceList)

                        Log.d(
                            "MARU_ADAPTER_COUNT",
                            "어댑터 행사 개수: ${experienceAdapter.itemCount}"
                        )

                        Log.d(
                            "MARU_AGE_FILTER",
                            "전체 ${apiExperienceList.size}개 중 ${filteredExperienceList.size}개 선별"
                        )

                        Log.d(
                            "SeoulCultureAPI",
                            "실제 행사 ${apiExperienceList.size}개 화면 연결 완료"
                        )

                    } else {
                        Log.e("SeoulCultureAPI", "API 응답 데이터가 비어 있습니다.")
                    }
                } else {
                    Log.e("SeoulCultureAPI", "서버 응답 오류: ${response.code()}")
                }
            }

            override fun onFailure(
                call: Call<SeoulCultureResponse>,
                t: Throwable
            ) {
                Log.e("SeoulCultureAPI", "API 연결 실패", t)
            }
        })

        // 행사명 검색창 입력에 따라 목록 필터링
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

        // 카테고리 버튼 목록과 기본 선택 상태 설정
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
            button.setTextColor(android.graphics.Color.parseColor("#FF858A"))
        }

        categoryAll.setBackgroundResource(R.drawable.bg_category_selected)
        categoryAll.backgroundTintList = null
        categoryAll.setTextColor(android.graphics.Color.WHITE)

        // 카테고리 선택 시 버튼 색상과 행사 목록을 함께 변경
        fun selectCategory(selectedButton: Button, category: String) {
            categoryButtons.forEach { button ->
                button.setBackgroundResource(R.drawable.bg_category_unselected)
                button.backgroundTintList = null
                button.setTextColor(android.graphics.Color.parseColor("#FF858A"))
            }

            selectedButton.setBackgroundResource(R.drawable.bg_category_selected)
            selectedButton.backgroundTintList = null
            selectedButton.setTextColor(android.graphics.Color.WHITE)

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

        // 지역 버튼을 누르면 서울시 자치구 선택 팝업 표시
        filterRegion.setOnClickListener {
            val regions = arrayOf(
                "전체 지역",
                "강남구", "강동구", "강북구", "강서구", "관악구",
                "광진구", "구로구", "금천구", "노원구", "도봉구",
                "동대문구", "동작구", "마포구", "서대문구", "서초구",
                "성동구", "성북구", "송파구", "양천구", "영등포구",
                "용산구", "은평구", "종로구", "중구", "중랑구"
            )

            AlertDialog.Builder(requireContext())
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
