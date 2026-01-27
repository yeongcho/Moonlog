package com.example.guru2_android_team04_android.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.guru2_android_team04_android.data.model.MonthlySummary
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.JsonMini

// MonthlyDao : monthly_summaries 테이블 접근용 DAO
// 목적:
// - 지난달 감정 요약같은 월간 요약 데이터를 DB에 저장/조회한다.
// - ownerId(사용자) + yearMonth(YYYY-MM)를 복합 PK로 관리하여 월별로 1개 요약만 유지한다.
// - 앱에서 지난달 요약을 생성한 뒤 저장해두면, 매번 일기 전체를 다시 집계하지 않아도 빠르게 보여줄 수 있다.
// - 월간 요약 화면(UI)에 필요한 one_line_summary/detail_summary/emotion_flow/keywords_json을 함께 저장/조회한다.
class MonthlyDao(private val db: SQLiteDatabase) {

    // 특정 사용자(ownerId)의 특정 월(yearMonth, "YYYY-MM") 요약을 조회한다.
    // 반환:
    // - 해당 월 요약이 있으면 MonthlySummary 반환
    // - 없으면 null (아직 요약을 생성하지 않았거나, 해당 월에 일기가 없어서 저장된 적이 없는 경우)
    fun get(ownerId: String, yearMonth: String): MonthlySummary? {
        db.rawQuery(
            """
            SELECT owner_id, year_month, dominant_mood,
                   one_line_summary, detail_summary, emotion_flow, keywords_json,
                   updated_at
            FROM ${AppDb.T.MONTHLY}
            WHERE owner_id=? AND year_month=?
            LIMIT 1
            """.trimIndent(), arrayOf(ownerId, yearMonth)
        ).use { c ->

            // 예외처리) 조회 결과가 없으면 null 반환
            if (!c.moveToFirst()) return null

            // DB row -> MonthlySummary 모델 매핑
            // dominant_mood는 Int로 저장되어 있어 enum으로 변환한다.
            return MonthlySummary(
                ownerId = c.getString(0),
                yearMonth = c.getString(1),
                dominantMood = Mood.fromDb(c.getInt(2)),
                oneLineSummary = c.getString(3),
                detailSummary = c.getString(4),
                emotionFlow = c.getString(5),
                keywords = JsonMini.jsonToList(c.getString(6)),
                updatedAt = c.getLong(7)
            )
        }
    }

    // 월간 요약 저장(없으면 INSERT, 있으면 UPDATE)
    // - monthly_summaries는 PRIMARY KEY(owner_id, year_month)이므로 월당 1개만 유지된다.
    // 동작:
    // 1) update를 먼저 시도한다.
    // 2) 업데이트된 row가 없으면(insert 대상) insert를 수행한다.
    fun upsert(summary: MonthlySummary) {

        // ContentValues: DB에 저장할 컬럼/값 구성
        val cv = ContentValues().apply {
            put("owner_id", summary.ownerId)
            put("year_month", summary.yearMonth)
            put("dominant_mood", summary.dominantMood.dbValue)
            put("one_line_summary", summary.oneLineSummary)
            put("detail_summary", summary.detailSummary)
            put("emotion_flow", summary.emotionFlow)
            put("keywords_json", JsonMini.listToJson(summary.keywords.take(3)))
            // updated_at은 저장/갱신 시점마다 최신값으로 갱신한다.
            put("updated_at", System.currentTimeMillis())
        }

        // 1) 먼저 update 시도 (PK(owner_id, year_month) 기준)
        val updated = db.update(
            AppDb.T.MONTHLY,
            cv,
            "owner_id=? AND year_month=?",
            arrayOf(summary.ownerId, summary.yearMonth)
        )

        // 2) 해당 row가 없어서 update가 안 됐으면 insert 수행
        // 예외처리) insert는 실패 시 -1을 반환할 수 있으나,
        // 여기서는 호출부에서 필요 시 예외/결과 처리하도록 둔다.
        if (updated == 0) db.insert(AppDb.T.MONTHLY, null, cv)
    }

    // 지난달 요약을 읽기 쉽게 호출하기 위한 래퍼 함수.
    fun getLastMonth(ownerId: String, lastYm: String): MonthlySummary? = get(ownerId, lastYm)

    // 특정 연도(year)의 월간 요약 목록을 조회한다.
    // - 예: 2025년의 1월~12월 요약을 모두 가져오기
    // 반환:
    // - year_month 오름차순 정렬된 MonthlySummary 리스트
    fun getYear(ownerId: String, year: Int): List<MonthlySummary> {

        // 조회 범위: "YYYY-01" ~ "YYYY-12"
        val start = String.format("%04d-01", year)
        val end = String.format("%04d-12", year)

        val out = ArrayList<MonthlySummary>()

        db.rawQuery(
            """
            SELECT owner_id, year_month, dominant_mood,
                   one_line_summary, detail_summary, emotion_flow, keywords_json,
                   updated_at
            FROM ${AppDb.T.MONTHLY}
            WHERE owner_id=? AND year_month BETWEEN ? AND ?
            ORDER BY year_month ASC
            """.trimIndent(), arrayOf(ownerId, start, end)
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    MonthlySummary(
                        ownerId = c.getString(0),
                        yearMonth = c.getString(1),
                        dominantMood = Mood.fromDb(c.getInt(2)),
                        oneLineSummary = c.getString(3),
                        detailSummary = c.getString(4),
                        emotionFlow = c.getString(5),
                        keywords = JsonMini.jsonToList(c.getString(6)),
                        updatedAt = c.getLong(7)
                    )
                )
            }
        }
        return out
    }
}
