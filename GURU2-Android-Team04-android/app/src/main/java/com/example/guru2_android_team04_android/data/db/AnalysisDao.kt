package com.example.guru2_android_team04_android.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.guru2_android_team04_android.data.model.AiAnalysis
import com.example.guru2_android_team04_android.util.JsonMini

// AnalysisDao : AI 분석 결과(ai_analysis 테이블)에 접근하는 DAO(Data Access Object)
// 일기 1개(entry_id)당 AI 분석 1개 구조이므로, ai_analysis는 entry_id를 UNIQUE로 갖고(중복 불가) 분석을 캐싱한다.
class AnalysisDao(private val db: SQLiteDatabase) {

    // 특정 일기(entryId)에 대한 AI 분석 결과를 1개 조회한다.
    // - 분석이 없으면 null 반환 (아직 Gemini 분석을 안 했거나 실패한 경우)
    // 동작:
    // 1) entry_id로 ai_analysis 테이블에서 row를 조회
    // 2) actions_json / hashtags_json은 JSON 문자열이므로 List<String>으로 변환
    fun getByEntryId(entryId: Long): AiAnalysis? {

        // 예외처리) Cursor 사용 후 반드시 닫혀야 하므로 use { }로 감싼다.
        // 예외가 발생해도 use 블록이 cursor close를 보장한다.
        db.rawQuery(
            """
            SELECT analysis_id, entry_id, summary, trigger_pattern, actions_json, hashtags_json, mission_summary, full_text, created_at
            FROM ${AppDb.T.ANALYSIS}
            WHERE entry_id=?
            LIMIT 1
            """.trimIndent(), arrayOf(entryId.toString())
        ).use { c ->

            // 예외처리) 조회 결과가 없으면 null 반환
            if (!c.moveToFirst()) return null

            // DB row -> AiAnalysis 모델로 매핑
            // actions/hashtags는 JSON 문자열 형태로 저장되어 있어 JsonMini로 역직렬화한다.
            return AiAnalysis(
                analysisId = c.getLong(0),
                entryId = c.getLong(1),
                summary = c.getString(2),
                triggerPattern = c.getString(3),
                actions = JsonMini.jsonToList(c.getString(4)),
                hashtags = JsonMini.jsonToList(c.getString(5)),
                missionSummary = c.getString(6),
                fullText = c.getString(7),
                createdAt = c.getLong(8)
            )
        }
    }

    // AI 분석 결과를 저장한다. (없으면 insert, 있으면 update)
    // 반환값:
    // - 저장된 analysis_id를 반환한다.
    // - update 성공 시 기존 analysis_id를 찾아 반환한다.
    // - insert 성공 시 insert가 반환한 rowId(analysis_id)를 반환한다.
    // - update는 성공했는데 id 조회가 실패하면 -1L
    fun upsert(analysis: AiAnalysis): Long {

        // ContentValues: 컬럼명 -> 값 형태로 DB에 넣을 데이터를 구성
        // actions/hashtags는 List<String>이므로 JSON 문자열로 직렬화해서 저장한다.
        val cv = ContentValues().apply {
            put("entry_id", analysis.entryId)
            put("summary", analysis.summary)
            put("trigger_pattern", analysis.triggerPattern)
            put("actions_json", JsonMini.listToJson(analysis.actions))
            put("hashtags_json", JsonMini.listToJson(analysis.hashtags))
            put("mission_summary", analysis.missionSummary)
            put("full_text", analysis.fullText)

            // created_at은 "분석 생성/갱신 시각"으로 사용한다.
            put("created_at", System.currentTimeMillis())
        }

        // 1) 먼저 entry_id 기준으로 update 시도
        // 예외처리) entry_id가 존재하지 않으면 updated=0이 나오며 insert로 넘어간다.
        val updated = db.update(
            AppDb.T.ANALYSIS, cv, "entry_id=?", arrayOf(analysis.entryId.toString())
        )

        // 2) update가 되었으면 해당 entry_id의 analysis_id를 다시 조회해 반환
        if (updated > 0) return getIdByEntryId(analysis.entryId) ?: -1L

        // 3) update가 안 됐으면 insert 수행
        // 예외처리) insert 실패 시 -1L을 반환할 수 있다(SQLite insert 규약)
        return db.insert(AppDb.T.ANALYSIS, null, cv)
    }

    // 내부 전용: entry_id로 analysis_id(기본키)만 조회한다.
    // upsert에서 update 성공 시 analysis_id를 반환하기 위해 사용한다.
    private fun getIdByEntryId(entryId: Long): Long? {
        db.rawQuery(
            "SELECT analysis_id FROM ${AppDb.T.ANALYSIS} WHERE entry_id=? LIMIT 1",
            arrayOf(entryId.toString())
        ).use { c ->

            // 예외처리) 조회 결과가 없으면 null 반환
            return if (c.moveToFirst()) c.getLong(0) else null
        }
    }
}
