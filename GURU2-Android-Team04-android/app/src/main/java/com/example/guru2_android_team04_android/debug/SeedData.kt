package com.example.guru2_android_team04_android.debug

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.guru2_android_team04_android.MyApp
import com.example.guru2_android_team04_android.data.db.AnalysisDao
import com.example.guru2_android_team04_android.data.db.AppDb
import com.example.guru2_android_team04_android.data.db.AppDbHelper
import com.example.guru2_android_team04_android.data.db.DiaryDao
import com.example.guru2_android_team04_android.data.db.UserDao
import com.example.guru2_android_team04_android.data.model.AiAnalysis
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil
import com.example.guru2_android_team04_android.util.EmailPolicy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// SeedData : 디버그/데모(채점) 환경에서 "테스트 계정 + 예시 일기/분석 데이터"를 자동으로 채워주는 유틸 객체
// 용도:
// - 로그인 계정 1개를 미리 만들고, 과거 날짜 일기와 AI 분석 캐시까지 함께 넣어 캘린더/목록/월간요약/마음카드(분석) 기능을 바로 확인할 수 있게 한다.
// 설계:
// - object로 선언해 전역에서 1개 인스턴스만 사용한다.
// - 이미 데이터가 있으면 다시 넣지 않기 정책으로 중복 시드를 방지한다.
object SeedData {

    // 채점용 로그인 계정 정보
    // - 앱을 설치한 누구나 동일한 계정으로 로그인해서 화면을 확인할 수 있다.
    private const val SEED_EMAIL = "test123@example.com"
    private const val SEED_PW = "test123456"
    private const val SEED_NICK = "테스트"

    // 생성할 과거 일기 개수(원하는 만큼 조절 가능)
    // - 아래에서 날짜 리스트를 만들고 take()로 최종 개수를 제한한다.
    private const val SEED_DIARY_COUNT = 12

    // ensureSeedUserAndPastDiaries : 시드 데이터가 "없을 때만" 계정/일기/분석을 생성한다.
    // 동작 흐름:
    // 1) 테스트 계정이 없으면 회원가입 수행
    // 2) DB에서 해당 계정의 userId를 찾아 ownerId("USER_<id>") 생성
    // 3) 해당 ownerId로 이미 일기가 있으면 중복 삽입 방지(바로 종료)
    // 4) 과거 일기 + AI 분석 캐시를 묶어서 생성
    fun ensureSeedUserAndPastDiaries(context: Context) {

        // applicationContext를 MyApp으로 캐스팅해 AppService에 접근한다.
        // - 디버그 시드 로직이 UI 레이어에 의존하지 않도록 앱 전역 객체를 활용한다.
        val app = context.applicationContext as MyApp
        val appService = app.appService

        // 1) 계정이 없으면 가입시키기
        // - 내부적으로 비밀번호 해시 저장까지 포함된다.
        // 예외처리) 이미 가입된 이메일이면 EmailAlreadyUsed로 실패할 수 있는데, 시드는 "있으면 유지"가 목적이므로 실패해도 정상 흐름으로 본다.
        appService.signUp(
            email = SEED_EMAIL,
            password = SEED_PW.toCharArray(),
            passwordConfirm = SEED_PW.toCharArray(),
            nickname = SEED_NICK
        )

        // 2) DB에서 userId를 찾아 ownerId 만들기
        // - 앱의 데이터 소유자 키는 ownerId("USER_xxx" / "ANON_xxx") 형태를 사용한다.
        val helper = AppDbHelper(context.applicationContext)
        val db = helper.writableDatabase

        // EmailPolicy.normalize:
        // - DB에 저장/조회할 때 이메일을 동일 규칙으로 정규화해서 일관성을 유지한다.
        val normalized = EmailPolicy.normalize(SEED_EMAIL)

        // 예외처리) 어떤 이유로든 사용자를 찾지 못하면 시드를 진행할 수 없으므로 바로 종료한다.
        val found = UserDao(db).findByEmail(normalized) ?: return

        val userId = found.first.userId
        val ownerId = "USER_$userId"

        // 3) 이미 일기가 있으면 중복 시드 방지
        if (hasAnyEntries(db, ownerId)) return

        // 4) 과거 일기 + 상세 분석(캐시)까지 시드 생성
        seedPastDiaries(db, ownerId)

        // 예외처리) DB를 직접 열었으므로 사용 후 close로 자원 누수를 방지한다.
        db.close()
    }

