package com.project.maru

// 서버 firestore.rules의 cleanText와 동일한 목록을 사용한다.
// 단어 기반 필터이므로 문맥 판단이나 모든 변형 욕설을 보장하지 않는다.
internal object ContentPolicy {
    const val MESSAGE = "욕설이나 비하 표현이 포함되어 있어요. 내용을 수정해 주세요."
    private val blockedWords = Regex(
        """씨[ ._*·-]*발|개[ ._*·-]*새[ ._*·-]*끼|병[ ._*·-]*신|미[ ._*·-]*친[ ._*·-]*놈|미[ ._*·-]*친[ ._*·-]*년|좆|ㅅ[ ._*·-]*ㅂ|ㅂ[ ._*·-]*ㅅ|(^|[^가-힣])시[ ._*·-]*발($|[^가-힣])|(^|[^a-z])fuck(ing|er|ers)?($|[^a-z])|(^|[^a-z])shit($|[^a-z])""",
        RegexOption.IGNORE_CASE
    )

    fun containsAbuse(text: String): Boolean = blockedWords.containsMatchIn(text)
}
    