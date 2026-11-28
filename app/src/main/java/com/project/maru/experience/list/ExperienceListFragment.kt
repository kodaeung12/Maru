package com.project.maru.experience.list

import com.project.maru.experience.location.EventCoordinates
import com.project.maru.experience.Experience
import com.project.maru.experience.data.ExperienceCandidateCache
import com.project.maru.experience.ExperiencePeriod
import com.project.maru.experience.data.familyExperiences
import com.project.maru.experience.FavoriteStore
import com.project.maru.experience.data.SEOUL_CULTURE_PAGE_SIZE
import com.project.maru.experience.data.SeoulCultureData
import com.project.maru.experience.data.SeoulCultureEvent
import com.project.maru.experience.data.SeoulCultureFirstPage
import com.project.maru.experience.data.SeoulCultureResponse
import com.project.maru.experience.data.SeoulCultureRetrofit

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.maru.BuildConfig
import com.project.maru.R
import com.project.maru.showHome
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ExperienceListFragment : Fragment(R.layout.fragment_experience_list) {
    private var activeCall: Call<SeoulCultureResponse>? = null
    private var firstPageJob: Job? = null

    private companion object {
        const val API_PAGE_SIZE = SEOUL_CULTURE_PAGE_SIZE
        const val API_MAX_COUNT = 5000
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        view.findViewById<TextView>(R.id.textExperienceTitle).text = navController.currentDestination?.label
        view.findViewById<View>(R.id.btnBack).setOnClickListener { navController.showHome() }

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerExperience)
        val search = view.findViewById<EditText>(R.id.editSearch)
        val status = view.findViewById<TextView>(R.id.textSearchStatus)
        val retry = view.findViewById<Button>(R.id.buttonRetrySearch)
        val favoritesMode = navController.currentDestination?.id == R.id.favoritesFragment
        val favorites = FavoriteStore(viewLifecycleOwner) { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        val adapter = ExperienceAdapter(emptyList(), favorites)
        var candidates: List<Experience> = emptyList()
        var loading = true
        var failed = false
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = adapter

        fun showResultCount() {
            if (loading || failed) return
            status.text = if (favoritesMode && favorites.items.isEmpty()) getString(R.string.favorites_empty)
                else if (adapter.itemCount == 0) getString(R.string.search_no_results)
                else getString(R.string.search_count, adapter.itemCount)
        }
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                showResultCount()
            }
        })
        search.doAfterTextChanged { adapter.filterList(it?.toString().orEmpty()) }
        search.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
        search.setOnEditorActionListener { _, action, event ->
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)) {
                val query = search.text.toString()
                adapter.filterList(query)
                if (!favoritesMode) com.project.maru.SearchRanking.record(query)
                (requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                    .hideSoftInputFromWindow(search.windowToken, 0)
                true
            } else false
        }
        if (savedInstanceState == null) search.setText(arguments?.getString("query").orEmpty())
        ExperienceCategoryPicker.bind(view.findViewById(R.id.filterCategory), adapter,
            arguments?.getString("category"), favoritesMode)
        ExperiencePeriodPicker.bind(view.findViewById(R.id.filterPeriod), adapter)
        val regionButton = view.findViewById<Button>(R.id.filterRegion)
        regionButton.setOnClickListener {
            val regions = arrayOf("전체 지역", "강남구", "강동구", "강북구", "강서구", "관악구",
                "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구",
                "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구",
                "용산구", "은평구", "종로구", "중구", "중랑구")
            AlertDialog.Builder(requireContext()).setTitle("지역 선택")
                .setItems(regions) { _, index ->
                    regionButton.text = "${regions[index]}  ⌄"
                    adapter.filterRegion(regions[index])
                }.setNegativeButton(android.R.string.cancel, null).show()
        }

        favorites.onChanged = {
            if (this.view === view) {
                if (favoritesMode) {
                    loading = !favorites.ready
                    failed = favorites.error != null
                    candidates = favorites.items
                    adapter.updateData(candidates)
                    status.text = favorites.error ?: if (loading) getString(R.string.favorites_loading) else status.text
                    retry.visibility = if (failed && favorites.signedIn) View.VISIBLE else View.GONE
                    if (!failed && !loading) showResultCount()
                } else {
                    adapter.notifyDataSetChanged()
                }
            }
        }
        if (favoritesMode) {
            retry.setOnClickListener { favorites.reload() }
            status.setText(R.string.favorites_loading)
            return
        }

        fun showFailure() {
            loading = false
            failed = true
            status.text = if (candidates.isEmpty()) getString(R.string.search_failed)
                else "일부 행사만 불러왔어요. 다시 시도하면 전체 목록을 불러옵니다."
            retry.visibility = View.VISIBLE
        }

        // 받은 행사부터 보여 주고, 전체 조회가 끝날 때까지 진행 상태를 표시함

        fun loadPage(start: Int, collected: MutableList<Experience>, lastRow: Int = API_MAX_COUNT) {
            fun showPage(data: SeoulCultureData?) {
                if (data == null || (data.row.isEmpty() && start <= data.list_total_count)) {
                    showFailure()
                    return
                }
                collected.addAll(data.familyExperiences())
                candidates = collected.distinctBy { listOf(it.title, it.location, it.date) }
                adapter.updateData(candidates)
                status.text = "체험·행사를 불러오는 중이에요… (${candidates.size}개 표시)"
                val next = start + data.row.size
                val lastAvailableRow = minOf(data.list_total_count, API_MAX_COUNT)
                val hasNext = next <= lastAvailableRow && data.row.isNotEmpty()
                ExperienceCandidateCache.publish(candidates, complete = !hasNext)
                if (hasNext) {
                    loadPage(next, collected, lastAvailableRow)
                } else {
                    loading = false
                    showResultCount()
                }
            }

            if (start == 1) {
                firstPageJob = viewLifecycleOwner.lifecycleScope.launch {
                    showPage(SeoulCultureFirstPage.load())
                }
                return
            }

            val end = minOf(start + API_PAGE_SIZE - 1, lastRow)
            val call = SeoulCultureRetrofit.apiService.getCultureEvents(BuildConfig.SEOUL_API_KEY, start, end)
            activeCall = call
            call.enqueue(object : Callback<SeoulCultureResponse> {
                override fun onResponse(call: Call<SeoulCultureResponse>, response: Response<SeoulCultureResponse>) {
                    if (activeCall !== call || this@ExperienceListFragment.view !== view) return
                    showPage(response.body()?.aiViewCulture?.takeIf { response.isSuccessful })
                }
                override fun onFailure(call: Call<SeoulCultureResponse>, error: Throwable) {
                    if (activeCall === call && this@ExperienceListFragment.view === view) showFailure()
                }
            })
        }


        fun loadEvents() {
            firstPageJob?.cancel()
            activeCall?.cancel()
            activeCall = null
            candidates = emptyList()
            adapter.updateData(candidates)
            loading = true
            failed = false
            retry.visibility = View.GONE
            status.setText(R.string.search_loading)
            if (BuildConfig.SEOUL_API_KEY.isBlank()) {
                failed = true
                loading = false
                status.setText(R.string.search_key_missing)
                return
            }
            loadPage(1, mutableListOf())
        }
        retry.setOnClickListener { loadEvents() }
        val cached = ExperienceCandidateCache.completed
        if (cached == null) {
            loadEvents()
        } else {
            candidates = cached
            loading = false
            adapter.updateData(candidates)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        view?.findViewById<EditText>(R.id.editSearch)?.let { it.setText(it.text.toString()) }
    }

    override fun onDestroyView() {
        firstPageJob?.cancel()
        firstPageJob = null
        activeCall?.cancel()
        activeCall = null
        view?.findViewById<RecyclerView>(R.id.recyclerExperience)?.adapter = null
        super.onDestroyView()
    }
}

internal fun SeoulCultureEvent.toExperience(): Experience = Experience(
    title = TITLE,
    location = PLACE,
    date = DATE,
    startDate = ExperiencePeriod.parseApiDate(STRTDATE),
    endDate = ExperiencePeriod.parseApiDate(END_DATE),
    category = when (CODENAME) {
        "교육/체험" -> if (listOf("교육", "강좌", "북토크").any { TITLE.contains(it, ignoreCase = true) }) "교육" else "체험"
        "축제-자연/경관" -> "자연"
        "전시/미술", "공연", "콘서트", "뮤지컬/오페라", "연극", "클래식", "무용",
        "독주/독창회", "국악", "영화", "축제-기타" -> "문화/예술"
        "축제-관광/체육" -> "체험"
        else -> "기타"
    },
    imageUrl = MAIN_IMG,
    targetAge = USE_TRGT,
    region = GUNAME,
    coordinates = EventCoordinates.fromApi(LAT, LOT),
    feeLabel = IS_FREE?.trim()?.takeIf { it.isNotEmpty() } ?: USE_FEE.orEmpty().trim(),
    sourceCategory = CODENAME.trim()
)
