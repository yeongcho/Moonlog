package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.db.DiaryEntryReader
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil
import com.example.guru2_android_team04_android.util.MindCardTextUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TodayDiaryDetailUiBinder : 오늘의 일기 보기 화면(activity_today_diary_detail.xml) <-> AppService 연동 전담 클래스
// 용도:
// - entryId로 DB에서 일기 원문을 읽어 화면에 표시한다.
// - 마음 카드 프리뷰(위로 2줄 + 미션 1개)를 조회해 화면에 표시한다.
// - 수정/삭제/즐겨찾기/상세 분석 버튼의 클릭 이벤트를 연결한다.
// 설계:
// - IO 작업(DB 조회/업데이트)은 Dispatchers.IO에서 수행한다.
// - UI 업데이트는 Dispatchers.Main에서 수행한다.
class TodayDiaryDetailUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    // DiaryEntryReader : entryId로 일기를 읽기 위한 읽기 전용 헬퍼
    // - 상세 화면에서 일기 원문(제목/본문/감정 등)을 표시하기 위해 사용
    private val reader = DiaryEntryReader(activity)

    // bind : entryId에 해당하는 일기 상세 화면을 구성한다.
    // 동작 흐름:
    // 1) entryId 유효성 검사
    // 2) View 참조 획득
    // 3) IO에서 일기 로드 + 프로필(닉네임) + 마음카드 프리뷰 조회
    // 4) Main에서 화면 표시 + 버튼 이벤트(즐겨찾기/수정/삭제/분석) 연결
    fun bind(entryId: Long) {

        // 예외처리) entryId가 올바르지 않으면 더 진행할 수 없으므로 토스트 후 종료한다.
        if (entryId <= 0L) {
            Toast.makeText(activity, "entryId가 올바르지 않아요.", Toast.LENGTH_SHORT).show()
            activity.finish()
            return
        }

        // XML View 참조들 (일기 카드 영역)
        val tvDate = activity.findViewById<TextView>(R.id.tvDiaryDayText)
        val ivMood = activity.findViewById<ImageView>(R.id.ivDetailEmotion)
        val tvMood = activity.findViewById<TextView>(R.id.tvDetailEmotionTag)
        val tvTitle = activity.findViewById<TextView>(R.id.tvDiaryTitle)
        val tvContent = activity.findViewById<TextView>(R.id.tvDiaryContent)

        // XML View 참조들 (수정/삭제/즐겨찾기)
        val tvEdit = activity.findViewById<TextView>(R.id.tvDetailEdit)
        val tvDelete = activity.findViewById<TextView>(R.id.tvDetailDelete)
        val ivFav = activity.findViewById<ImageView>(R.id.ivDetailFavorites)

        // XML View 참조들 (마음 카드 영역: 위로 2줄 + 미션)
        val tvComfort = activity.findViewById<TextView>(R.id.tvNicknameMsg)
        val tvConsole = activity.findViewById<TextView>(R.id.tvConsoleText)
        val tvMission = activity.findViewById<TextView>(R.id.tvMissionText)

        // XML View 참조들 (상세 분석 보러가기 버튼)
        val btnAnalysis =
            activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save_card)

        // DB/서비스 호출은 IO에서 수행
        activity.lifecycleScope.launch(Dispatchers.IO) {

            // 1) 일기 원문 로드
            val entry = reader.getByIdOrNull(entryId)

            // 예외처리) 일기가 DB에 없으면(삭제되었거나 잘못된 id) 화면을 유지할 수 없으므로 종료한다.
            if (entry == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "일기를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
                return@launch
            }

            // 2) 사용자 프로필(닉네임) 조회
            // - 마음 카드 위로 문구에 닉네임을 넣기 위해 필요하다.
            val profile = appService.getUserProfile()
            val nickname = profile.nickname

            // 3) 마음 카드 프리뷰 조회
            // - Gemini 분석이 있으면 프리뷰(comfortPreview, mission)를 받고 실패하면 null로 두고 기본 문구를 사용한다.
            val preview = when (val r = appService.getMindCardPreviewByEntryIdSafe(entryId)) {
                is AppResult.Success -> r.data
                is AppResult.Failure -> null
            }

            // 화면 반영/이벤트 연결은 Main에서 수행
            withContext(Dispatchers.Main) {

                // 날짜/제목/본문 표시
                // - dateYmd("YYYY-MM-DD")를 한국어 보기 형식으로 변환해서 표시한다.
                tvDate.text = DateUtil.ymdToPrettyKo(entry.dateYmd)
                tvTitle.text = entry.title
                tvContent.text = entry.content

                // 감정 아이콘 + 감정 라벨(태그) 표시
                ivMood.setImageResource(iconResOf(entry.mood))
                tvMood.text = "태그: ${moodKo(entry.mood)}"

                // 마음 카드 프리뷰 표시
                // - MindCardTextUtil 규칙으로 2줄(첫줄/둘째줄)을 만들어 UI에 동일하게 적용한다.
                val (line1, line2) = MindCardTextUtil.makeComfortLines(nickname, preview?.comfortPreview)
                tvComfort.text = line1
                tvConsole.text = line2

                // 미션 표시
                // 예외처리) preview가 없으면 기본 미션 문구를 사용한다.
                tvMission.text = "오늘의 미션: ${preview?.mission ?: "천천히 숨 고르기"}"

                // 즐겨찾기(하트) 토글 기능
                // - 현재 상태(entry.isFavorite)를 기준으로 아이콘을 초기화한다.
                // - 클릭 시 DB 업데이트 후 성공하면 아이콘/상태를 갱신한다.
                var fav = entry.isFavorite
                ivFav.setImageResource(if (fav) R.drawable.ic_favorites_o else R.drawable.ic_favorites_x)
                ivFav.setOnClickListener {
                    val owner = entry.ownerId
                    val next = !fav

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val ok = appService.setEntryFavorite(owner, entryId, next)
                        withContext(Dispatchers.Main) {
                            if (ok) {
                                fav = next
                                ivFav.setImageResource(if (fav) R.drawable.ic_favorites_o else R.drawable.ic_favorites_x)
                            } else {
                                Toast.makeText(activity, "즐겨찾기 변경에 실패했어요.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // 수정 버튼
                // - DiaryEditorActivity로 entryId를 넘겨 "수정 모드"로 진입한다.
                tvEdit.setOnClickListener {
                    activity.startActivity(
                        Intent(activity, DiaryEditorActivity::class.java).apply {
                            putExtra("entryId", entryId)
                        }
                    )
                }

                // 삭제 버튼
                // - DB에서 일기를 삭제한 뒤 홈 화면으로 이동한다.
                // 예외처리) 삭제는 DB 작업이므로 IO에서 처리하고, 이동/finish는 Main에서 수행한다.
                tvDelete.setOnClickListener {
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        appService.deleteEntry(entryId)
                        withContext(Dispatchers.Main) {
                            activity.startActivity(Intent(activity, HomeActivity::class.java))
                            activity.finish()
                        }
                    }
                }

                // 상세 분석 보러가기 버튼
                // - 분석 시작 화면으로 entryId를 전달한다.
                btnAnalysis.setOnClickListener {
                    activity.startActivity(
                        Intent(activity, AnalysisStartActivity::class.java).apply {
                            putExtra("entryId", entryId)
                        }
                    )
                }
            }
        }
    }

    // moodKo : Mood enum을 화면용 한글 라벨로 변환한다.
    // - 상세 화면의 "태그: 기쁨/평온 ..." 표시용
    private fun moodKo(m: Mood): String = when (m) {
        Mood.JOY -> "기쁨"
        Mood.CONFIDENCE -> "자신감"
        Mood.CALM -> "평온"
        Mood.NORMAL -> "평범"
        Mood.DEPRESSED -> "우울"
        Mood.ANGRY -> "분노"
        Mood.TIRED -> "피곤함"
    }

    // iconResOf : Mood에 따라 감정 아이콘 리소스를 결정한다.
    // - 상세 화면 상단의 ivDetailEmotion에 표시된다.
    private fun iconResOf(mood: Mood): Int = when (mood) {
        Mood.JOY -> R.drawable.emotion_joy
        Mood.CONFIDENCE -> R.drawable.emotion_confidence
        Mood.CALM -> R.drawable.emotion_calm
        Mood.NORMAL -> R.drawable.emotion_normal
        Mood.DEPRESSED -> R.drawable.emotion_sad
        Mood.ANGRY -> R.drawable.emotion_angry
        Mood.TIRED -> R.drawable.emotion_tired
    }
}