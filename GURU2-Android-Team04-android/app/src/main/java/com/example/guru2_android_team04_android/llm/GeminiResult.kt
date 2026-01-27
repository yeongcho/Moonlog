package com.example.guru2_android_team04_android.llm

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// GeminiResult : Gemini API 응답을 앱 내부 모델로 정리한 결과 DTO
// 용도:
// - AppService.runAnalysis()에서 LLM 분석 결과를 받아 AiAnalysis로 저장/표시할 때 사용한다.
// - UI(MindCardDetail 등)에서 필요한 필드(요약/트리거/미션/해시태그/전체 텍스트)를 한 번에 전달한다.
// 설계:
// - actions는 "미션" 성격의 행동 제안 목록이며, 앱 UI는 3개를 기본으로 기대한다.
data class GeminiResult(
    val summary: String, val triggerPattern: String, val actions: List<String>,     // 1~3
    val hashtags: List<String>,    // 0~N
    val missionSummary: String,    // 1줄
    val fullText: String
)

// MonthlyGeminiResult : 월간 요약 화면(activity_monthly_summary.xml)에 필요한 결과 DTO
data class MonthlyGeminiResult(
    val oneLineSummary: String,
    val detailSummary: String,
    val emotionFlow: String,
    val keywords: List<String> // 0~3
)

