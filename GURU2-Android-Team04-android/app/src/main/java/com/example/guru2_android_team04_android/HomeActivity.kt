package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.model.ko
import com.example.guru2_android_team04_android.util.DateUtil
import com.example.guru2_android_team04_android.util.MindCardTextUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// HomeActivity : 앱의 메인 홈 화면 Activity
// 용도:
// - 상단 배너: 지난 달 월간 요약(대표 감정) 안내 + 화살표 클릭 시 월간 요약 화면으로 이동
// - 오늘 영역: 오늘 날짜 표시 + 이야기 들려주기 버튼(오늘 일기 작성/조회로 분기)
// - 마음 카드: 오늘 일기 분석 프리뷰(위로 2줄 + 오늘의 미션) 표시
// 동작 핵심:
// - DB/분석 조회는 IO 스레드에서 수행하고, UI 반영은 Main 스레드에서 수행한다.
// - 오늘 일기가 있으면 해당 entryId로 MindCardPreview를 조회해 홈 화면 문구를 개인화한다.
class HomeActivity : AppCompatActivity() {

    // appService : 앱의 비즈니스 로직 진입점
    // - 로그인/프로필/일기 조회/월간 요약 생성/마음 카드 프리뷰 조회 등을 담당한다.
    // - lazy로 선언해 Activity가 실제로 사용할 때 초기화되도록 한다.
    private val appService by lazy { (application as MyApp).appService }

    // onCreate : 홈 화면 초기화
    // 동작 흐름:
    // 1) 레이아웃 로드 + 하단 네비게이션 바 바인딩
    // 2) 오늘 날짜를 표시
    // 3) IO에서 프로필/월간요약/오늘일기/마음카드 프리뷰를 조회
    // 4) Main에서 배너/마음카드 UI를 업데이트하고 버튼 클릭 이벤트를 연결
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 하단 네비게이션 바 연결
        // - 현재 탭을 Home으로 활성화한다.
        com.example.guru2_android_team04_android.ui.bind.BottomNavBinder.bind(
            this, R.id.navigation_home
        )

        // 오늘 날짜 영역(activity_home.xml의 tvDiaryDayText)
        val tvDiaryDayText = findViewById<TextView>(R.id.tvDiaryDayText)

        // 상단 배너 영역
        // - tv_banner_title : "닉네임님의 1월 감정 요약"
        // - tv_banner_subtitle : "지난 달은 주로 '평온' 하셨네요."
        val tvBannerTitle = findViewById<TextView>(R.id.tv_banner_title)
        val tvBannerSubtitle = findViewById<TextView>(R.id.tv_banner_subtitle)

        // 배너 화살표 아이콘(클릭 시 월간 요약 화면으로 이동)
        val ivMonthlySummary = findViewById<ImageView>(R.id.ivMonthlySummary)

        // 마음 카드 영역(activity_home.xml 하단 카드)
        // - tvNicknameMsg / tvConsoleText : 위로/격려 문구 2줄
        // - tvMissionText : 오늘의 미션 1줄
        val tvNicknameMsg = findViewById<TextView>(R.id.tvNicknameMsg)
        val tvConsoleText = findViewById<TextView?>(R.id.tvConsoleText)
        val tvMissionText = findViewById<TextView>(R.id.tvMissionText)

        // 오늘 날짜를 보기 좋은 포맷으로 표시(예: "2026년 2월 6일 금요일")
        tvDiaryDayText.text = DateUtil.todayPrettyKo()

