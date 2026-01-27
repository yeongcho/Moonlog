package com.example.guru2_android_team04_android.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.data.model.MoodStat
import com.example.guru2_android_team04_android.util.JsonMini
import com.example.guru2_android_team04_android.util.StreakUtil

// DiaryDao : diary_entries 테이블에 대한 CRUD와 통계 기능을 제공하는 DAO (일기 기능의 핵심 데이터 접근 계층)
// 설계 :
// - ownerId로 사용자 구분: "USER_xxx"(회원) 또는 "ANON_xxx"(비회원)
// - date_ymd는 "YYYY-MM-DD" 문자열로 저장하여 BETWEEN 조회(월/주 범위)를 쉽게 한다.
// - tags는 List<String>이므로 tags_json(JSON 문자열)로 저장한다.
// - is_temporary=1(비회원 임시 저장)은 목록/캘린더/통계에서 숨기기 위해 조회 쿼리에서 제외한다.
// 즐겨찾기(하트) 관련 기능은 FavoriteDao.kt에서 소유자 검증까지 포함해 담당
class DiaryDao(private val db: SQLiteDatabase) {

    // 일기 저장(없으면 INSERT, 있으면 UPDATE)
    // - 테이블에 UNIQUE(owner_id, date_ymd)가 있으므로 같은 날짜에 1개만 유지된다.
    // - 즉, 같은 ownerId가 같은 date_ymd로 저장하면 "수정"처럼 동작한다.
    // 반환값:
    // - 저장된 entry_id를 반환한다.
    // - update 성공 시 기존 entry_id를 조회해서 반환한다.
    // - insert 성공 시 insert가 반환한 rowId(entry_id)를 반환한다.
    // - update는 됐는데 id 조회가 실패하면 -1L
    fun upsert(entry: DiaryEntry): Long {

        // ContentValues: 컬럼명 -> 값 형태로 DB에 저장할 데이터 구성
        // tags는 List<String>이라 JSON 문자열로 직렬화해서 저장한다.
        val cv = ContentValues().apply {
            put("owner_id", entry.ownerId)
            put("date_ymd", entry.dateYmd)
            put("title", entry.title)
            put("content", entry.content)
            put("mood", entry.mood.dbValue)
            put("tags_json", JsonMini.listToJson(entry.tags))
            put("is_favorite", if (entry.isFavorite) 1 else 0)
            put("is_temporary", if (entry.isTemporary) 1 else 0)

            // created_at은 최초 생성 시각으로 쓰되,
            // upsert 구조상 update 때도 그대로 들어올 수 있으므로 생성 시각을 유지하려면 호출부에서 entry.createdAt을 유지해서 넘겨야 한다.
            put("created_at", entry.createdAt)

            // updated_at은 저장 시점마다 갱신한다.
            put("updated_at", System.currentTimeMillis())
        }

        // 1) 먼저 update 시도
        // - 같은 owner/date가 있으면 해당 row가 갱신된다.
        val updated = db.update(
            AppDb.T.ENTRIES, cv, "owner_id=? AND date_ymd=?", arrayOf(entry.ownerId, entry.dateYmd)
        )

        // 2) update가 성공했으면 entry_id를 다시 조회해서 반환
        // 예외처리) update 되었는데도 id 조회가 실패하면 -1L로 방어
        if (updated > 0) return getIdByOwnerAndDate(entry.ownerId, entry.dateYmd) ?: -1L

        // 3) update가 안 됐으면 insert 수행
        // 예외처리) insert 실패 시 -1L을 반환할 수 있다(SQLite insert 규약)
        return db.insert(AppDb.T.ENTRIES, null, cv)
    }

    // 내부 전용: ownerId + date_ymd로 entry_id만 조회한다.
    // upsert에서 update 성공 시 entry_id 반환을 위해 사용한다.
    private fun getIdByOwnerAndDate(ownerId: String, ymd: String): Long? {
        db.rawQuery(
            "SELECT entry_id FROM ${AppDb.T.ENTRIES} WHERE owner_id=? AND date_ymd=? LIMIT 1",
            arrayOf(ownerId, ymd)
        ).use { c ->
            // 예외처리) 결과가 없으면 null 반환
            return if (c.moveToFirst()) c.getLong(0) else null
        }
    }

