package com.example.guru2_android_team04_android.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

// SettingsDao : settings 테이블 접근용 DAO
// 목적:
// - 사용자(ownerId)별 앱 설정 값을 저장/조회/삭제한다.
// - 서버 없이 로컬 DB만 사용하는 구조에서 "사용자 설정 저장소" 역할을 한다.
// - 예: 프로필 이미지 URI, 앱 옵션 값, 사용자별 환경 설정 등
// 설계:
// - settings 테이블은 (owner_id, setting_key)를 복합 PRIMARY KEY로 사용한다.
// - 같은 설정(setting_key)은 사용자(ownerId)당 1개만 존재한다.
class SettingsDao(private val db: SQLiteDatabase) {

    // 특정 사용자(ownerId)의 특정 설정(setting_key) 값을 조회한다.
    // 반환:
    // - 설정이 존재하면 value(String) 반환
    // - 존재하지 않으면 null 반환
    fun get(ownerId: String, key: String): String? {
        return db.rawQuery(
            """
            SELECT value
            FROM ${AppDb.T.SETTINGS}
            WHERE owner_id=? AND setting_key=?
            LIMIT 1
            """.trimIndent(), arrayOf(ownerId, key)
        ).use { c ->
            // 예외처리) 해당 설정이 없으면 null 반환
            if (!c.moveToFirst()) null else c.getString(0)
        }
    }

    // 설정 값 저장(없으면 INSERT, 있으면 UPDATE)
    // - 복합 PRIMARY KEY(owner_id, setting_key)에 의해 사용자별 설정 1개만 유지된다.
    // 동작:
    // 1) update를 먼저 시도한다.
    // 2) 업데이트된 row가 없으면(insert 대상) insert를 수행한다.
    fun upsert(ownerId: String, key: String, value: String) {

        // ContentValues: DB에 저장할 컬럼/값 구성
        // 예외처리) DB 컬럼명은 setting_key이므로 put("setting_key", key)로 저장해야 한다.
        val cv = ContentValues().apply {
            put("owner_id", ownerId)
            put("setting_key", key)
            put("value", value)
        }

        // 1) 먼저 update 시도
        val updated = db.update(
            AppDb.T.SETTINGS, cv, "owner_id=? AND setting_key=?", arrayOf(ownerId, key)
        )

        // 2) 해당 row가 없으면 insert 수행
        // 예외처리) insert 실패 시 -1을 반환할 수 있으나,
        // 이 DAO에서는 호출부에서 필요 시 재시도/오류 처리하도록 책임을 넘긴다.
        if (updated == 0) db.insert(AppDb.T.SETTINGS, null, cv)
    }

    // 특정 사용자(ownerId)의 특정 설정(setting_key)을 삭제한다.
    // 반환:
    // - 삭제된 row 수 (1이면 성공, 0이면 해당 설정이 존재하지 않음)
    fun delete(ownerId: String, key: String): Int {
        return db.delete(
            AppDb.T.SETTINGS, "owner_id=? AND setting_key=?", arrayOf(ownerId, key)
        )
    }

    companion object {

        // settings 테이블에서 사용하는 대표적인 설정 key 상수
        // - 프로필 이미지 URI 저장용
        // - SettingsDao.KEY_PROFILE_IMAGE_URI 형태로 사용하여 문자열 오타로 인한 버그를 방지한다.
        const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
    }
}
