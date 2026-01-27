package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.AppService
import com.example.guru2_android_team04_android.R
import com.example.guru2_android_team04_android.TodayDiaryDetailActivity
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.db.DiaryEntryReader
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.MindCardPreview
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil
import com.example.guru2_android_team04_android.data.model.MindCardPreviewResult
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// DiaryEditorUiBinder : 일기 작성 화면(activity_diary_editor.xml) <-> AppService 연동 전담 클래스
// 용도:
// - 화면에서 입력값을 읽고, 저장 버튼 이벤트를 처리한다.
// - 신규 작성 모드: 일기 저장 + AI 프리뷰/분석 준비까지 수행한 뒤 상세 화면으로 이동한다.
// - 수정 모드: 기존 일기 내용을 불러와 입력 폼에 채우고, 저장 시 DB 업데이트를 수행한다.
//
// 설계:
// - Activity는 화면 표시에 집중하고, 이 Binder는 UI 이벤트/데이터 흐름을 담당해 코드 복잡도를 낮춘다.
class DiaryEditorUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    // DiaryEntryReader : entryId로 일기 원문을 읽기 위한 읽기 전용 헬퍼
    // - 수정 모드에서 기존 내용을 채울 때 사용한다.
    private val reader = DiaryEntryReader(activity)

    // bind : 작성/수정 화면을 실제로 동작하게 연결하는 진입 메서드
    // 매개변수:
    // - editEntryId : 수정할 entryId (null이면 신규 작성 모드)
    // 동작 흐름:
    // 1) XML의 View들을 찾아서 참조를 만든다.
    // 2) 오늘 날짜를 표시한다.
    // 3) 수정 모드면 entryId로 기존 일기를 조회해 입력 폼에 채운다.
    // 4) 감정 선택 변경 시 미리보기 아이콘을 즉시 업데이트한다.
    // 5) 저장 버튼 클릭 시 입력값으로 DiaryEntry를 만들고 저장 후 상세 화면으로 이동한다.
    fun bind(editEntryId: Long?) {

        // XML View 참조들
        val tvDate = activity.findViewById<TextView>(R.id.tvDiaryDayText)
        val rgMood = activity.findViewById<RadioGroup>(R.id.rg_mood)
        val ivPreview = activity.findViewById<ImageView>(R.id.iv_emotion_preview)
        val etTitle = activity.findViewById<EditText>(R.id.et_title)
        val etContent = activity.findViewById<EditText>(R.id.et_content)
        val btnSave = activity.findViewById<MaterialButton>(R.id.btn_save_card)

        // 오늘 날짜 표시(작성 화면 상단)
        tvDate.text = DateUtil.todayPrettyKo()

        // 수정 모드면 기존 데이터 로드 후 폼에 채우기
        // 예외처리) DB에 entryId가 없을 수 있으므로(getByIdOrNull), null이면 신규 작성처럼 동작한다.
        val existing = editEntryId?.let { reader.getByIdOrNull(it) }
        if (existing != null) {
            etTitle.setText(existing.title)
            etContent.setText(existing.content)
            setMoodUi(existing.mood, rgMood, ivPreview)
        }

        // 감정 라디오 선택이 바뀔 때, 미리보기 아이콘을 즉시 변경
        rgMood.setOnCheckedChangeListener { _, _ ->
            val mood = moodFromUi(rgMood.checkedRadioButtonId)
            ivPreview.setImageResource(iconResOf(mood))
        }

        // 저장 버튼 클릭 처리
        btnSave.setOnClickListener {

            // 입력값 추출
            // - text가 null일 수 있으므로 orEmpty()로 안전하게 처리한다.
            val title = etTitle.text?.toString().orEmpty()
            val content = etContent.text?.toString().orEmpty()
            val mood = moodFromUi(rgMood.checkedRadioButtonId)

            // 저장/분석 작업은 IO에서 수행
            activity.lifecycleScope.launch(Dispatchers.IO) {

                // 현재 ownerId 확보
                // - 로그인 상태면 USER_xxx
                // - 아니면 ANON_xxx 세션을 시작해 ownerId를 만든다.
                // 예외처리) ownerId가 없으면 데이터 소유자 구분이 불가능하므로, 반드시 startAnonymousSession()으로 보장한다.
                val owner = appService.currentOwnerIdOrNull() ?: appService.startAnonymousSession()

                val now = System.currentTimeMillis()

                // 신규/수정에 따라 저장할 DiaryEntry 구성
                // - 수정 모드: 기존 엔티티를 copy로 갱신(updatedAt만 변경)
                // - 신규 모드: 오늘 날짜(DateUtil.todayYmd)로 새 엔티티 생성
                // - 비회원이면 isTemporary를 true로 두어(ANON_ prefix) 목록/통계에서 제외할 수 있다.
                val entry = if (existing != null) {
                    existing.copy(
                        title = title,
                        content = content,
                        mood = mood,
                        updatedAt = now
                    )
                } else {
                    DiaryEntry(
                        entryId = 0L,
                        ownerId = owner,
                        dateYmd = DateUtil.todayYmd(),
                        title = title,
                        content = content,
                        mood = mood,
                        tags = emptyList(),
                        isFavorite = false,
                        isTemporary = owner.startsWith("ANON_"),
                        createdAt = now,
                        updatedAt = now
                    )
                }

                // 저장 결과 구성:
                // - 신규 작성: 저장 + AI 프리뷰/마음 카드 준비까지 수행(appService.saveEntryAndPrepareMindCardSafe)
                // - 수정: 분석까지 강제하지 않고 저장만 수행(빠르게 끝내기 위한 정책)
                val r: AppResult<MindCardPreviewResult> = if (existing == null) {
                    appService.saveEntryAndPrepareMindCardSafe(entry)
                } else {
                    // 수정은 분석을 생략하고, UI 이동에 필요한 최소 프리뷰만 만들어 반환한다.
                    // 예외처리) 수정 후에도 상세 화면으로 이동하려면 entryId가 필요하므로, upsertEntry가 반환하는 id를 프리뷰에 넣어준다.
                    val id = appService.upsertEntry(entry)
                    AppResult.Success(
                        MindCardPreviewResult(
                            preview = MindCardPreview(
                                entryId = id,
                                dateYmd = entry.dateYmd,
                                title = entry.title,
                                mood = entry.mood,
                                tags = entry.tags,
                                comfortPreview = "수정이 완료됐어요.",
                                mission = "천천히 숨 고르기"
                            ),
                            analysisError = null
                        )
                    )
                }

                // UI 반영(토스트/화면 이동)은 Main에서 수행
                withContext(Dispatchers.Main) {
                    when (r) {

                        // 저장 성공:
                        // - 분석 실패 메시지가 있으면 토스트로만 안내하고 흐름은 계속 진행한다.
                        // - 오늘 일기 상세 화면으로 이동(entryId 전달) 후 현재 화면 종료
                        is AppResult.Success -> {

                            // 분석 실패가 있었다면 사용자에게 안내
                            r.data.analysisError?.let {
                                Toast.makeText(activity, it.userMessage, Toast.LENGTH_SHORT).show()
                            }

                            activity.startActivity(
                                Intent(activity, TodayDiaryDetailActivity::class.java).apply {
                                    putExtra("entryId", r.data.preview.entryId)
                                }
                            )
                            activity.finish()
                        }

                        // 저장 실패:
                        // - AppError.userMessage를 그대로 Toast로 표시한다.
                        is AppResult.Failure -> {
                            Toast.makeText(
                                activity,
                                r.error.userMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    // setMoodUi : 수정 모드에서 기존 mood 값을 라디오 선택/미리보기 아이콘에 반영한다.
    // - Mood(enum) → RadioButton id로 매핑 후 체크 상태를 맞춘다.
    private fun setMoodUi(mood: Mood, rg: RadioGroup, iv: ImageView) {
        val id = when (mood) {
            Mood.JOY -> R.id.rb_joy
            Mood.CONFIDENCE -> R.id.rb_confidence
            Mood.CALM -> R.id.rb_calm
            Mood.NORMAL -> R.id.rb_normal
            Mood.DEPRESSED -> R.id.rb_depression
            Mood.ANGRY -> R.id.rb_anger
            Mood.TIRED -> R.id.rb_fatigue
        }
        rg.check(id)
        iv.setImageResource(iconResOf(mood))
    }

    // moodFromUi : 현재 선택된 RadioButton id를 Mood enum으로 변환한다.
    // 예외처리) 예상치 못한 id가 들어오면 앱이 죽지 않도록 NORMAL로 처리한다.
    private fun moodFromUi(checkedId: Int): Mood = when (checkedId) {
        R.id.rb_joy -> Mood.JOY
        R.id.rb_confidence -> Mood.CONFIDENCE
        R.id.rb_calm -> Mood.CALM
        R.id.rb_normal -> Mood.NORMAL
        R.id.rb_depression -> Mood.DEPRESSED
        R.id.rb_anger -> Mood.ANGRY
        R.id.rb_fatigue -> Mood.TIRED
        else -> Mood.NORMAL
    }

    // iconResOf : Mood 값에 따라 미리보기 아이콘 리소스를 결정한다.
    // - 작성 화면 상단의 iv_emotion_preview에 표시된다.
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