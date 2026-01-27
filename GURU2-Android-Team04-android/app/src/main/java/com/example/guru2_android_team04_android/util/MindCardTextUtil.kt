package com.example.guru2_android_team04_android.util

// MindCardTextUtil : AI 분석 결과 텍스트에서 마음 카드 짧은 문장을 생성하는 텍스트 가공 유틸리티
// 용도:
// - AI가 생성한 긴 분석 텍스트(fullText)를 그대로 카드에 노출하면 UI가 과도하게 길어지므로, 앞부분의 핵심 문장만 잘라서 보여주기 위해 사용한다.
// 설계:
// - 문장 수 제한 + 글자 수 제한을 동시에 적용하여 다양한 길이의 AI 응답에도 UI가 안정적으로 유지되도록 한다.
object MindCardTextUtil {

    // fullText에서 앞쪽 1~2문장만 잘라 프리뷰 문자열을 생성한다.
    // 파라미터:
    // - fullText: AI 분석 전체 텍스트
    // - maxSentences: 최대 문장 수 (기본 2문장)
    // - maxChars: 최대 글자 수 (기본 90자)
    // 반환:
    // - 조건에 맞게 잘린 프리뷰 문자열
    // - 필요 시 말줄임표(…)를 붙인다.
    fun makePreview(
        fullText: String,
        maxSentences: Int = 2,
        maxChars: Int = 90
    ): String {

        // 앞뒤 공백 제거
        val cleaned = fullText.trim()
        // 예외처리) 입력 텍스트가 비어 있으면 빈 문자열 반환
        if (cleaned.isEmpty()) return ""
        // 문장 분리 기준 정의
        // - 줄바꿈
        // - 영어 문장 종결 기호(. ! ?)
        // - 한국어 종결 표현("다.", "요.")
        // Regex를 사용해 "문장 경계" 기준으로 분리한다.
        val parts = cleaned.split(
            Regex("(?<=\\n)|(?<=[.!?])\\s+|(?<=다\\.)\\s+|(?<=요\\.)\\s+")
        )
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 문장을 하나씩 이어 붙이며
        // - 최대 문장 수
        // - 최대 글자 수
        // 두 조건을 동시에 만족하도록 구성한다.
        val sb = StringBuilder()
        var count = 0

        for (p in parts) {
            if (count >= maxSentences) break

            // 현재까지 만든 문장 뒤에 새 문장을 붙였을 때의 후보 문자열
            val next = if (sb.isEmpty()) p else "${sb} $p"

            // 글자 수 제한 초과 시 중단
            if (next.length > maxChars) {
                // 예외처리) 첫 문장 하나만으로도 maxChars를 초과하면 강제로 잘라서 반환한다.
                if (sb.isEmpty()) {
                    return p.take(maxChars).trimEnd() + "…"
                }
                break
            }

            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(p)
            count++
        }

        val preview = sb.toString().trim()

        // 예외처리) 최종 결과가 글자 수 제한을 넘는 경우 말줄임 처리
        if (preview.length > maxChars) {
            return preview.take(maxChars).trimEnd() + "…"
        }
        return preview
    }

    fun splitTwoLines(text: String): Pair<String, String> {
        val t = text.trim()
        if (t.isBlank()) return "" to ""
        val parts = t.split("\n", ". ", "。", "!", "?", "…")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return parts.getOrNull(0).orEmpty() to parts.getOrNull(1).orEmpty()
    }

    fun makeComfortLines(nickname: String, comfortPreview: String?): Pair<String, String> {
        val (l1, l2) = splitTwoLines(comfortPreview.orEmpty())
        val line1 = "${nickname}님, ${l1.ifBlank { "오늘도 기록해줘서 고마워요." }}"
        val line2 = l2.ifBlank { "지금은 충분히 잘하고 있어요." }
        return line1 to line2
    }

}