    // hasAnyEntries : 특정 ownerId에 대해 diary_entries가 한 건이라도 존재하는지 확인한다.
    // 용도:
    // - 시드 데이터 중복 삽입을 방지하기 위한 사전 체크 함수
    private fun hasAnyEntries(db: SQLiteDatabase, ownerId: String): Boolean {
        return db.rawQuery(
            "SELECT COUNT(*) FROM ${AppDb.T.ENTRIES} WHERE owner_id=?", arrayOf(ownerId)
        ).use { c ->

            // 예외처리) COUNT(*)는 항상 1행이 나오므로 moveToFirst() 후 0번째 컬럼을 읽는다.
            c.moveToFirst()
            c.getInt(0) > 0
        }
    }

    // seedPastDiaries : 날짜/감정/태그/본문/AI분석을 생성해 DB에 저장한다.
    private fun seedPastDiaries(db: SQLiteDatabase, ownerId: String) {
        val diaryDao = DiaryDao(db)
        val analysisDao = AnalysisDao(db)

        // 날짜 문자열(YYYY-MM-DD) 생성을 위한 포맷터
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

        // createdAt/updatedAt에 공통으로 넣을 기준 시각(시드 생성 시각)
        val now = System.currentTimeMillis()

        // 월간요약/캘린더 검증을 위해 "지난달/이번달" 기준으로 날짜를 만든다.
        val lastYm = DateUtil.previousMonthYm()      // 예: 2025-12
        val thisYm = DateUtil.thisMonthYm()          // 예: 2026-01

        // 지난달 8개, 이번달 4개 (총 12개)
        // - SEED_DIARY_COUNT로 최종 개수는 제한된다.
        val lastMonthDays = listOf(3, 5, 8, 12, 15, 18, 22, 26)
        val thisMonthDays = listOf(2, 6, 9, 13)

        // ymdOf : "YYYY-MM" + day(Int) 조합을 "YYYY-MM-DD" 문자열로 변환한다.
        // - Calendar를 이용해 0시 0분 0초 기준으로 날짜를 고정한다.
        fun ymdOf(ym: String, day: Int): String {
            val y = ym.take(4).toInt()
            val m = ym.drop(5).take(2).toInt()
            val c = Calendar.getInstance()
            c.set(y, m - 1, day, 0, 0, 0)
            c.set(Calendar.MILLISECOND, 0)
            return fmt.format(c.time)
        }

        // 시드로 넣을 전체 날짜 리스트 생성
        // - 지난달 날짜 + 이번달 날짜를 합친 뒤 SEED_DIARY_COUNT만큼만 사용한다.
        val allDates = buildList {
            lastMonthDays.forEach { add(ymdOf(lastYm, it)) }
            thisMonthDays.forEach { add(ymdOf(thisYm, it)) }
        }.take(SEED_DIARY_COUNT)

        // makeDiaryContent : 감정(mood)에 맞는 일기 본문을 생성한다.
        // 목적:
        // - 단순 더미 텍스트가 아닌, 감정/상황이 드러나는 문장을 만들어 AI 분석/마음카드 UI가 자연스럽게 보이게 한다.
        // - intro + mood별 문장 + closing 구조로 3단 구성한다.
        fun makeDiaryContent(mood: Mood, ymd: String, idx: Int): String {
            val introPool = listOf(
                "오늘은 유난히 시간이 빠르게 지나갔다.",
                "아침부터 머리가 복잡했는데, 막상 하루를 보내고 나니 조금 정리된 느낌이다.",
                "별일 없는 하루라고 생각했는데, 돌이켜보니 꽤 많은 일이 있었다.",
                "괜히 마음이 들뜨기도 하고, 가라앉기도 했다. 요즘 내 마음은 롤러코스터 같다."
            )

            val joyPool = listOf(
                "작게나마 좋은 소식이 있어서 기분이 한결 가벼웠다. 이런 날이 자주 있었으면!",
                "오랜만에 웃을 일이 많았다. 사소한 농담 하나에도 크게 웃어서 좀 민망했지만 좋았다.",
                "내가 해낸 게 별거 아닌데도 스스로 대견했다. 오늘은 나 칭찬해도 되는 날."
            )

            val calmPool = listOf(
                "혼자 걷는 시간이 좋았다. 생각을 정리하는 데에는 역시 산책이 최고다.",
                "하고 싶은 일을 차근차근 정리했다. 마음이 조용해지니까 집중도 잘 됐다.",
                "따뜻한 차 한 잔을 마시면서 숨을 고르니, 괜히 급했던 마음이 풀렸다."
            )

            val confidencePool = listOf(
                "미뤄두던 일을 드디어 끝냈다. ‘할 수 있다’는 감각이 오랜만에 돌아왔다.",
                "오늘은 이상하게도 내가 꽤 괜찮은 사람처럼 느껴졌다. 이 기세를 이어가고 싶다.",
                "실수도 있었지만 그걸 바로잡는 과정에서 스스로 성장하는 느낌을 받았다."
            )

            val normalPool = listOf(
                "특별한 일은 없었지만, 그래서 오히려 편했다. 평범함이 주는 안정이 있다.",
                "해야 할 일을 적당히 하고, 적당히 쉬었다. 무난한 하루도 나쁘지 않다.",
                "하루가 밋밋해서 기록할 게 없을 줄 알았는데, 이런 날도 쌓이면 내 삶이 되겠지."
            )

            val tiredPool = listOf(
                "몸이 무거웠다. 커피를 마셔도 해결이 안 되는 피곤함이 있었다.",
                "집에 오자마자 눕고 싶었다. 오늘은 그냥 ‘버틴 것’만으로도 충분한 것 같다.",
                "해야 할 일이 남아 있는데 눈꺼풀이 계속 내려온다. 내일의 나에게 맡기자."
            )

            val depressedPool = listOf(
                "마음이 쉽게 가라앉았다. 이유를 찾으려 해도 딱 떨어지는 원인이 없다.",
                "괜찮아지려고 애쓰는데 오히려 더 지친 느낌이다. 오늘은 그냥 받아들이기로 했다.",
                "비교를 멈추고 싶은데 자꾸 남들 속도가 눈에 들어온다. 나만의 속도를 믿고 싶다."
            )

            val angryPool = listOf(
                "사소한 말 한마디에 감정이 확 올라왔다. 나도 모르게 예민해져 있었다.",
                "억울한 기분이 남아서 하루 종일 찝찝했다. 그래도 숨 고르고 다시 생각해보려 했다.",
                "짜증이 잦아든 뒤에야 알았다. 사실은 피곤해서 그랬던 것 같다."
            )

            // mood별 문장 풀에서 idx에 따라 하나를 선택한다.
            // 예외처리) getOrNull을 사용해 인덱스 오류를 방지하고, 없으면 빈 문자열로 대체한다.
            val extraLine = when (mood) {
                Mood.JOY -> joyPool
                Mood.CALM -> calmPool
                Mood.CONFIDENCE -> confidencePool
                Mood.NORMAL -> normalPool
                Mood.TIRED -> tiredPool
                Mood.DEPRESSED -> depressedPool
                Mood.ANGRY -> angryPool
            }.getOrNull(idx % 3).orEmpty()

            val closingPool = listOf(
                "내일은 오늘보다 조금 더 가벼운 하루였으면 좋겠다.",
                "기록하고 나니 마음이 한 톨 정도는 정리된 느낌이다.",
                "오늘의 감정은 오늘에 두고, 내일은 새로 시작해보자."
            )

            val intro = introPool[idx % introPool.size]
            val closing = closingPool[(idx + 1) % closingPool.size]

            // intro -> mood 문장 -> closing 순서로 본문 구성
            return buildString {
                append(intro).append("\n")
                append(extraLine).append("\n")
                append(closing)
            }
        }

        // makeSeedAnalysis : 저장된 일기(entryId)에 대응하는 "AI 분석 결과"를 가짜로 만들어 저장한다.
        // 목적:
        // - 실제 Gemini 호출 없이도 분석 화면/마음카드 UI가 동작하는지 검증 가능하게 한다.
        // - fullText는 프리뷰 카드에서 일부만 노출되므로, 첫 문장이 위로/격려 톤으로 시작하도록 구성한다.
        fun makeSeedAnalysis(
            entryId: Long, mood: Mood, tags: List<String>, ymd: String, idx: Int
        ): AiAnalysis {

            // comfortFirst : 화면에서 바로 보이는 첫 문단(위로/격려)을 감정별로 분기한다.
            val comfortFirst = when (mood) {
                Mood.JOY -> "오늘의 기쁨은 생각보다 오래 남아요. 잘해낸 나를 가볍게 칭찬해줘도 좋아요."
                Mood.CALM -> "지금처럼 숨이 고르게 돌아온 순간이 참 소중해요. 급하지 않아도 괜찮아요."
                Mood.CONFIDENCE -> "오늘 해낸 것들이 분명히 있어요. 완벽이 아니라 ‘전진’이 핵심이에요."
                Mood.NORMAL -> "평범한 하루도 충분히 의미 있어요. 기록한 순간들이 내일의 나를 지탱해줘요."
                Mood.TIRED -> "피곤함은 게으름이 아니라 ‘신호’예요. 오늘은 여기까지도 충분해요."
                Mood.DEPRESSED -> "마음이 가라앉는 날엔 억지로 밝아지려 하지 않아도 돼요. 지금 그대로 괜찮아요."
                Mood.ANGRY -> "화가 난 건 그만큼 소중한 걸 지키고 싶었다는 뜻일 수 있어요. 감정은 나쁜 게 아니에요."
            }

            // summary : 분석 리스트/마음카드에서 보여줄 짧은 요약 문장
            val summary = when (mood) {
                Mood.JOY -> "작은 성취가 기분을 가볍게 만든 하루예요."
                Mood.CALM -> "속도를 낮추며 마음을 정돈한 하루예요."
                Mood.CONFIDENCE -> "해냈다는 감각이 자신감을 키운 하루예요."
                Mood.NORMAL -> "무난한 일상 속에서 안정감을 찾은 하루예요."
                Mood.TIRED -> "에너지가 부족해 ‘정리’와 ‘휴식’이 필요한 하루예요."
                Mood.DEPRESSED -> "감정이 가라앉았지만, 스스로를 붙잡으려 한 하루예요."
                Mood.ANGRY -> "예민함 뒤에 피로/압박이 숨어 있을 수 있는 하루예요."
            }

            // trigger : 감정 원인/패턴(트리거) 설명
            val trigger = when (mood) {
                Mood.TIRED, Mood.DEPRESSED -> "할 일이 겹치거나 기대치가 올라갈 때, 몸/마음이 먼저 지치는 패턴"
                Mood.ANGRY -> "상황을 통제하기 어렵다고 느끼는 순간, 감정이 빠르게 치솟는 패턴"
                Mood.CONFIDENCE -> "작은 성공 경험이 쌓이면 동력이 커지는 패턴"
                Mood.CALM -> "걷기/정리 같은 루틴이 마음을 안정시키는 패턴"
                Mood.JOY -> "좋은 소식·관계의 따뜻함이 기분을 끌어올리는 패턴"
                Mood.NORMAL -> "큰 이벤트가 없을수록 오히려 컨디션이 안정되는 패턴"
            }

            // actions : 사용자가 바로 실행할 수 있는 미션 제안 리스트
            val actions = when (mood) {
                Mood.TIRED -> listOf(
                    "내일 바로 시작할 페이지만 펼쳐두기", "시원한 물 한 잔 마시고 어깨 힘 빼기", "내일 할 일을 3개만 적고 나머지는 ‘보류’로 두기"
                )

                Mood.DEPRESSED -> listOf(
                    "오늘 마음을 한 단어로만 정리해보기", "10분만 산책하거나 창문 열고 바람 쐬기", "내가 해낸 작은 일 1개를 적고 스스로 인정하기"
                )

                Mood.ANGRY -> listOf(
                    "숨 4초 들이마시고 6초 내쉬기 5번", "짜증 포인트를 ‘사실/해석’으로 나눠 적기", "스트레칭 2분으로 몸의 열 빼기"
                )

                Mood.CONFIDENCE -> listOf(
                    "오늘 잘한 점 2개를 적어 근거 있는 자신감 만들기",
                    "내일의 가장 중요한 1개 목표만 먼저 정하기",
                    "집중을 깨는 요소 1개 치우기(알림/책상)"
                )

                Mood.CALM -> listOf(
                    "5분만 조용히 앉아서 몸 감각 체크하기", "따뜻한 차/물로 루틴 만들기", "잠들기 전, 고마운 순간 1개 적기"
                )

                Mood.JOY -> listOf(
                    "좋았던 일을 한 문장으로 기록해 ‘기쁨 저장’하기", "고마웠던 사람에게 짧게 메시지 보내기", "내일 기대되는 것 1개 정해보기"
                )

                Mood.NORMAL -> listOf(
                    "오늘 컨디션을 10점 만점으로 체크하기", "내일을 위한 작은 준비 1개 해두기", "하루 마무리 루틴 만들기(정리 5분)"
                )
            }

            // missionSummary : actions 전체를 아우르는 1줄 메시지(요약/격려 문장)
            val missionSummary = when (mood) {
                Mood.TIRED -> "오늘의 꿀잠은 도망이 아니라, 내일을 위한 도움닫기예요. 오늘은 여기까지도 충분해요."
                Mood.DEPRESSED -> "감정이 무거운 날엔 속도를 낮추는 게 회복의 시작이에요. 오늘은 나를 덜 몰아붙여요."
                Mood.ANGRY -> "감정의 열을 낮추면 생각이 또렷해져요. ‘지금’의 나를 먼저 진정시켜줘요."
                Mood.CONFIDENCE -> "잘하고 있다는 근거를 모으면 자신감은 더 단단해져요. 작은 전진을 이어가요."
                Mood.CALM -> "차분한 루틴은 마음의 안전지대가 돼요. 오늘 만든 평온을 내일도 이어가요."
                Mood.JOY -> "좋은 순간을 붙잡는 연습은 기분을 오래 지켜줘요. 오늘의 기쁨을 저장해요."
                Mood.NORMAL -> "무난한 하루도 ‘쌓이면 실력’이에요. 오늘의 기록이 내일을 더 편하게 해줘요."
            }

            // hashtags : 태그 + 감정 기반 해시태그를 조합해 중복을 제거한다.
            // - tags에서 일부를 가져오고, 감정에 맞는 키워드를 추가한다.
            val hashtags = buildList {
                addAll(tags.take(3))
                add(
                    when (mood) {
                        Mood.TIRED -> "마음챙김"
                        Mood.DEPRESSED -> "회복"
                        Mood.ANGRY -> "감정정리"
                        Mood.CONFIDENCE -> "성장"
                        Mood.CALM -> "호흡"
                        Mood.JOY -> "행복저장"
                        Mood.NORMAL -> "일상기록"
                    }
                )
                add(if (idx % 2 == 0) "오늘은_여기까지" else "긍정회로")
            }.distinct()

            // fullText : 상세 분석 본문(긴 텍스트)
            // - 실제 AI가 만든 것처럼 자연스럽게 연결되도록 문장을 이어 붙인다.
            val fullText = buildString {
                append(comfortFirst).append(" ")
                append(summary).append(" ")
                append("오늘 기록을 보면, ").append(trigger).append("이 보여요. ")
                append("이럴 때는 ‘더 세게’가 아니라 ‘더 작게’가 오히려 도움이 돼요.")
                append("지금의 나는 휴식/정리/재시작이 필요한 구간에 있을 가능성이 커요.")
                append("지금 할 수 있는 가장 쉬운 선택부터 해볼까요? ")
                append("아주 작은 행동 하나가 내일의 부담을 확 줄여줘요.")
            }

            // AiAnalysis 모델로 반환(DAO에서 JSON 직렬화되어 DB에 저장됨)
            return AiAnalysis(
                entryId = entryId,
                summary = summary,
                triggerPattern = trigger,
                actions = actions,
                hashtags = hashtags,
                missionSummary = missionSummary,
                fullText = fullText
            )
        }

        // 날짜별로 일기 + 분석을 저장한다.
        for ((idx, ymd) in allDates.withIndex()) {
            val isLastMonth = ymd.startsWith(lastYm)

            // 감정 분포를 의도적으로 구성한다.
            // - 지난달: JOY가 최다(월간 대표 감정 검증)
            // - 이번달: 다양한 감정(그래프/목록 다양성)
            val mood = if (isLastMonth) {
                // 지난달 8개 중 JOY 6개, TIRED 1개, DEPRESSED 1개
                when (idx % 8) {
                    0, 1, 2, 3, 4, 5 -> Mood.JOY
                    6 -> Mood.TIRED
                    else -> Mood.DEPRESSED
                }
            } else {
                // 이번달 4개는 다양하게
                when (idx % 4) {
                    0 -> Mood.CALM
                    1 -> Mood.CONFIDENCE
                    2 -> Mood.NORMAL
                    else -> Mood.ANGRY
                }
            }

            // 감정에 맞는 기본 태그 세트 구성
            val tags = when (mood) {
                Mood.JOY -> listOf("행복", "만족")
                Mood.CALM -> listOf("휴식", "여유")
                Mood.CONFIDENCE -> listOf("성취", "자신감")
                Mood.NORMAL -> listOf("일상", "기록")
                Mood.TIRED -> listOf("피곤", "컨디션")
                Mood.DEPRESSED -> listOf("우울", "생각")
                Mood.ANGRY -> listOf("짜증", "스트레스")
            }

            // DiaryEntry 엔티티 생성
            val entry = DiaryEntry(
                entryId = 0L,
                ownerId = ownerId,
                dateYmd = ymd,
                title = if (isLastMonth) "지난달 예시 기록 ${idx + 1}" else "이번달 예시 기록 ${idx + 1}",
                content = makeDiaryContent(mood, ymd, idx),
                mood = mood,
                tags = tags,
                isFavorite = false,
                isTemporary = false,
                createdAt = now,
                updatedAt = now
            )

            // 1) 일기 저장
            // - DiaryDao.upsert는 (owner_id, date_ymd) UNIQUE 제약을 기준으로 insert/update를 수행한다.
            val savedEntryId = diaryDao.upsert(entry)

            // 2) 저장 직후 상세 분석도 같이 저장(캐시)
            // - 분석 화면은 entryId 기준으로 ai_analysis를 조회하므로, 미리 넣어두면 즉시 확인 가능하다.
            val analysis = makeSeedAnalysis(
                entryId = savedEntryId, mood = mood, tags = tags, ymd = ymd, idx = idx
            )
            analysisDao.upsert(analysis)
        }

        // 추가 일기 1개를 더 생성한다(분석/카드 검증용)
        // - 지난달 28일 TIRED로 넣되, 지난달 대표 감정(JOY)이 최다인 상태는 유지한다.
        run {
            val extraYmd = ymdOf(lastYm, 28)

            val extraEntry = DiaryEntry(
                entryId = 0L,
                ownerId = ownerId,
                dateYmd = extraYmd,
                title = "추가 기록(상세 분석 검증)",
                content = """
                    해야 할 일이 몰리니까 머릿속이 복잡해졌다.
                    작은 실수에도 예민해지는 게 느껴져서 잠깐 숨을 고르기로 했다.

                    오늘은 체크리스트를 다시 만들고, 당장 해야 할 것부터 하나씩 줄였다.
                    ‘지금 다 못해도 괜찮다’고 말해주니 마음이 조금 가벼워졌다.

                    내일은 집중 시간을 짧게 끊어서, 부담 없이 시작해보자.
                """.trimIndent(),
                mood = Mood.TIRED,
                tags = listOf("마감", "체크리스트", "회복"),
                isFavorite = false,
                isTemporary = false,
                createdAt = now,
                updatedAt = now
            )

            val extraEntryId = diaryDao.upsert(extraEntry)

            // 추가 일기도 분석 캐시 저장(상세/이미지 카드에서 즉시 사용 가능)
            val extraAnalysis = makeSeedAnalysis(
                entryId = extraEntryId,
                mood = Mood.TIRED,
                tags = extraEntry.tags,
                ymd = extraYmd,
                idx = 999
            )
            analysisDao.upsert(extraAnalysis)
        }
    }
}
