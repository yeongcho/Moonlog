package com.example.guru2_android_team04_android.domain

import android.database.sqlite.SQLiteDatabase
import com.example.guru2_android_team04_android.data.db.AppDb
import com.example.guru2_android_team04_android.data.db.DiaryDao

// BadgeEngine : 배지 지급 규칙을 실행하는 도메인 클래스
// 용도:
// - 사용자의 일기 작성 현황(누적 개수/연속 작성/감정 종류 등)을 계산해서, 조건을 만족하면 배지를 지급한다.
// - DB에 배지 마스터(badges)와 사용자 배지(user_badges)가 있기 때문에, 규칙 판단 + 지급 처리를 한 곳에서 관리한다.
// 설계 포인트:
// - UI/Activity에서 직접 SQL을 다루지 않게 하고, "배지 로직"을 domain 레이어로 분리한다.
// - badges 테이블에는 rule_type/rule_value로 조건이 저장되어 있고, 이를 해석해서 지급 여부를 판단한다.
class BadgeEngine(private val db: SQLiteDatabase) {

    // 사용자의 배지 조건을 검사하고, 만족하는 배지는 지급한다.
    // 동작 흐름:
    // 1) DiaryDao를 통해 사용자 통계(누적 작성 수, 연속 작성 수, 감정 종류 수)를 계산한다.
    // 2) badges(배지 마스터) 테이블을 전부 읽는다.
    // 3) 각 배지의 rule_type/rule_value를 해석해서 조건 만족 여부를 판단한다.
    // 4) 만족하면 user_badges에 기록(이미 있으면 중복 지급하지 않음)
    fun checkAndGrant(ownerId: String) {
        val diaryDao = DiaryDao(db)

        // 누적 일기 작성 수(임시 저장 제외): "ENTRY_COUNT_AT_LEAST" 규칙에 사용
        val entryCount = diaryDao.countEntries(ownerId)

        // 최근 기준 연속 작성(streak) 일수: "STREAK_AT_LEAST" 규칙에 사용
        val streak = diaryDao.computeStreakFromLatest(ownerId)

        // 사용한 감정 종류 수(중복 제거): "DISTINCT_MOOD_AT_LEAST" 규칙에 사용
        val distinctMood = diaryDao.distinctMoodCount(ownerId)

        // badges(배지 마스터)를 전부 조회해서 규칙 기반으로 지급한다.
        db.rawQuery(
            "SELECT badge_id, rule_type, rule_value FROM ${AppDb.T.BADGES}", emptyArray()
        ).use { c ->
            while (c.moveToNext()) {
                val badgeId = c.getInt(0)
                val ruleType = c.getString(1)
                val ruleValue = c.getInt(2)

                // rule_type 문자열을 기준으로, 어떤 통계를 비교할지 결정한다.
                // 예외처리) 정의되지 않은 rule_type이 들어오면 false 처리하여 오작동(무조건 지급)을 방지한다.
                val satisfied = when (ruleType) {
                    "ENTRY_COUNT_AT_LEAST" -> entryCount >= ruleValue
                    "STREAK_AT_LEAST" -> streak >= ruleValue
                    "DISTINCT_MOOD_AT_LEAST" -> distinctMood >= ruleValue
                    else -> false
                }

                // 조건 만족 시 user_badges에 기록(중복 방지 포함)
                if (satisfied) grantIfNot(ownerId, badgeId)
            }
        }
    }

    // 이미 지급된 배지가 아니면 user_badges에 배지 획득 기록을 추가한다.
    // 목적:
    // - 같은 배지가 여러 번 지급되는 것을 방지한다.
    // - user_badges는 (owner_id, badge_id)가 PK이므로 논리적으로도 "중복 지급 불가" 구조
    private fun grantIfNot(ownerId: String, badgeId: Int) {

        // 해당 사용자가 이미 이 배지를 갖고 있는지 확인
        val exists = db.rawQuery(
            "SELECT 1 FROM ${AppDb.T.USER_BADGES} WHERE owner_id=? AND badge_id=? LIMIT 1",
            arrayOf(ownerId, badgeId.toString())
        ).use { it.moveToFirst() }

        // 없으면 INSERT로 지급(earned_at은 배지를 획득한 시각 기록)
        if (!exists) {
            db.execSQL(
                """
                INSERT INTO ${AppDb.T.USER_BADGES}(owner_id, badge_id, earned_at, is_selected)
                VALUES(?, ?, ?, 0)
                """.trimIndent(), arrayOf(ownerId, badgeId, System.currentTimeMillis())
            )
        }
    }

    // 프로필에 표시할 "대표 배지"를 선택한다.
    // 동작:
    // 1) 해당 사용자(ownerId)의 모든 배지 is_selected를 0으로 초기화한다.
    // 2) 선택한 badgeId만 is_selected=1로 설정한다.
    // 설계:
    // - 대표 배지는 한 개만 유지해야 하므로, 일괄 초기화 후 하나만 1로 설정
    fun selectBadge(ownerId: String, badgeId: Int) {

        // 한 사용자당 대표 배지는 하나만 유지
        db.execSQL(
            "UPDATE ${AppDb.T.USER_BADGES} SET is_selected=0 WHERE owner_id=?", arrayOf(ownerId)
        )

        db.execSQL(
            "UPDATE ${AppDb.T.USER_BADGES} SET is_selected=1 WHERE owner_id=? AND badge_id=?",
            arrayOf(ownerId, badgeId)
        )
    }
}