    // 특정 날짜(ymd)에 해당하는 일기 1개를 조회한다.
    // - 해당 날짜에 일기가 없으면 null 반환
    // - 비회원 임시 저장(is_temporary)은 여기서 필터링하지 않는다. (상세 화면에서 "임시 저장"도 열어야 할 수 있기 때문)
    fun getByDate(ownerId: String, ymd: String): DiaryEntry? {
        db.rawQuery(
            """
            SELECT entry_id, owner_id, date_ymd, title, content, mood, tags_json, is_favorite, is_temporary, created_at, updated_at
            FROM ${AppDb.T.ENTRIES}
            WHERE owner_id=? AND date_ymd=?
            LIMIT 1
            """.trimIndent(), arrayOf(ownerId, ymd)
        ).use { c ->

            // 예외처리) 해당 날짜 데이터가 없으면 null 반환
            if (!c.moveToFirst()) return null

            // DB row -> DiaryEntry 모델 매핑
            // mood는 Int로 저장되어 있어 enum으로 변환한다.
            // tags_json은 JSON 문자열이므로 List<String>으로 역직렬화한다.
            return DiaryEntry(
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

    // entry_id 기준으로 일기 삭제
    // 반환값:
    // - 삭제된 row 개수(0이면 해당 id가 없었다는 뜻)
    // 참고:
    // - ai_analysis 테이블이 entry_id를 FK로 참조하고 ON DELETE CASCADE가 설정되어 있어
    //   일기 삭제 시 분석 결과도 자동 삭제된다(AppDbHelper에서 FK 활성화 필요).
    fun delete(entryId: Long): Int {
        return db.delete(AppDb.T.ENTRIES, "entry_id=?", arrayOf(entryId.toString()))
    }

    // 월 단위 일기 목록 조회
    // - yearMonth: "YYYY-MM"
    // - 내부적으로 01~31 범위로 조회하며, SQLite 문자열 BETWEEN을 이용한다.
    // 예외처리) 2월/30일인 달도 end를 -31로 잡지만,
    // BETWEEN 비교는 문자열 기준이라 존재하지 않는 날짜 row가 추가로 나오지는 않는다.
    fun getByMonth(ownerId: String, yearMonth: String): List<DiaryEntry> {
        val start = "$yearMonth-01"
        val end = "$yearMonth-31"
        return getByRange(ownerId, start, end)
    }

    // 기간 범위 일기 목록 조회
    // - startYmd ~ endYmd (둘 다 "YYYY-MM-DD")
    // - is_temporary=0 조건으로 "비회원 임시 저장"은 목록에서 숨긴다.
    // 반환:
    // - 날짜 오름차순으로 정렬된 일기 리스트
    fun getByRange(ownerId: String, startYmd: String, endYmd: String): List<DiaryEntry> {
        val out = ArrayList<DiaryEntry>()

        db.rawQuery(
            """
            SELECT entry_id, owner_id, date_ymd, title, content, mood, tags_json, is_favorite, is_temporary, created_at, updated_at
            FROM ${AppDb.T.ENTRIES}
            WHERE owner_id=? 
              AND is_temporary=0
              AND date_ymd BETWEEN ? AND ?
            ORDER BY date_ymd ASC
            """.trimIndent(), arrayOf(ownerId, startYmd, endYmd)
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
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
                )
            }
        }
        return out
    }

    // 월간 감정 통계 조회
    // - mood별 COUNT(*)를 집계하여 그래프/요약에 사용한다.
    // - is_temporary=0: 비회원 임시 저장 데이터는 통계에서 제외한다.
    fun getMoodStatsByMonth(ownerId: String, yearMonth: String): List<MoodStat> {
        val start = "$yearMonth-01"
        val end = "$yearMonth-31"
        val out = ArrayList<MoodStat>()

        db.rawQuery(
            """
            SELECT mood, COUNT(*) 
            FROM ${AppDb.T.ENTRIES}
            WHERE owner_id=? AND is_temporary=0 AND date_ymd BETWEEN ? AND ?
            GROUP BY mood
            """.trimIndent(), arrayOf(ownerId, start, end)
        ).use { c ->
            while (c.moveToNext()) {
                out.add(MoodStat(Mood.fromDb(c.getInt(0)), c.getInt(1)))
            }
        }
        return out
    }

    // 전체 일기 개수(임시 저장 제외)
    // - 배지 조건(누적 작성 수)에 사용된다.
    fun countEntries(ownerId: String): Int {
        db.rawQuery(
            "SELECT COUNT(*) FROM ${AppDb.T.ENTRIES} WHERE owner_id=? AND is_temporary=0",
            arrayOf(ownerId)
        ).use { c ->
            // 예외처리) COUNT(*)는 항상 row를 반환하지만, 방어적으로 moveToFirst 체크
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    // 사용자가 사용한 감정 종류 수(임시 저장 제외)
    // - 배지 조건(모든 감정 사용)에 사용된다.
    fun distinctMoodCount(ownerId: String): Int {
        db.rawQuery(
            "SELECT COUNT(DISTINCT mood) FROM ${AppDb.T.ENTRIES} WHERE owner_id=? AND is_temporary=0",
            arrayOf(ownerId)
        ).use { c ->
            // 예외처리) COUNT(DISTINCT ...)도 항상 row를 반환하지만 방어적으로 체크
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    // 최근 작성일 기준으로 연속 작성(streak) 일수를 계산한다.
    // - 최신 날짜부터 과거로 정렬된 date_ymd 목록을 가져온다.
    // - StreakUtil.computeConsecutiveDays()에서 "하루씩 연속인지"를 계산한다.
    fun computeStreakFromLatest(ownerId: String): Int {
        val dates = ArrayList<String>()

        db.rawQuery(
            """
            SELECT date_ymd 
            FROM ${AppDb.T.ENTRIES} 
            WHERE owner_id=? AND is_temporary=0
            ORDER BY date_ymd DESC 
            LIMIT 400
            """.trimIndent(), arrayOf(ownerId)
        ).use { c ->
            while (c.moveToNext()) dates.add(c.getString(0))
        }

        // 예외처리) 일기가 한 번도 없으면 streak는 0
        if (dates.isEmpty()) return 0

        return StreakUtil.computeConsecutiveDays(dates)
    }

    // 캘린더 화면에서 "날짜별 감정"을 표시하기 위한 데이터 조회.
    // 반환 형식:
    // - key: "YYYY-MM-DD"
    // - value: Mood enum
    // LinkedHashMap을 사용한 이유:
    // - 조회된 순서를 유지해서(날짜 오름차순/조회순서) UI 처리에 예측 가능성을 준다.
    fun getMoodMapByMonth(ownerId: String, yearMonth: String): Map<String, Mood> {
        val start = "$yearMonth-01"
        val end = "$yearMonth-31"
        val out = LinkedHashMap<String, Mood>()

        db.rawQuery(
            """
            SELECT date_ymd, mood
            FROM ${AppDb.T.ENTRIES}
            WHERE owner_id=? AND is_temporary=0 AND date_ymd BETWEEN ? AND ?
            """.trimIndent(), arrayOf(ownerId, start, end)
        ).use { c ->
            while (c.moveToNext()) {
                out[c.getString(0)] = Mood.fromDb(c.getInt(1))
            }
        }

        return out
    }

    // 월간 태그 빈도 조회(원그래프 데이터용)
    // - 반환: tag -> count
    // 처리 방식:
    // 1) 해당 월의 모든 tags_json을 가져온다.
    // 2) 각 row의 tags_json을 List<String>으로 변환한다.
    // 3) 태그별로 등장 횟수를 누적한다.
    // 예외처리) tags_json이 비어있거나 [] 인 경우,
    // JsonMini가 빈 리스트로 반환하므로 자연스럽게 누적이 되지 않는다.
    fun getTagCountsByMonth(ownerId: String, yearMonth: String): Map<String, Int> {
        val start = "$yearMonth-01"
        val end = "$yearMonth-31"
        val counts = LinkedHashMap<String, Int>()

        db.rawQuery(
            """
            SELECT tags_json
            FROM ${AppDb.T.ENTRIES}
            WHERE owner_id=? AND is_temporary=0 AND date_ymd BETWEEN ? AND ?
            """.trimIndent(), arrayOf(ownerId, start, end)
        ).use { c ->
            while (c.moveToNext()) {
                val tags = JsonMini.jsonToList(c.getString(0))
                for (t in tags) {
                    counts[t] = (counts[t] ?: 0) + 1
                }
            }
        }
        return counts
    }
}
