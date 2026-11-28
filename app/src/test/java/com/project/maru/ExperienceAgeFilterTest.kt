package com.project.maru

import com.project.maru.experience.list.isSuitableForAge5To8
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperienceAgeFilterTest {
    @Test fun acceptsOverlappingAges() {
        listOf("누구나", "전 연령", "5세 이상", "7세 이상", "8세 이상", "5~8세", "5세~8세",
            "8세 이하", "6~12세", "3~6세", "만 7세 초과", "만 6세 미만",
            "7세 이상 관람 가능 (2019년 이전 출생자)", "어린이~성인 누구나", "유아, 아동·청소년, 성인 누구나").forEach {
            assertTrue(it, isSuitableForAge5To8(it))
        }
    }

    @Test fun rejectsUnsupportedAges() {
        listOf("4세 이하", "9세 이상", "10~15세", "성인 대상", "성인 누구나",
            "만 8세 초과", "만 5세 미만", "", "서울도서관 회원").forEach {
            assertFalse(it, isSuitableForAge5To8(it))
        }
    }
}
