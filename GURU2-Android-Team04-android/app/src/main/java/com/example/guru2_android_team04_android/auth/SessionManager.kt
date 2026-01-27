package com.example.guru2_android_team04_android.auth

import android.content.Context
import java.util.UUID

// SessionManager : 앱의 "현재 사용자 세션"을 관리하는 클래스
// 이 앱은 로그인 사용자(USER_xxx)와 비회원 사용자(ANON_xxx)를 모두 지원하므로, 현재 사용자가 누구인지(ownerId)를 항상 추적
// 목적:
//  해당 클래스는 SharedPreferences를 이용해
//  1) 현재 세션의 ownerId
//  2) 익명 사용자 고유 ID
//  3) 익명 사용자 최초 생성 시각을 로컬에 영구 저장/관리한다.
// 서버 없이 SQLite 기반 앱이므로 ownerId는 모든 데이터(일기, 배지 등)의 "소유자 키" 역할을 한다.
class SessionManager(context: Context) {

    // SharedPreferences 객체
    // - 앱 내부 저장소에 "session" 이라는 이름으로 저장된다.
    // - 앱 종료/재실행 이후에도 값이 유지된다.
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    // 현재 세션의 ownerId를 반환한다.
    // 반환값 예:
    // - "USER_3"   : 로그인한 회원
    // - "ANON_xxx" : 비회원(익명 사용자)
    // - null       : 아직 세션이 시작되지 않은 상태
    fun currentOwnerId(): String? = prefs.getString(KEY_OWNER, null)

    // 현재 세션의 ownerId를 저장한다.
    // 사용 시점:
    // - 로그인 성공 시
    // - 회원가입 직후
    // - 비회원 세션 시작 시
    fun setOwnerId(ownerId: String) {
        prefs.edit().putString(KEY_OWNER, ownerId).apply()
    }

    // 현재 세션(ownerId)만 제거한다.
    // 사용 시점:
    // - 로그아웃
    // 주의:
    // - 익명 사용자 정보(KEY_ANON, KEY_ANON_CREATED_AT)는 삭제하지 않는다.
    // - 따라서 로그아웃 후 다시 비회원으로 시작하면 이전과 동일한 ANON_xxx가 재사용된다.
    fun clear() {
        prefs.edit().remove(KEY_OWNER).apply()
    }

    // 익명 사용자(ownerId)를 가져오거나, 아직 없다면 새로 생성해서 저장한 뒤 반환한다.
    //
    // 동작 흐름:
    // 1. SharedPreferences에 기존 익명 ID가 있으면 그대로 반환
    // 2. 없다면 UUID를 이용해 새로운 ANON_xxx 생성
    // 3. 생성된 ID를 로컬에 저장
    fun getOrCreateAnonOwnerId(): String {
        val existing = prefs.getString(KEY_ANON, null)
        if (existing != null) return existing

        // ANON_ 접두사를 붙여
        // 회원(USER_xxx)과 명확히 구분되는 ownerId 생성
        val newId = "ANON_${UUID.randomUUID()}"

        // 익명 사용자 ID 저장
        // 앱 재실행 이후에도 동일한 익명 사용자로 인식된다.
        prefs.edit().putString(KEY_ANON, newId).apply()

        return newId
    }

    // 익명 사용자가 "처음 앱을 사용하기 시작한 시각"을 반환한다.
    //
    // 목적:
    // - 서비스 이용 일수(D+N) 계산
    // - 비회원도 며칠째 사용 중인지 보여주기 위함
    //
    // 동작 흐름:
    // 1. 이미 저장된 시각이 있으면 그대로 반환
    // 2. 없으면 현재 시각(System.currentTimeMillis())을 저장 후 반환
    fun getOrCreateAnonCreatedAt(): Long {
        val existing = prefs.getLong(KEY_ANON_CREATED_AT, -1L)
        if (existing > 0) return existing

        // 현재 시각(ms 단위)
        val now = System.currentTimeMillis()

        // 익명 사용자 최초 생성 시각 저장
        prefs.edit().putLong(KEY_ANON_CREATED_AT, now).apply()

        return now
    }

    companion object {
        // SharedPreferences Key
        // 현재 세션의 ownerId
        // - USER_xxx 또는 ANON_xxx
        private const val KEY_OWNER = "current_owner_id"

        // 익명 사용자에게 부여된 고유 ownerId
        // - 앱 삭제 전까지 유지됨
        private const val KEY_ANON = "anon_owner_id"

        // 익명 사용자가 처음 생성된 시각(ms)
        // - 서비스 이용 일수 계산에 사용
        private const val KEY_ANON_CREATED_AT = "anon_created_at"
    }
}
