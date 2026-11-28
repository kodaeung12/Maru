package com.project.maru

import com.project.maru.experience.data.SeoulCultureEvent
import com.project.maru.experience.list.ExperienceCategory
import com.project.maru.experience.list.toExperience
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperienceCategoryTest {
    @Test fun matchesHomesFourCategoriesUsingSeoulClassification() {
        assertTrue(ExperienceCategory.CULTURE.matches(event("전시/미술")))
        assertTrue(ExperienceCategory.CULTURE.matches(event("축제-문화/예술")))
        assertFalse(ExperienceCategory.CULTURE.matches(event("축제-기타")))
        assertTrue(ExperienceCategory.NATURE.matches(event("축제-자연/경관")))
        assertTrue(ExperienceCategory.FESTIVAL.matches(event("축제-전통/역사")))
        assertTrue(ExperienceCategory.FESTIVAL.matches(event("축제-자연/경관")))
        assertFalse(ExperienceCategory.FESTIVAL.matches(event("콘서트")))
        assertFalse(ExperienceCategory.FESTIVAL.matches(event("기타", "축제 이름의 공연")))
        assertTrue(ExperienceCategory.EDUCATION.matches(event("교육/체험", "어린이 교육")))
        assertFalse(ExperienceCategory.EDUCATION.matches(event("교육/체험", "도자기 만들기")))
    }

    private fun event(category: String, title: String = "행사") =
        SeoulCultureEvent(title, "2026-11-26", "체험관", category, "종로구", "누구나", "").toExperience()
}
