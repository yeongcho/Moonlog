package com.example.guru2_android_team04_android.data.db

import android.database.sqlite.SQLiteDatabase
import com.example.guru2_android_team04_android.data.model.MindCardPreview
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.JsonMini
import com.example.guru2_android_team04_android.util.MindCardTextUtil

// FavoriteDao : 즐겨찾기(하트) 기능을 위한 DAO.
// - 즐겨찾기 여부 변경과 즐겨찾기 카드 목록 조회를 담당한다.
// - ownerId + entryId 소유자 검증을 먼저 수행하여 다른 사용자의 데이터를 변경하지 못하도록 방어한다.
class FavoriteDao(private val db: SQLiteDatabase) {

    // entryId가 해당 ownerId의 소유인지 확인한다.
    // 목적:
    // - 다른 사용자의 entryId를 임의로 넘겨 즐겨찾기 변경하는 것을 방지
    private fun isOwnedBy(ownerId: String, entryId: Long): Boolean {
        return db.rawQuery(
            "SELECT 1 FROM ${AppDb.T.ENTRIES} WHERE owner_id=? AND entry_id=? LIMIT 1",
            arrayOf(ownerId, entryId.toString())
        ).use { it.moveToFirst() }
    }

    // 즐겨찾기(하트) 상태를 설정한다.
    // 동작:
    // 1) 소유자 검증 실패 시 false
    // 2) is_favorite 값을 0/1로 업데이트
    fun set(ownerId: String, entryId: Long, favorite: Boolean): Boolean {

        // 예외처리) 소유자가 아니면 즐겨찾기 변경을 허용하지 않는다.
        if (!isOwnedBy(ownerId, entryId)) return false

        val cv = android.content.ContentValues().apply {
            put("is_favorite", if (favorite) 1 else 0)
        }

        val updated = db.update(
            AppDb.T.ENTRIES, cv, "owner_id=? AND entry_id=?", arrayOf(ownerId, entryId.toString())
        )

        // 예외처리) updated가 0이면 조건에 맞는 row가 없거나 업데이트가 반영되지 않은 상태
        return updated > 0
    }

    // 특정 일기가 즐겨찾기(하트) 상태인지 확인한다.
    fun isFavorite(ownerId: String, entryId: Long): Boolean {
        return db.rawQuery(
            "SELECT is_favorite FROM ${AppDb.T.ENTRIES} WHERE owner_id=? AND entry_id=? LIMIT 1",
            arrayOf(ownerId, entryId.toString())
        ).use { c ->

            // 예외처리) row가 없으면 false 처리
            if (!c.moveToFirst()) false else c.getInt(0) == 1
        }
    }

    // 즐겨찾기(하트)된 일기 목록을 "마음 카드" 형태로 가져온다.
    // 반환 모델:
    // - UI 공통 카드 모델(MindCardPreview)로 통일한다.
    // 처리 방식:
    // - diary_entries + ai_analysis를 LEFT JOIN하여 분석이 없는 경우도 목록은 보여준다.
    // - comfortPreview에는 full_text를 넣는다(분석이 없으면 '분석 전').
    // - mission은 actions_json의 첫 번째 항목을 사용하고, 없으면 기본 미션을 사용한다.
    fun getFavoriteMindCards(ownerId: String): List<MindCardPreview> {
        val out = ArrayList<MindCardPreview>()

        db.rawQuery(
            """
            SELECT 
              e.entry_id,
              e.date_ymd,
              e.title,
              e.mood,
              e.tags_json,
              COALESCE(a.full_text, '') AS full_text,
              COALESCE(a.actions_json, '[]') AS actions_json
            FROM ${AppDb.T.ENTRIES} e
            LEFT JOIN ${AppDb.T.ANALYSIS} a
              ON e.entry_id = a.entry_id
            WHERE e.owner_id=? AND e.is_favorite=1
            ORDER BY e.updated_at DESC
            """.trimIndent(), arrayOf(ownerId)
        ).use { c ->
            while (c.moveToNext()) {
                val entryId = c.getLong(0)
                val dateYmd = c.getString(1)
                val title = c.getString(2)
                val mood = Mood.fromDb(c.getInt(3))
                val tags = JsonMini.jsonToList(c.getString(4))

                val fullText = c.getString(5).orEmpty().trim()

                val comfortPreview =
                    if (fullText.isBlank()) "" else MindCardTextUtil.makePreview(fullText)

                val actions = JsonMini.jsonToList(c.getString(6))
                val mission = actions.firstOrNull()?.takeIf { it.isNotBlank() } ?: "천천히 숨 고르기"

                out.add(
                    MindCardPreview(
                        entryId = entryId,
                        dateYmd = dateYmd,
                        title = title,
                        mood = mood,
                        tags = tags,
                        comfortPreview = comfortPreview,
                        mission = mission
                    )
                )
            }
        }
        return out
    }
}