        // 데이터 로딩은 IO 스레드에서 수행
        lifecycleScope.launch(Dispatchers.IO) {

            // 현재 사용자 프로필 로드
            // - 닉네임, ownerId(USER_xxx / ANON_xxx) 등을 포함
            val profile = appService.getUserProfile()
            val ownerId = profile.ownerId

            // 지난달 yearMonth("yyyy-MM") 계산
            // - 홈 배너는 "지난 달 월간 요약"을 보여준다.
            val lastYm = DateUtil.previousMonthYm()

            // 월간 요약이 없으면 생성/캐시 보장하는 함수
            // - 반환값 monthly가 null이면 "요약 데이터가 아직 없음" 상태로 처리한다.
            val monthly = appService.ensureLastMonthMonthlySummary(ownerId)

            // 오늘 일기 작성 여부 확인:
            // 1) 오늘 날짜(yyyy-MM-dd)
            // 2) 오늘이 속한 달(yyyy-MM) 단위로 월 조회 후
            // 3) 그 중 오늘 날짜와 일치하는 entry를 찾는다.
            val todayYmd = DateUtil.todayYmd()
            val ym = todayYmd.take(7)
            val todayEntry =
                appService.getEntriesByMonth(ownerId, ym).firstOrNull { it.dateYmd == todayYmd }

            // 마음 카드 프리뷰 로드:
            // - 오늘 일기가 존재할 때만 entryId 기반으로 프리뷰를 요청한다.
            // - AppResult로 감싸져 있으므로 실패 시 null 처리한다
            val preview = if (todayEntry != null) {
                when (val r = appService.getMindCardPreviewByEntryIdSafe(todayEntry.entryId)) {
                    is AppResult.Success -> r.data
                    is AppResult.Failure -> null
                }
            } else null

            withContext(Dispatchers.Main) {

                // 배너 제목: 요구사항상 항상 보여야 하므로 profile + 지난달 월 숫자로 고정 생성
                // 예외처리) substring(5,7) 파싱 실패 가능성이 있어 toIntOrNull로 방어한다.
                val monthNum = lastYm.substring(5, 7).toIntOrNull() ?: 0
                tvBannerTitle.text = "${profile.nickname}님의 ${monthNum}월 감정 요약"

                // 배너 부제목:
                // - 월간 요약이 없으면 안내 문구
                // - 있으면 대표 감정을 한국어 라벨로 표시
                tvBannerSubtitle.text = if (monthly == null) {
                    "이번 달에는 열심히 일기를 써보아요!"
                } else {
                    // monthly.dominantMood.ko() : Mood enum -> 한국어 문자열 변환 확장 함수
                    "지난 달은 주로 '${monthly.dominantMood.ko()}' 하셨네요."
                }

                // 배너 화살표 클릭 시 월간 요약 화면으로 이동
                // - yearMonth를 extra로 전달해 해당 월 데이터를 보여주도록 한다.
                ivMonthlySummary.setOnClickListener {
                    // 비회원(ANON_...)은 월간 요약 화면 진입 불가
                    if (ownerId.startsWith("ANON_")) {
                        android.widget.Toast.makeText(
                            this@HomeActivity,
                            "회원만 월간 요약을 볼 수 있어요. 로그인 후 이용해주세요!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    startActivity(
                        Intent(this@HomeActivity, MonthlySummaryActivity::class.java).apply {
                            putExtra("yearMonth", lastYm)
                        })
                }

                // 마음 카드 위로/격려 문구 구성
                // - comfortPreview가 없으면 기본 문구를 내부 로직에서 구성할 수 있다.
                val nickname = profile.nickname
                val (line1, line2) = MindCardTextUtil.makeComfortLines(
                    nickname = nickname, comfortPreview = preview?.comfortPreview
                )

                // XML에 tvConsoleText가 존재하는 디자인(2줄 분리)과 없을 수도 있는 디자인(1개 TextView에 줄바꿈) 모두 대응한다.
                // 예외처리) tvConsoleText가 null인 경우에도 문구가 표시되도록 분기 처리한다.
                if (tvConsoleText != null) {
                    tvNicknameMsg.text = line1
                    tvConsoleText.text = line2
                } else {
                    tvNicknameMsg.text = "$line1\n$line2"
                }

                // 오늘의 미션:
                // - 프리뷰가 없으면 "이야기 들려주기 눌러보기" 같은 기본 미션을 표시한다.
                // - 프리뷰가 있으면 분석에서 생성된 mission을 노출한다.
                tvMissionText.text = if (preview == null) {
                    "오늘의 미션: '이야기 들려주기' 눌러보기"
                } else {
                    "오늘의 미션: ${preview.mission}"
                }

                // 이야기 들려주기 버튼:
                // - 오늘 일기가 없으면 오늘 일기 작성 화면(DiaryEditorActivity)으로 이동
                // - 오늘 일기가 있으면 오늘 일기 화면(TodayDiaryDetailActivity)으로 이동(entryId 전달)
                val btn =
                    findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btn_today_write)
                btn.setOnClickListener {
                    if (todayEntry == null) {
                        startActivity(Intent(this@HomeActivity, DiaryEditorActivity::class.java))
                    } else {
                        startActivity(
                            Intent(this@HomeActivity, TodayDiaryDetailActivity::class.java).apply {
                                putExtra("entryId", todayEntry.entryId)
                            })
                    }
                }
            }
        }
    }
}
