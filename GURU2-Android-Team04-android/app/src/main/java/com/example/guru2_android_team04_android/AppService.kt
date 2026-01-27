package com.example.guru2_android_team04_android

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.guru2_android_team04_android.auth.SessionManager
import com.example.guru2_android_team04_android.core.AppError
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.db.*
import com.example.guru2_android_team04_android.data.model.*
import com.example.guru2_android_team04_android.domain.BadgeEngine
import com.example.guru2_android_team04_android.export.CardExporter
import com.example.guru2_android_team04_android.llm.GeminiClient
import com.example.guru2_android_team04_android.security.Pbkdf2
import com.example.guru2_android_team04_android.util.DateUtil
import com.example.guru2_android_team04_android.util.EmailPolicy
import com.example.guru2_android_team04_android.util.MindCardTextUtil
import com.example.guru2_android_team04_android.util.NetworkUtil
import com.example.guru2_android_team04_android.util.PasswordPolicy

// AppService : 앱의 서비스 계층
// 용도:
// - UI(Activity/Fragment/ViewModel)에서 직접 DB/네트워크에 접근하지 않도록, 기능 단위로 로직을 묶어 제공한다.
// - 로그인/회원가입/탈퇴, 일기 저장/조회, AI 분석, 즐겨찾기(보관함), 배지, 내보내기 같은 여러 기능을 한 흐름으로 연결해 앱 동작을 완성하는 역할
// 설계 포인트:
// - SessionManager로 현재 사용자(ownerId)를 관리한다.
// - AppDbHelper로 SQLite 접근을 통일한다.
// - 외부 I/O(네트워크/DB)는 실패 가능하므로, 대부분 AppResult로 감싸 UI에서 처리 가능하게 한다.
class AppService(
    context: Context, private val geminiClient: GeminiClient
) {
    // applicationContext를 보관하여 Activity 생명주기와 무관하게 안전하게 사용한다.
    private val appContext = context.applicationContext

    // 현재 사용자(회원/비회원) 식별자(ownerId) 및 비회원 세션 생성 시각 등을 관리한다.
    private val session = SessionManager(appContext)

    // SQLite DB Helper(테이블 생성/버전업/연결 관리)
    private val helper = AppDbHelper(appContext)

    // Session / Auth
    // 비회원 세션을 시작한다.
    // 동작:
    // - ANON_xxx 형태의 ownerId를 생성/가져온 뒤 세션에 저장한다.
    // - 비회원 최초 생성 시각도 저장해 "서비스 이용일" 같은 통계에 활용한다.
    fun startAnonymousSession(): String {
        val anon = session.getOrCreateAnonOwnerId()
        session.getOrCreateAnonCreatedAt()
        session.setOwnerId(anon)
        return anon
    }

    // 현재 세션의 ownerId를 반환한다. (없으면 null)
    fun currentOwnerIdOrNull(): String? = session.currentOwnerId()

    // 이메일 사용 가능 여부를 검사한다.
    // - 형식 검증 후, DB(users)에서 중복 여부를 확인한다.
    fun isEmailAvailable(email: String): AppResult<Boolean> {
        val normalized = EmailPolicy.normalize(email)

        // 예외처리) 빈 이메일/잘못된 이메일 형식은 즉시 실패로 반환(네트워크/DB 접근 안 함)
        if (normalized.isEmpty()) return AppResult.Failure(AppError.EmptyEmail)
        if (!EmailPolicy.isValid(normalized)) return AppResult.Failure(AppError.InvalidEmail)

        return try {
            val db = helper.readableDatabase
            val exists = UserDao(db).existsByEmail(normalized)
            AppResult.Success(!exists)
        } catch (e: Exception) {
            // 예외처리) DB 접근 실패 등 예기치 못한 오류는 Unknown으로 래핑
            AppResult.Failure(AppError.Unknown(e.message ?: "unknown"))
        }
    }

    // 회원가입
    // 흐름:
    // 1) 입력값 검증
    // 2) 중복 이메일 확인
    // 3) 트랜잭션 시작 → 사용자 저장 → 비회원 데이터가 있으면 회원 데이터로 마이그레이션
    // 4) 기본 프로필 이미지 값(settings) 저장
    // 5) 성공 시 세션 ownerId를 USER_xxx로 전환
    fun signUp(
        email: String, password: CharArray, passwordConfirm: CharArray, nickname: String
    ): AppResult<Long> {
        val normalizedEmail = EmailPolicy.normalize(email)
        val nick = nickname.trim()

        // 예외처리) 입력 검증 실패 시 즉시 실패 반환
        if (normalizedEmail.isEmpty()) return AppResult.Failure(AppError.EmptyEmail)
        if (!EmailPolicy.isValid(normalizedEmail)) return AppResult.Failure(AppError.InvalidEmail)
        if (nick.isEmpty()) return AppResult.Failure(AppError.EmptyNickname)
        if (password.isEmpty()) return AppResult.Failure(AppError.EmptyPassword)
        if (passwordConfirm.isEmpty()) return AppResult.Failure(AppError.EmptyPasswordConfirm)
        if (!password.contentEquals(passwordConfirm)) return AppResult.Failure(AppError.PasswordMismatch)
        if (!PasswordPolicy.isStrong(password)) return AppResult.Failure(AppError.WeakPassword)

        val beforeOwner = session.currentOwnerId()
        val db = helper.writableDatabase
        val userDao = UserDao(db)

        return try {
            // 예외처리) 중복 이메일이면 트랜잭션 전에 즉시 실패
            if (userDao.existsByEmail(normalizedEmail)) return AppResult.Failure(AppError.EmailAlreadyUsed)

            db.beginTransaction()
            try {
                // 비밀번호는 해시(PBKDF2)로 저장(평문 저장 금지)
                val hash = Pbkdf2.hash(password)

                val userId = userDao.insert(nick, normalizedEmail, hash)

                // 예외처리) insert 실패(rowId <= 0)면 강제로 예외를 던져 트랜잭션 롤백 유도
                if (userId <= 0L) throw RuntimeException("user insert failed")

                val newOwner = ownerIdForUser(userId)

                // 비회원 데이터가 존재하면 회원 계정으로 ownerId를 통째로 교체(마이그레이션)
                if (beforeOwner != null && beforeOwner.startsWith("ANON_")) {
                    migrateOwnerId(db, fromOwner = beforeOwner, toOwner = newOwner)

                    // 비회원 임시 저장(is_temporary=1) 데이터를 회원으로 전환 시 정식 저장으로 변경
                    db.execSQL(
                        "UPDATE ${AppDb.T.ENTRIES} SET is_temporary=0 WHERE owner_id=?",
                        arrayOf(newOwner)
                    )
                }

                // 기본 프로필 이미지 설정이 없으면 채움
                val settingsDao = SettingsDao(db)
                if (settingsDao.get(newOwner, SettingsDao.KEY_PROFILE_IMAGE_URI) == null) {
                    settingsDao.upsert(newOwner, SettingsDao.KEY_PROFILE_IMAGE_URI, "default.png")
                }

                db.setTransactionSuccessful()
                db.endTransaction()

                // 회원가입 완료 후 세션 ownerId를 USER_xxx로 전환
                session.setOwnerId(newOwner)
                AppResult.Success(userId)
            } catch (t: Throwable) {
                // 예외처리) 트랜잭션이 열린 상태에서 예외가 나면 종료를 보장
                try {
                    db.endTransaction()
                } catch (_: Exception) {
                }
                throw t
            }
        } catch (e: SQLiteConstraintException) {
            // 예외처리) UNIQUE(email) 충돌 등 DB 제약 조건 위반을 명확한 에러로 매핑
            AppResult.Failure(AppError.EmailAlreadyUsed)
        } catch (e: Exception) {
            // 예외처리) 그 외 오류는 Unknown으로 래핑
            AppResult.Failure(AppError.Unknown(e.message ?: "unknown"))
        }
    }

    // 로그인
    // 흐름:
    // 1) 이메일/비밀번호 형식 검증
    // 2) users 테이블에서 email로 사용자 조회
    // 3) PBKDF2 해시 검증
    // 4) 비회원 데이터가 있으면 회원으로 마이그레이션
    // 5) 세션 ownerId를 USER_xxx로 설정
    fun login(email: String, password: CharArray): AppResult<Boolean> {
        val normalizedEmail = EmailPolicy.normalize(email)

        // 예외처리) 입력 검증 실패 시 즉시 실패 반환
        if (normalizedEmail.isEmpty()) return AppResult.Failure(AppError.EmptyEmail)
        if (!EmailPolicy.isValid(normalizedEmail)) return AppResult.Failure(AppError.InvalidEmail)
        if (password.isEmpty()) return AppResult.Failure(AppError.EmptyPassword)

        val beforeOwner = session.currentOwnerId()
        val db = helper.writableDatabase
        val userDao = UserDao(db)

        return try {
            val found = userDao.findByEmail(normalizedEmail)
                ?: return AppResult.Failure(AppError.AuthFailed)

            val user = found.first
            val hash = found.second

            val ok = Pbkdf2.verify(password, hash)
            if (!ok) return AppResult.Failure(AppError.AuthFailed)

            val newOwner = ownerIdForUser(user.userId)

            // 로그인 시점에도 비회원 데이터가 있으면 회원 계정으로 통합
            if (beforeOwner != null && beforeOwner.startsWith("ANON_")) {
                db.beginTransaction()
                try {
                    migrateOwnerId(db, fromOwner = beforeOwner, toOwner = newOwner)
                    db.execSQL(
                        "UPDATE ${AppDb.T.ENTRIES} SET is_temporary=0 WHERE owner_id=?",
                        arrayOf(newOwner)
                    )
                    db.setTransactionSuccessful()
                } finally {
                    // 예외처리) 트랜잭션 종료는 반드시 수행
                    db.endTransaction()
                }
            }

            // 기본 프로필 이미지 값이 없으면 저장
            val settingsDao = SettingsDao(db)
            if (settingsDao.get(newOwner, SettingsDao.KEY_PROFILE_IMAGE_URI) == null) {
                settingsDao.upsert(newOwner, SettingsDao.KEY_PROFILE_IMAGE_URI, "default.png")
            }

            session.setOwnerId(newOwner)
            AppResult.Success(true)
        } catch (e: Exception) {
            // 예외처리) 예기치 못한 오류는 Unknown으로 래핑
            AppResult.Failure(AppError.Unknown(e.message ?: "unknown"))
        }
    }

    // 로그아웃: 세션(현재 ownerId 등) 초기화
    fun logout() = session.clear()

    // 회원 탈퇴(현재 세션이 회원(USER_)일 때만 가능)
    // 동작:
    // - 사용자의 일기/분석/배지/설정/월간요약을 모두 삭제한 후 users에서도 삭제한다.
    // - 여러 테이블을 함께 지우므로 트랜잭션으로 묶어 "부분 삭제"를 방지한다.
    fun withdrawCurrentUser(): Boolean {
        val ownerId = session.currentOwnerId() ?: return false

        // 예외처리) 비회원은 탈퇴 대상이 아니므로 false
        if (!ownerId.startsWith("USER_")) return false

        val userId = ownerId.removePrefix("USER_").toLongOrNull() ?: return false
        val db = helper.writableDatabase

        return try {
            db.beginTransaction()
            try {
                // 분석 테이블은 entry_id 기반이므로 ownerId의 entry 목록을 서브쿼리로 삭제
                db.delete(
                    AppDb.T.ANALYSIS,
                    "entry_id IN (SELECT entry_id FROM ${AppDb.T.ENTRIES} WHERE owner_id=?)",
                    arrayOf(ownerId)
                )
                db.delete(AppDb.T.ENTRIES, "owner_id=?", arrayOf(ownerId))
                db.delete(AppDb.T.USER_BADGES, "owner_id=?", arrayOf(ownerId))
                db.delete(AppDb.T.SETTINGS, "owner_id=?", arrayOf(ownerId))
                db.delete(AppDb.T.MONTHLY, "owner_id=?", arrayOf(ownerId))
                UserDao(db).deleteUserById(userId)

                db.setTransactionSuccessful()
            } finally {
                // 예외처리) 트랜잭션 종료는 반드시 수행
                db.endTransaction()
            }

            session.clear()
            true
        } catch (_: Exception) {
            // 예외처리) 탈퇴 과정에서 오류가 나면 false 반환(호출부에서 안내)
            false
        }
    }

    // users.user_id를 앱 내부 ownerId 규칙(USER_xxx)로 변환
    private fun ownerIdForUser(userId: Long) = "USER_$userId"

    // 비회원 ownerId(ANON_xxx)로 저장된 데이터를 회원 ownerId(USER_xxx)로 일괄 변경한다.
    // - 여러 테이블에서 owner_id 컬럼을 동일하게 사용하므로, 한번에 "사용자 소유권"을 이전할 수 있다.
    private fun migrateOwnerId(db: SQLiteDatabase, fromOwner: String, toOwner: String) {
        db.execSQL(
            "UPDATE ${AppDb.T.ENTRIES} SET owner_id=? WHERE owner_id=?", arrayOf(toOwner, fromOwner)
        )
        db.execSQL(
            "UPDATE ${AppDb.T.MONTHLY} SET owner_id=? WHERE owner_id=?", arrayOf(toOwner, fromOwner)
        )
        db.execSQL(
            "UPDATE ${AppDb.T.USER_BADGES} SET owner_id=? WHERE owner_id=?",
            arrayOf(toOwner, fromOwner)
        )
        db.execSQL(
            "UPDATE ${AppDb.T.SETTINGS} SET owner_id=? WHERE owner_id=?",
            arrayOf(toOwner, fromOwner)
        )
    }

    // Diary CRUD

    // 일기 저장(upsert) + 저장 후 배지 조건 확인
    // - 일기 저장이 성공하면 BadgeEngine이 누적 작성/연속 작성/감정 다양성 기준을 체크한다.
    fun upsertEntry(entry: DiaryEntry): Long {
        val db = helper.writableDatabase
        val id = DiaryDao(db).upsert(entry)
        BadgeEngine(db).checkAndGrant(entry.ownerId)
        return id
    }

    // entry_id 기준으로 일기 삭제
    fun deleteEntry(entryId: Long): Int {
        val db = helper.writableDatabase
        return DiaryDao(db).delete(entryId)
    }

    // 월 단위 일기 목록 조회
    fun getEntriesByMonth(ownerId: String, yearMonth: String): List<DiaryEntry> {
        val db = helper.readableDatabase
        return DiaryDao(db).getByMonth(ownerId, yearMonth)
    }

    // 주 단위(기간) 일기 목록 조회
    fun getEntriesByWeek(
        ownerId: String, weekStartYmd: String, weekEndYmd: String
    ): List<DiaryEntry> {
        val db = helper.readableDatabase
        return DiaryDao(db).getByRange(ownerId, weekStartYmd, weekEndYmd)
    }

    // 캘린더 화면용: 날짜별 감정 맵 조회
    fun getCalendarMoodMap(ownerId: String, yearMonth: String): Map<String, Mood> {
        val db = helper.readableDatabase
        return DiaryDao(db).getMoodMapByMonth(ownerId, yearMonth)
    }

    // 캘린더/통계 화면용: 태그 빈도 맵 조회
    fun getCalendarTagCounts(ownerId: String, yearMonth: String): Map<String, Int> {
        val db = helper.readableDatabase
        return DiaryDao(db).getTagCountsByMonth(ownerId, yearMonth)
    }

    // 저장 완료 -> 홈에서 보여줄 "마음 카드(Preview)" 생성
// 특징:
// - 저장은 반드시 수행한다.
// - AI 분석은 실패할 수 있으므로, 실패하더라도 Preview 카드 생성은 가능하게 만든다.
// - 추가: 분석 실패 시 에러(AppError)를 함께 반환하여 UI에서 토스트로 안내할 수 있게 한다.
    fun saveEntryAndPrepareMindCardSafe(entry: DiaryEntry): AppResult<MindCardPreviewResult> {
        val db = helper.writableDatabase

        val ymd = DateUtil.todayYmd()
        val isAnon = entry.ownerId.startsWith("ANON_")

        // 예외처리) 제목/내용이 비어 있으면 저장 자체를 막는다.
        val title = entry.title.trim()
        val content = entry.content.trim()
        if (title.isEmpty() || content.isEmpty()) {
            return AppResult.Failure(AppError.Unknown("empty title/content"))
        }

        val toSave = entry.copy(
            dateYmd = ymd, title = title, content = content, isTemporary = isAnon
        )

        val entryId = try {
            DiaryDao(db).upsert(toSave)
        } catch (e: Exception) {
            // 예외처리) DB 저장 실패 시 실패 결과 반환
            return AppResult.Failure(AppError.Unknown(e.message ?: "db error"))
        }

        // 저장 성공 후 배지 엔진 실행
        BadgeEngine(db).checkAndGrant(entry.ownerId)

        // 분석은 실패해도 저장된 일기는 유지되어야 하므로 try-catch 결과를 분리한다.
        val analysisR = runAnalysisSafe(entryId)
        val analysis = (analysisR as? AppResult.Success)?.data
        val err = (analysisR as? AppResult.Failure)?.error

        val short = makeShortComfortMessage(analysis)
        val mission1 = analysis?.actions?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "천천히 숨 고르기"

        val preview = MindCardPreview(
            entryId = entryId,
            dateYmd = ymd,
            title = toSave.title,
            mood = toSave.mood,
            tags = toSave.tags,
            comfortPreview = short,
            mission = mission1
        )

        return AppResult.Success(MindCardPreviewResult(preview, err))
    }

    // 분석(fullText)이 있으면 앞쪽 일부를 프리뷰로 만들고, 분석이 없으면 기본 위로 문구를 반환한다.
    private fun makeShortComfortMessage(analysis: AiAnalysis?): String {
        if (analysis == null) return "아직 일기를 작성하지 않았어요. 이야기를 작성하고 마음 답장을 확인해요."
        val preview =
            MindCardTextUtil.makePreview(analysis.fullText, maxSentences = 2, maxChars = 90)
        return if (preview.isBlank()) analysis.summary else preview
    }

    // 내부 전용: entry_id로 DiaryEntry를 직접 로드한다.
    // - DAO에 "id로 조회" 함수가 없을 때 서비스 내부에서 필요한 최소 조회만 수행한다.
    private fun loadEntryByIdOrNull(entryId: Long): DiaryEntry? {
        val db = helper.readableDatabase
        return db.rawQuery(
            """
            SELECT entry_id, owner_id, date_ymd, title, content, mood, tags_json, is_favorite, is_temporary, created_at, updated_at
            FROM ${AppDb.T.ENTRIES}
            WHERE entry_id=?
            LIMIT 1
            """.trimIndent(), arrayOf(entryId.toString())
        ).use { c ->
            // 예외처리) 해당 entry_id가 없으면 null
            if (!c.moveToFirst()) return null
            DiaryEntry(
                entryId = c.getLong(0),
                ownerId = c.getString(1),
                dateYmd = c.getString(2),
                title = c.getString(3),
                content = c.getString(4),
                mood = Mood.fromDb(c.getInt(5)),
                tags = com.example.guru2_android_team04_android.util.JsonMini.jsonToList(
                    c.getString(
                        6
                    )
                ),
                isFavorite = c.getInt(7) == 1,
                isTemporary = c.getInt(8) == 1,
                createdAt = c.getLong(9),
                updatedAt = c.getLong(10)
            )
        }
    }

    // 특정 entry_id로 마음 카드 Preview를 생성한다(안전 버전)
    // - 분석 실패 시에도 Preview 생성은 가능하도록 기본 문구/미션을 사용한다.
    fun getMindCardPreviewByEntryIdSafe(entryId: Long): AppResult<MindCardPreview> {
        val entry = loadEntryByIdOrNull(entryId) ?: return AppResult.Failure(AppError.NotFound)

        val (analysis, _) = when (val r = runAnalysisSafe(entryId)) {
            is AppResult.Success -> r.data to null
            is AppResult.Failure -> null to r.error
        }

        val short = makeShortComfortMessage(analysis)
        val mission1 = analysis?.actions?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "천천히 숨 고르기"

        return AppResult.Success(
            MindCardPreview(
                entryId = entry.entryId,
                dateYmd = entry.dateYmd,
                title = entry.title,
                mood = entry.mood,
                tags = entry.tags,
                comfortPreview = short,
                mission = mission1
            )
        )
    }

    // 마음 카드 상세 분석 조회(안전 버전)
    // - 분석을 반드시 얻어야 상세 화면을 구성할 수 있으므로, runAnalysisSafe 결과를 그대로 반환한다(성공이면 Detail로 변환).
    fun getMindCardDetailByEntryIdSafe(entryId: Long): AppResult<MindCardDetail> {
        return when (val r = runAnalysisSafe(entryId)) {
            is AppResult.Success -> {
                val a = r.data

                // 상세 UI는 3개의 미션 슬롯이 고정이므로 부족하면 기본값으로 채운다.
                val missions = buildList {
                    addAll(a.actions.take(3))
                    while (size < 3) add("천천히 숨 고르기")
                }

                AppResult.Success(
                    MindCardDetail(
                        entryId = entryId,
                        summary = a.summary,
                        triggerPattern = a.triggerPattern,
                        hashtags = a.hashtags,
                        missions = missions,
                        missionSummary = a.missionSummary,
                        fullText = a.fullText
                    )
                )
            }

            is AppResult.Failure -> r
        }
    }

    // AI Analysis
    // DB에 저장된 분석 결과를 조회한다(없으면 null)
    fun getAnalysis(entryId: Long): AiAnalysis? {
        val db = helper.readableDatabase
        return AnalysisDao(db).getByEntryId(entryId)
    }

    // 분석 실행(unsafe 버전)
    // - 네트워크 실패/파싱 실패/entry 미존재 등 예외를 던질 수 있다.
    // - 내부적으로 이미 DB에 분석이 있으면 재요청하지 않고 캐시를 반환한다.
    fun runAnalysis(entryId: Long): AiAnalysis {
        val db = helper.writableDatabase
        val analysisDao = AnalysisDao(db)

        // 이미 분석이 있으면 캐시 반환
        analysisDao.getByEntryId(entryId)?.let { return it }

        // 분석 대상 일기 로드 (없으면 예외)
        val entry = db.rawQuery(
            """
            SELECT owner_id, date_ymd, title, content, mood, tags_json, is_favorite, is_temporary, created_at, updated_at
            FROM ${AppDb.T.ENTRIES}
            WHERE entry_id=?
            LIMIT 1
            """.trimIndent(), arrayOf(entryId.toString())
        ).use { c ->
            // 예외처리) entry가 없으면 분석 불가
            if (!c.moveToFirst()) throw IllegalArgumentException("Entry not found: $entryId")

            DiaryEntry(
                entryId = entryId,
                ownerId = c.getString(0),
                dateYmd = c.getString(1),
                title = c.getString(2),
                content = c.getString(3),
                mood = Mood.fromDb(c.getInt(4)),
                tags = com.example.guru2_android_team04_android.util.JsonMini.jsonToList(
                    c.getString(
                        5
                    )
                ),
                isFavorite = c.getInt(6) == 1,
                isTemporary = c.getInt(7) == 1,
                createdAt = c.getLong(8),
                updatedAt = c.getLong(9)
            )
        }

        // 외부 API 호출(Gemini)
        val result = geminiClient.analyzeDiary(
            moodLabel = entry.mood.name, tags = entry.tags, diaryText = entry.content
        )

        val analysis = AiAnalysis(
            entryId = entryId,
            summary = result.summary,
            triggerPattern = result.triggerPattern,
            actions = result.actions.take(3),
            hashtags = result.hashtags,
            missionSummary = result.missionSummary,
            fullText = result.fullText
        )

        // 분석 결과를 로컬 DB에 저장(캐시)
        analysisDao.upsert(analysis)
        return analysis
    }

    // 분석 실행
    // - 네트워크/파싱/entry 미존재 등을 AppError로 매핑해 UI가 안정적으로 처리할 수 있게 한다.
    fun runAnalysisSafe(entryId: Long): AppResult<AiAnalysis> {
        //예외처리)
        // 캐시 먼저 반환
        getAnalysis(entryId)?.let { return AppResult.Success(it) }
        //캐시가 없을 때만 네트워크 체크
        if (!NetworkUtil.isNetworkAvailable(appContext)) {
            return AppResult.Failure(AppError.NetworkUnavailable)
        }

        return try {
            AppResult.Success(runAnalysis(entryId))
        } catch (e: RuntimeException) {
            val msg = e.message.orEmpty()
            when {
                msg.contains("LLM API failed") -> {
                    val code =
                        Regex("LLM API failed: (\\d+)").find(msg)?.groupValues?.get(1)?.toInt()
                            ?: -1
                    AppResult.Failure(AppError.ApiError(code))
                }

                msg.contains("JSON", ignoreCase = true) -> AppResult.Failure(AppError.ParseError)

                msg.contains("Entry not found") -> AppResult.Failure(AppError.NotFound)

                else -> AppResult.Failure(AppError.Unknown(msg))
            }
        } catch (e: Exception) {
            // 예외처리) 그 외 알 수 없는 오류
            AppResult.Failure(AppError.Unknown(e.message ?: "unknown"))
        }
    }

    // 마음 카드 보관함 = 하트
    // 즐겨찾기(하트) 상태 변경
    // - FavoriteDao는 "ownerId 소유 검증"을 포함하므로, 다른 사용자 데이터 변경을 방지한다.
    fun setEntryFavorite(ownerId: String, entryId: Long, favorite: Boolean): Boolean {
        val db = helper.writableDatabase
        return FavoriteDao(db).set(ownerId, entryId, favorite)
    }

    // 즐겨찾기된 카드 목록(보관함) 조회
    // - MindCardPreview 형태로 반환하여 홈 카드 UI와 모델을 공유한다.
    fun getMindCardArchive(ownerId: String): List<MindCardPreview> {
        val db = helper.readableDatabase
        return FavoriteDao(db).getFavoriteMindCards(ownerId)
    }

    // Export to Gallery

    // 마음 카드를 이미지(Bitmap)로 렌더링한 뒤 갤러리에 저장한다.
    // - 저장 실패 시 예외를 던질 수 있다(unsafe).
    fun exportMindCardToGallery(context: Context, entryId: Long): Uri {
        val entry =
            loadEntryByIdOrNull(entryId) ?: throw IllegalArgumentException("Entry not found")
        val db = helper.readableDatabase
        val analysis = AnalysisDao(db).getByEntryId(entryId)

        // 3장 저장하고, 첫 번째 Uri를 대표로 반환(기존 시그니처 유지)
        val uris = CardExporter.save3ScreensToGallery(
            context = context,
            entry = entry,
            analysis = analysis,
            baseName = "mindcard_${entry.dateYmd}"
        )
        return uris.first()
    }

    // Export 안전 버전
    // - 권한 문제/저장 실패 등을 AppError로 변환해 UI에서 안내 가능하게 한다.
    fun exportMindCardToGallerySafe(context: Context, entryId: Long): AppResult<Uri> {
        return try {
            AppResult.Success(exportMindCardToGallery(context, entryId))
        } catch (_: SecurityException) {
            AppResult.Failure(AppError.PermissionDenied)
        } catch (_: Exception) {
            AppResult.Failure(AppError.StorageError)
        }
    }

    // ✅ NEW: 분석 화면(일기 보기/위로/실천안) 3장을 "XML 디자인 그대로" 캡처해서 갤러리에 저장한다.
    // - render는 테마/리소스 영향을 받으므로 Activity context를 그대로 전달한다.
    // - 저장은 applicationContext로 진행한다.
    fun exportAnalysisScreensToGallery(context: Context, entryId: Long): List<Uri> {
        val entry =
            loadEntryByIdOrNull(entryId) ?: throw IllegalArgumentException("Entry not found")
        val db = helper.readableDatabase
        val analysis = AnalysisDao(db).getByEntryId(entryId)

        val bmp1 = CardExporter.renderAnalysisDiaryScreen(context, entry)
        val bmp2 = CardExporter.renderAnalysisComfortScreen(context, entry, analysis)
        val bmp3 = CardExporter.renderAnalysisActionsScreen(context, entry, analysis)

        val appCtx = context.applicationContext
        return listOf(
            CardExporter.saveToGallery(appCtx, bmp1, "analysis_diary_${entry.dateYmd}"),
            CardExporter.saveToGallery(appCtx, bmp2, "analysis_comfort_${entry.dateYmd}"),
            CardExporter.saveToGallery(appCtx, bmp3, "analysis_actions_${entry.dateYmd}")
        )
    }

    // ✅ NEW: 3장 저장 안전 버전
    // - 권한 문제/저장 실패 등을 AppError로 변환해 UI에서 안내 가능하게 한다.
    fun exportAnalysisScreensToGallerySafe(context: Context, entryId: Long): AppResult<List<Uri>> {
        return try {
            AppResult.Success(exportAnalysisScreensToGallery(context, entryId))
        } catch (_: SecurityException) {
            AppResult.Failure(AppError.PermissionDenied)
        } catch (_: Exception) {
            AppResult.Failure(AppError.StorageError)
        }
    }

    // Badges / Profile
    // 배지 조건 검사 및 지급
    fun checkAndGrantBadges(ownerId: String) {
        val db = helper.writableDatabase
        BadgeEngine(db).checkAndGrant(ownerId)
    }

    // 사용자가 선택한 배지를 1개로 유지하도록 설정
    fun selectBadge(ownerId: String, badgeId: Int) {
        val db = helper.writableDatabase
        BadgeEngine(db).selectBadge(ownerId, badgeId)
    }

    // 배지 상태 목록 조회(획득 여부/선택 여부 포함)
    fun getBadgeStatuses(ownerId: String): List<BadgeStatus> {
        val db = helper.readableDatabase
        return BadgeDao(db).getAllBadgeStatuses(ownerId)
    }

    // 서비스 이용 일수 계산
    // - 회원: users.created_at 기준
    // - 비회원: 세션에 저장된 anonCreatedAt 기준
    fun getServiceDays(ownerId: String): Long {
        return if (ownerId.startsWith("USER_")) {
            val uid = ownerId.removePrefix("USER_").toLongOrNull() ?: return 0L
            val db = helper.readableDatabase
            val user = UserDao(db).getById(uid) ?: return 0L
            DateUtil.daysSince(user.createdAt)
        } else {
            val anonStart = session.getOrCreateAnonCreatedAt()
            DateUtil.daysSince(anonStart)
        }
    }

    // 프로필 정보 조회
    // - 선택 배지 + 설정(프로필 이미지 URI) + 닉네임/이메일(회원일 때) 조합
    fun getUserProfile(): UserProfile {
        val ownerId = session.currentOwnerId() ?: session.getOrCreateAnonOwnerId()
        val db = helper.readableDatabase

        val selectedBadge = BadgeDao(db).getSelectedBadge(ownerId)
        val profileUri = SettingsDao(db).get(ownerId, SettingsDao.KEY_PROFILE_IMAGE_URI)

        return if (ownerId.startsWith("USER_")) {
            val uid = ownerId.removePrefix("USER_").toLongOrNull() ?: 0L
            val user = UserDao(db).getById(uid)

            UserProfile(
                ownerId = ownerId,
                nickname = user?.nickname ?: "사용자",
                emailOrAnon = user?.email ?: "unknown",
                serviceDays = getServiceDays(ownerId),
                selectedBadge = selectedBadge,
                profileImageUri = profileUri
            )
        } else {
            UserProfile(
                ownerId = ownerId,
                nickname = "익명",
                emailOrAnon = "ANON",
                serviceDays = getServiceDays(ownerId),
                selectedBadge = selectedBadge,
                profileImageUri = profileUri
            )
        }
    }

    // 닉네임 변경(회원만 가능)
    fun updateNickname(newNickname: String): Boolean {
        val ownerId = session.currentOwnerId() ?: return false

        // 예외처리) 비회원은 닉네임 변경 불가
        if (!ownerId.startsWith("USER_")) return false

        val uid = ownerId.removePrefix("USER_").toLongOrNull() ?: return false

        val nick = newNickname.trim()
        // 예외처리) 빈 닉네임은 허용하지 않음
        if (nick.isEmpty()) return false

        val db = helper.writableDatabase
        return try {
            UserDao(db).updateNickname(uid, nick) > 0
        } catch (_: Exception) {
            // 예외처리) DB 오류 시 false
            false
        }
    }

    // 프로필 이미지 URI 변경(settings에 저장)
    fun updateProfileImageUri(newUri: String?): Boolean {
        val ownerId = session.currentOwnerId() ?: return false
        val db = helper.writableDatabase
        val dao = SettingsDao(db)

        return try {
            dao.upsert(ownerId, SettingsDao.KEY_PROFILE_IMAGE_URI, newUri?.trim().orEmpty())
            true
        } catch (_: Exception) {
            // 예외처리) DB 오류 시 false
            false
        }
    }

    // 지난달 감정 요약(월간 요약 생성 시 필요한 "최다 태그" 조회)
    fun getLastMonthTopTag(ownerId: String): String {
        val lastYm = DateUtil.previousMonthYm()
        val db = helper.readableDatabase
        val tagCounts = DiaryDao(db).getTagCountsByMonth(ownerId, lastYm)

        // 예외처리) 태그가 없으면 빈 문자열 반환
        if (tagCounts.isEmpty()) return ""

        return tagCounts.entries.maxByOrNull { it.value }?.key.orEmpty()
    }

    // 지난달 월간 요약이 없으면 생성하고, 있으면 기존 값을 반환한다.
    // - 월간요약은 "캐시" 성격이므로 중복 계산을 피한다.
    fun ensureLastMonthMonthlySummary(ownerId: String): MonthlySummary? {
        val lastYm = DateUtil.previousMonthYm()
        val db = helper.writableDatabase
        val monthlyDao = MonthlyDao(db)

        // 이미 있으면 그대로 반환
        monthlyDao.get(ownerId, lastYm)?.let { return it }

        // 통계가 없으면(일기 없음) 생성 불가
        val moodStats = DiaryDao(db).getMoodStatsByMonth(ownerId, lastYm)
        if (moodStats.isEmpty()) return null

        val dominantMood = moodStats.maxByOrNull { it.count }!!.mood

        // 월간 입력 텍스트 구성(너무 길어지지 않게 요약해서 Gemini에 전달)
        val entries = DiaryDao(db).getByMonth(ownerId, lastYm)
        if (entries.isEmpty()) return null

        val brief = buildString {
            // 너무 긴 입력 방지: 일기 25개까지만, 본문은 200자까지만
            for (e in entries.take(25)) {
                val content = e.content.replace("\n", " ").take(200)
                append("- ${e.dateYmd} | mood=${e.mood.name} | tags=${e.tags.joinToString(",")} | $content\n")
            }
        }

        val monthlyR = geminiClient.summarizeMonth(
            yearMonth = lastYm, dominantMoodLabel = dominantMood.name, entriesBrief = brief
        )

        val ms = MonthlySummary(
            ownerId = ownerId,
            yearMonth = lastYm,
            dominantMood = dominantMood,
            oneLineSummary = monthlyR.oneLineSummary.ifBlank { "이번 달을 한 문장으로 정리해볼게요." },
            detailSummary = monthlyR.detailSummary.ifBlank { "이번 달 기록을 바탕으로 한 요약을 준비 중이에요." },
            emotionFlow = monthlyR.emotionFlow.ifBlank { "안정 → 변화 → 회복" },
            keywords = monthlyR.keywords
        )

        monthlyDao.upsert(ms)
        return ms
    }
}
