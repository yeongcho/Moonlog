package com.example.guru2_android_team04_android.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.guru2_android_team04_android.data.model.User

// UserDao : users 테이블 접근용 DAO.
// 목적:
// - 회원(User) 정보를 DB에 저장/조회/수정/삭제하는 역할을 한다.
// - 회원가입, 로그인, 프로필 수정, 회원 탈퇴 로직에서 사용된다.
// users 테이블:
// - user_id : 내부 식별자(PK, AUTOINCREMENT)
// - nickname : 사용자 닉네임
// - email : 로그인 ID 역할 (UNIQUE)
// - password_hash : 비밀번호 해시값(PBKDF2 결과)
// - created_at : 회원 가입 시각(epoch millis)
// 설계:
// - 비밀번호는 절대 평문 저장하지 않고 hash만 저장한다.
// - email을 기준으로 로그인/중복 체크를 수행한다.
class UserDao(private val db: SQLiteDatabase) {

    // 신규 회원을 users 테이블에 삽입한다.
    // 매개변수:
    // - nickname : 사용자 닉네임
    // - email : 로그인 이메일 (UNIQUE)
    // - passwordHash : PBKDF2로 해시된 비밀번호
    // 반환값:
    // - insert 성공 시 생성된 user_id
    // - 실패 시 -1 (SQLite insert 규약)
    fun insert(nickname: String, email: String, passwordHash: String): Long {
        val cv = ContentValues().apply {
            put("nickname", nickname)
            put("email", email)
            put("password_hash", passwordHash)

            // created_at은 회원 가입 시점을 기록한다.
            put("created_at", System.currentTimeMillis())
        }
        return db.insert(AppDb.T.USERS, null, cv)
    }

    // 이메일이 이미 존재하는지 여부를 확인한다.
    // - 회원가입 시 중복 이메일 체크 용도
    // 반환값:
    // - true : 해당 이메일을 가진 사용자가 이미 존재
    // - false : 사용 가능
    fun existsByEmail(email: String): Boolean {
        return db.rawQuery(
            "SELECT 1 FROM ${AppDb.T.USERS} WHERE email=? LIMIT 1", arrayOf(email)
        ).use { c ->
            // 예외처리) 결과가 1행이라도 있으면 true
            c.moveToFirst()
        }
    }

    // 이메일로 사용자 정보를 조회한다. (로그인 시 사용)
    // 반환:
    // - User 객체 + password_hash 쌍
    // - 해당 이메일이 없으면 null
    // - 비밀번호 검증은 AppService에서 수행한다.
    fun findByEmail(email: String): Pair<User, String /* password_hash */>? {
        db.rawQuery(
            """
            SELECT user_id, nickname, email, created_at, password_hash
            FROM ${AppDb.T.USERS}
            WHERE email=?
            LIMIT 1
            """.trimIndent(), arrayOf(email)
        ).use { c ->

            // 예외처리) 해당 이메일 사용자가 없으면 null 반환
            if (!c.moveToFirst()) return null

            // users 테이블 row -> User 모델 매핑
            val user = User(
                userId = c.getLong(0),
                nickname = c.getString(1),
                email = c.getString(2),
                createdAt = c.getLong(3)
            )

            val hash = c.getString(4)
            return user to hash
        }
    }

    // 닉네임 수정
    // - 마이페이지에서 닉네임 변경 시 사용
    // 반환값:
    // - 업데이트된 row 수 (1이면 성공, 0이면 해당 user_id가 없음)
    fun updateNickname(userId: Long, nickname: String): Int {
        val cv = ContentValues().apply {
            put("nickname", nickname)
        }
        return db.update(
            AppDb.T.USERS, cv, "user_id=?", arrayOf(userId.toString())
        )
    }

    // 회원 탈퇴 시 사용자 정보를 삭제한다.
    // - users 테이블에서만 삭제
    // - 실제 서비스에서는 FK/트랜잭션으로 연관 데이터도 함께 정리(AppService에서 처리)
    // 반환값:
    // - 삭제된 row 수
    fun deleteUserById(userId: Long): Int {
        return db.delete(
            AppDb.T.USERS, "user_id=?", arrayOf(userId.toString())
        )
    }

    // user_id 기준으로 사용자 정보를 조회한다.
    // 반환:
    // - User 객체
    // - 존재하지 않으면 null
    fun getById(userId: Long): User? {
        db.rawQuery(
            """
            SELECT user_id, nickname, email, created_at
            FROM ${AppDb.T.USERS}
            WHERE user_id=?
            LIMIT 1
            """.trimIndent(), arrayOf(userId.toString())
        ).use { c ->

            // 예외처리) 해당 user_id가 없으면 null 반환
            if (!c.moveToFirst()) return null

            return User(
                userId = c.getLong(0),
                nickname = c.getString(1),
                email = c.getString(2),
                createdAt = c.getLong(3)
            )
        }
    }
}