// GeminiClient : Gemini API 호출을 담당하는 네트워크 클라이언트(LLM 연동 계층)
// 용도:
// - 일기 텍스트를 Gemini에 전송하고, JSON 형태의 분석 결과를 받아 앱 모델로 변환한다.
// 동작 흐름:
// 1) buildPrompt()로 "JSON만 출력"하도록 강제한 프롬프트를 만든다.
// 2) Gemini API 요청 바디(JSON)를 구성해 OkHttp로 POST 요청한다.
// 3) 응답(raw JSON)에서 실제 텍스트를 extractTextFromGeminiResponse()로 뽑는다.
// 4) parseStrictJson()으로 텍스트를 엄격 JSON으로 파싱하여 GeminiResult로 반환한다.
class GeminiClient(
    private val apiKey: String, private val endpointUrl: String
) {

    // OkHttpClient를 싱글 인스턴스로 재사용하여 연결/스레드 자원을 효율적으로 관리한다.
    // - connectTimeout: 연결까지 최대 20초
    // - readTimeout: 응답 본문 수신까지 최대 40초
    private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS).build()

    // 일기 분석 요청(동기 호출)
    // 입력:
    // - moodLabel: 사용자가 선택한 감정 라벨(문자열)
    // - tags: 사용자가 선택한 태그 목록
    // - diaryText: 일기 본문
    // 반환:
    // - GeminiResult(앱에서 쓰기 좋은 구조로 정제된 분석 결과)
    // 예외처리) HTTP 실패(2xx 아님) 시 RuntimeException을 던져 호출부(AppService)에서 에러로 처리하게 한다.
    fun analyzeDiary(
        moodLabel: String, tags: List<String>, diaryText: String
    ): GeminiResult {

        val prompt = buildPrompt(moodLabel, tags, diaryText)

        // Gemini API 요청 포맷에 맞춰 contents/parts 구조로 텍스트 프롬프트를 담는다.
        val bodyJson = JSONObject().apply {
            put(
                "contents", JSONArray().put(
                    JSONObject().put(
                        "parts", JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
        }

        // API Key는 헤더(x-goog-api-key)로 전달한다.
        // Content-Type은 JSON으로 설정한다.
        val req = Request.Builder().url(endpointUrl).addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json").post(
                bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            ).build()

        // 동기 네트워크 호출(백그라운드 스레드에서 실행되어야 함)
        http.newCall(req).execute().use { resp ->
            // 예외처리) body가 null일 수 있으므로 orEmpty로 방어한다.
            val raw = resp.body?.string().orEmpty()
            // 예외처리) 서버 오류/인증 실패 등으로 응답이 실패하면 코드 포함 에러 메시지로 예외를 발생시킨다.
            if (!resp.isSuccessful) {
                Log.e("GeminiHTTP", "code=${resp.code} body=${raw.take(2000)}")
                throw RuntimeException("LLM API failed: ${resp.code} body=${raw.take(800)}")
            }


            // Gemini 응답 JSON에서 "모델이 생성한 텍스트" 부분만 추출
            val text = extractTextFromGeminiResponse(raw)

            // 모델이 준 텍스트를 JSON으로 엄격 파싱하여 결과를 만든다.
            return parseStrictJson(text)
        }
    }

    // Gemini에게 "JSON만" 출력하도록 강하게 유도하는 프롬프트를 만든다.
    // 설계:
    // - 출력 스키마를 명시하여 파싱 가능한 구조를 강제한다.
    // - actions / hashtags / full_text 등 UI에서 바로 쓰는 요소를 포함한다.
    private fun buildPrompt(
        moodLabel: String, tags: List<String>, diaryText: String
    ): String {
        return """
        너는 감정 일기 코치야.
        아래 일기를 읽고 반드시 "JSON만" 출력해. 다른 문장은 절대 금지.

        [입력]
        - 사용자가 선택한 기분: $moodLabel
        - 감정 태그: ${tags.joinToString(", ")}
        - 일기:
        ${diaryText.trim()}

        [출력 JSON 스키마]
        {
          "summary": "오늘 감정 핵심 1~2문장 요약",
          "trigger_pattern": "텍스트에서 추정되는 트리거/패턴 1~2문장",
          "hashtags": ["#키워드1", "#키워드2", "#키워드3"],
          "actions": ["짧고 구체적인 행동 1", "짧고 구체적인 행동 2", "짧고 구체적인 행동 3"],
          "mission_summary": "위 3가지 미션을 아우르는 1줄 문장",
          "full_text": "사용자에게 건네는 따뜻한 분석/코칭(3~6문장, 과한 의학/진단 금지)"
        }

        [강제 규칙]
        - actions는 반드시 3개(부득이하면 최소 1개, 최대 3개)
        - 각 action은 20자 내외로 짧고, 지금 당장 가능한 행동만
        - hashtags는 0~5개, 반드시 #으로 시작
        - 의학적 진단/치료 지시 금지
        - JSON 외 다른 텍스트 출력 금지
        """.trimIndent()
    }

    // Gemini 응답(raw JSON)에서 실제 생성 텍스트를 추출한다.
    // 목적:
    // - Gemini 응답은 candidates/content/parts 구조로 감싸져 있어,
    //   그 안의 "text"만 뽑아내야 parseStrictJson()을 적용할 수 있다.
    //
    // 예외처리) 응답 구조가 예상과 다르면(raw 자체를) 그대로 반환해서
    // 상위에서 파싱 실패로 감지되게 한다.
    private fun extractTextFromGeminiResponse(raw: String): String {
        val root = JSONObject(raw)
        val candidates = root.optJSONArray("candidates") ?: return raw
        val c0 = candidates.optJSONObject(0) ?: return raw
        val content = c0.optJSONObject("content") ?: return raw
        val parts = content.optJSONArray("parts") ?: return raw
        val p0 = parts.optJSONObject(0) ?: return raw
        return p0.optString("text", raw).trim()
    }

    // 모델이 출력한 텍스트(JSON 문자열)를 엄격 파싱하여 GeminiResult로 변환한다.
    // 방어 로직:
    // - 코드블록(```json ... ```)로 감싸서 오는 경우를 제거한다.
    // - actions가 1~3개를 만족하지 않으면 기본값으로 채워 3개로 만든다(UI 안정성).
    // - hashtags는 # 접두사를 강제하고, 최대 5개로 제한한다.
    // - mission_summary가 비어 있으면 기본 문구로 채운다.
    //
    // 예외처리) JSON 파싱에 실패하거나 필수 키(summary/trigger_pattern/full_text)가 없으면 JSONException 발생
    // - 호출부(AppService.runAnalysisSafe)에서 ParseError로 처리할 수 있게 예외를 그대로 위로 올린다.
    private fun parseStrictJson(text: String): GeminiResult {

        val cleaned = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        val obj = JSONObject(cleaned)

        // actions 파싱 및 보정
        val actionsArr = obj.optJSONArray("actions") ?: JSONArray()
        val actions = ArrayList<String>()
        for (i in 0 until actionsArr.length()) {
            actions.add(actionsArr.getString(i))
        }

        val safeActions = when {
            actions.isEmpty() -> listOf("물 한 잔 천천히 마시기", "5분 가볍게 걷기", "숨 3번 길게 내쉬기")

            actions.size >= 3 -> actions.take(3)

            else -> buildList {
                addAll(actions)
                while (size < 3) add("숨 3번 길게 내쉬기")
            }
        }

        // hashtags 파싱 및 보정
        val hashtagsArr = obj.optJSONArray("hashtags") ?: JSONArray()
        val hashtags = ArrayList<String>()
        for (i in 0 until hashtagsArr.length()) {
            val s = hashtagsArr.optString(i, "").trim()
            if (s.isNotBlank()) {
                hashtags.add(if (s.startsWith("#")) s else "#$s")
            }
        }

        // mission_summary 보정
        val missionSummary = obj.optString("mission_summary", "").trim()
        val safeMissionSummary = if (missionSummary.isBlank()) "작게라도 몸과 마음을 돌보는 하루로 만들어봐요."
        else missionSummary

        return GeminiResult(
            summary = obj.getString("summary"),
            triggerPattern = obj.getString("trigger_pattern"),
            actions = safeActions,
            hashtags = hashtags.take(5),
            missionSummary = safeMissionSummary,
            fullText = obj.getString("full_text")
        )
    }

    // 월간 요약 생성 요청
    // 입력:
    // - yearMonth: "YYYY-MM"
    // - dominantMoodLabel: 최빈 감정 라벨(문자열)
    // - entriesBrief: 월 일기 요약 입력(길이 제한된 텍스트)
    fun summarizeMonth(
        yearMonth: String, dominantMoodLabel: String, entriesBrief: String
    ): MonthlyGeminiResult {
        val prompt = buildMonthlyPrompt(yearMonth, dominantMoodLabel, entriesBrief)

        val bodyJson = JSONObject().apply {
            put(
                "contents", JSONArray().put(
                    JSONObject().put(
                        "parts", JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
        }

        val req = okhttp3.Request.Builder().url(endpointUrl).addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json").post(
                bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            ).build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("LLM API failed: ${resp.code}")
            val raw = resp.body?.string().orEmpty()
            val text = extractTextFromGeminiResponse(raw)
            return parseMonthlyStrictJson(text)
        }
    }

    private fun buildMonthlyPrompt(
        yearMonth: String, dominantMoodLabel: String, entriesBrief: String
    ): String {
        return """
        너는 감정 일기 코치야.
        아래 한 달치 일기 요약 입력을 바탕으로 반드시 "JSON만" 출력해. 다른 문장은 절대 금지.

        [입력]
        - 대상 월: $yearMonth
        - 이번 달 최빈 감정: $dominantMoodLabel
        - 월간 일기 요약 입력(날짜/기분/태그/내용 요약):
        $entriesBrief

        [출력 JSON 스키마]
        {
          "one_line_summary": "한 줄 요약(강조 박스) 1문장",
          "detail_summary": "상세 요약 본문(3~7문장, 줄바꿈 허용)",
          "emotion_flow": "감정 흐름 한 줄(예: 안정 → 지침 → 회복)",
          "keywords": ["키워드1", "키워드2", "키워드3"]
        }

        [강제 규칙]
        - one_line_summary는 40자 내외 1문장
        - detail_summary는 3~7문장, 과한 의학/진단 금지
        - emotion_flow는 너무 길지 않게(20자 내외) "A → B → C" 형태 추천
        - keywords는 0~3개(가능하면 3개), 중복 금지, 너무 추상적인 단어 금지
        - JSON 외 다른 텍스트 출력 금지
        """.trimIndent()
    }

    private fun parseMonthlyStrictJson(text: String): MonthlyGeminiResult {
        val cleaned = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        val obj = JSONObject(cleaned)

        val oneLine = obj.optString("one_line_summary", "").trim()
        val detail = obj.optString("detail_summary", "").trim()
        val flow = obj.optString("emotion_flow", "").trim()

        val keywordsArr = obj.optJSONArray("keywords") ?: JSONArray()
        val keywords = ArrayList<String>()
        for (i in 0 until keywordsArr.length()) {
            val s = keywordsArr.optString(i, "").trim()
            if (s.isNotBlank()) keywords.add(s)
        }

        return MonthlyGeminiResult(
            oneLineSummary = oneLine,
            detailSummary = detail,
            emotionFlow = flow,
            keywords = keywords.distinct().take(3)
        )
    }
}
