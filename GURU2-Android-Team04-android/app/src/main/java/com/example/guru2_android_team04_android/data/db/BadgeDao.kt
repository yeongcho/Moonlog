package com.example.guru2_android_team04_android.data.db

import android.database.sqlite.SQLiteDatabase
import com.example.guru2_android_team04_android.data.model.Badge
import com.example.guru2_android_team04_android.data.model.BadgeStatus

// BadgeDao: 배지(BADGES) / 사용자 배지(USER_BADGES) 테이블을 조회하는 DAO(Data Access Object)
// - SQLiteDatabase를 직접 받아서 rawQuery로 읽기 전용 쿼리를 수행함
// - 목적 : "배지 목록 + (해당 사용자의 획득 여부/선택 여부)"를 한 번에 가져오는 것
class BadgeDao(private val db: SQLiteDatabase) {

    // 특정 ownerId(ANON_xxx 또는 USER_xxx)의 "전체 배지 상태"를 가져온다.
    // - BADGES: 배지의 마스터 데이터(이름/설명/규칙)
    // - USER_BADGES: 특정 사용자가 배지를 획득했는지/선택했는지 기록
    // - LEFT JOIN을 쓰는 이유: 아직 획득하지 않은 배지도 목록에 포함시키기 위해서
    fun getAllBadgeStatuses(ownerId: String): List<BadgeStatus> {

        // 결과를 담을 리스트
        val out = ArrayList<BadgeStatus>()

        // rawQuery: SELECT 결과를 Cursor로 받는다.
        // - ? 자리에 ownerId가 바인딩됨
        db.rawQuery(
            """
            SELECT 
              b.badge_id, b.name, b.description, b.rule_type, b.rule_value,
              ub.badge_id IS NOT NULL AS is_earned,
              CASE WHEN ub.is_selected=1 THEN 1 ELSE 0 END AS is_selected
            FROM ${AppDb.T.BADGES} b
            LEFT JOIN ${AppDb.T.USER_BADGES} ub
              ON b.badge_id = ub.badge_id AND ub.owner_id = ?
            ORDER BY b.badge_id ASC
            """.trimIndent(), arrayOf(ownerId)
        ).use { c ->

            // Cursor를 한 줄씩 읽으며 BadgeStatus로 변환
            while (c.moveToNext()) {

                // BADGES 테이블에서 온 "배지 마스터 정보"를 Badge 모델로 구성
                val badge = Badge(
                    badgeId = c.getInt(0),
                    name = c.getString(1),
                    description = c.getString(2),
                    ruleType = c.getString(3),
                    ruleValue = c.getInt(4)
                )

                // is_earned: USER_BADGES에 매칭되는 row가 있으면(=NULL이 아니면) 획득한 상태
                // - SQLite에서는 boolean이 없어서 0/1로 다루는 경우가 많음
                val isEarned = c.getInt(5) == 1

                // is_selected: 획득한 배지 중에서 사용자가 프로필 등에 "선택"한 배지인지
                // - ub.is_selected가 1이면 선택 상태
                val isSelected = c.getInt(6) == 1

                // Badge(마스터 정보) + 상태(획득/선택)를 합쳐 BadgeStatus로 저장
                out.add(BadgeStatus(badge, isEarned, isSelected))
            }
        }

        // 전체 배지 상태 리스트 반환(획득/미획득 모두 포함)
        return out
    }

    // 특정 ownerId가 "현재 선택한 배지" 하나를 가져온다.
    // - USER_BADGES에서 is_selected=1 인 row만 찾고
    // - BADGES와 JOIN해서 배지의 상세 정보까지 함께 가져온다.
    // - 선택된 배지가 없으면 null 반환
    fun getSelectedBadge(ownerId: String): Badge? {
        db.rawQuery(
            """
            SELECT b.badge_id, b.name, b.description, b.rule_type, b.rule_value
            FROM ${AppDb.T.BADGES} b
            JOIN ${AppDb.T.USER_BADGES} ub
              ON b.badge_id = ub.badge_id
            WHERE ub.owner_id=? AND ub.is_selected=1
            LIMIT 1
            """.trimIndent(), arrayOf(ownerId)
        ).use { c ->

            // 선택된 배지가 없으면 결과가 비어 있으므로 null 반환
            if (!c.moveToFirst()) return null

            // 선택된 배지의 마스터 정보를 Badge 모델로 변환해 반환
            return Badge(
                badgeId = c.getInt(0),
                name = c.getString(1),
                description = c.getString(2),
                ruleType = c.getString(3),
                ruleValue = c.getInt(4)
            )
        }
    }
}
