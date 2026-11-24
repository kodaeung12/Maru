package com.project.maru

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentPolicyTest {
    @Test fun blocksRegisteredKoreanAbuseAndCommonSeparators() {
        listOf("씨발", "씨 발", "개.새.끼", "병*신", "ㅅㅂ", "ㅂ ㅅ", "시발!", "이런 씨발 글").forEach {
            assertTrue(it, ContentPolicy.containsAbuse(it))
        }
    }
    @Test fun blocksEnglishAbuseRegardlessOfCase() {
        listOf("FUCK", "fucking bad", "what the SHIT").forEach {
            assertTrue(it, ContentPolicy.containsAbuse(it))
        }
    }
    @Test fun allowsOrdinaryParentingTextAndUnrelatedSubstrings() {
        listOf("아이와 함께 즐거운 체험이에요", "씨앗 심기", "새끼 고양이 돌보기", "변화의 시발점", "Shiitake mushroom", "").forEach {
            assertFalse(it, ContentPolicy.containsAbuse(it))
        }
    }
}
