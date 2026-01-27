package com.example.guru2_android_team04_android.data.db

import android.content.Context
import com.example.guru2_android_team04_android.data.db.AppDb
import com.example.guru2_android_team04_android.data.db.AppDbHelper
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.JsonMini

// DiaryEntryReader : 화면(오늘일기/분석)에서 entryId로 "일기 원문"이 필요할 때 사용하는 읽기 전용 헬퍼
// 용도:
// - ViewModel/UI에서 entryId만 가지고 DB에서 DiaryEntry 1개를 조회해 올 수 있게 한다.
// 설계 이유:
// - AppService 내부에 entryId로 DiaryEntry를 로드하는 함수가 private이라 외부(UI)에서 직접 호출이 불가능하다.
// - Service 구조를 크게 바꾸지 않고 화면 연동(일기 상세/AI 분석 화면)을 빠르게 하기 위해 DB에서 "최소 조회"만 수행한다.
// 특징:
// - 쓰기(INSERT/UPDATE/DELETE)는 하지 않고 조회만 제공한다.
// - SQLite를 직접 쓰지만 AppDb 테이블 상수(AppDb.T.ENTRIES)를 사용해 SQL 문자열 오타를 줄인다.
class DiaryEntryReader(context: Context) {

    // applicationContext 사용:
    // - Activity/Fragment Context를 직접 들고 있으면 화면 생명주기와 엮여 메모리 누수 위험이 생길 수 있다.
    // - DB Helper는 앱 전체에서 재사용해도 되는 객체라 applicationContext를 사용한다.
    private val appContext = context.applicationContext

    // AppDbHelper : SQLite DB 연결/생성/업그레이드를 담당하는 헬퍼
    // - readableDatabase를 통해 읽기 전용(또는 읽기 중심) 접근을 수행한다.
    private val helper = AppDbHelper(appContext)

    // entryId로 일기 1개를 조회하고, 없으면 null을 반환한다.
    // 반환값:
    // - DiaryEntry? : 존재하면 DiaryEntry, 없으면 null
    // 사용 시점:
    // - 일기 상세 화면 진입 시 원문 로딩
    // - "AI 분석 결과 화면"에서 분석과 함께 원문 표시가 필요할 때
    fun getByIdOrNull(entryId: Long): DiaryEntry? {

        // readableDatabase:
        // - 읽기 목적의 DB 연결을 얻는다.
        // 예외처리) DB 연결은 내부적으로 오류가 날 수 있으나(디스크/권한/손상 등) 일반 앱 환경에서는 드물며,
        //           호출부에서는 null 처리 또는 상위 레이어(AppResult 등)로 감싸는 방식으로 대응한다.
        val db = helper.readableDatabase

        // rawQuery:
        // - entry_id=? 형태의 바인딩을 사용해 SQL 인젝션 위험을 줄인다.
        // - LIMIT 1로 "딱 1개"만 가져오도록 해서 불필요한 조회 비용을 줄인다.
        return db.rawQuery(
            """
            SELECT entry_id, owner_id, date_ymd, title, content, mood, tags_json, is_favorite, is_temporary, created_at, updated_at
            FROM ${AppDb.T.ENTRIES}
            WHERE entry_id=?
            LIMIT 1
            """.trimIndent(), arrayOf(entryId.toString())
        ).use { c ->

            // 예외처리) 조회 결과가 없으면 moveToFirst()가 false이므로 null 반환
            // - 존재하지 않는 entryId(삭제된 일기, 잘못된 id 등) 접근을 안전하게 처리한다.
            if (!c.moveToFirst()) return null

            // DB row -> DiaryEntry 모델 매핑
            // - mood는 DB에 Int로 저장되어 있으므로 Mood.fromDb()로 enum/도메인 값으로 변환한다.
            // - tags_json은 JSON 문자열이므로 JsonMini.jsonToList()로 List<String>으로 역직렬화한다.
            // - is_favorite / is_temporary는 SQLite에 boolean 타입이 없어 0/1(Int)로 저장되므로 == 1로 변환한다.
            DiaryEntry(
                entryId = c.getLong(0),
                ownerId = c.getString(1),
                dateYmd = c.getString(2),
                title = c.getString(3),
                content = c.getString(4),
                mood = Mood.fromDb(c.getInt(5)),
                tags = JsonMini.jsonToList(c.getString(6)),
                isFavorite = c.getInt(7) == 1,
                isTemporary = c.getInt(8) == 1,
                createdAt = c.getLong(9),
                updatedAt = c.getLong(10)
            )
        }
    }
}
