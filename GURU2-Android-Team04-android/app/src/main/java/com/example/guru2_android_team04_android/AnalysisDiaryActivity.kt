package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.data.db.DiaryEntryReader
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil

// AnalysisDiaryActivity : 상세 분석 화면(1/3) - 일기 내용 카드 표시 Activity
// 용도:
// - 상세 분석을 시작하면, 먼저 분석할 일기의 내용을 카드 형태로 보여준다.
// - 날짜/감정 아이콘/감정 태그/제목/본문을 표시한다.
// - 하단 Tap 버튼(btnTapContinue)을 누르면 다음 화면(AnalysisComfortActivity)로 이동한다.
// 설계:
// - entryId로 DB에서 일기를 조회하고, 조회 결과를 UI에 바인딩한다.
class AnalysisDiaryActivity : AppCompatActivity() {

    // reader : entryId로 일기 원문을 읽기 위한 DB Reader
    private val reader by lazy { DiaryEntryReader(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_diary)

        // entryId : 분석할 일기의 PK
        val entryId = intent.getLongExtra("entryId", -1L)

        // entry : DB에서 읽어온 일기 데이터
        val entry = reader.getByIdOrNull(entryId)

        // 예외처리) entryId가 잘못되었거나 DB에서 일기를 찾지 못하면 화면을 구성할 수 없으므로 종료한다.
        if (entry == null) {
            Toast.makeText(this, "일기를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 날짜 표시 - 현재 코드는 오늘 날짜로 표시한다.
        findViewById<TextView>(R.id.tvDiaryDayText).text = DateUtil.todayPrettyKo()

        // 감정 태그(텍스트) + 감정 아이콘 표시
        findViewById<TextView>(R.id.tvDetailEmotionTag).text = "태그: ${moodKo(entry.mood)}"
        findViewById<ImageView>(R.id.ivDetailEmotion).setImageResource(iconResOf(entry.mood))

        // 제목/본문 표시
        findViewById<TextView>(R.id.tvDiaryTitle).text = entry.title
        findViewById<TextView>(R.id.tvDiaryContent).text = entry.content

        // 하단 Tap 버튼
        // - 다음 단계(위로/격려 화면)로 넘어가며 entryId를 그대로 전달한다.
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnTapContinue).setOnClickListener {
            startActivity(Intent(this, AnalysisComfortActivity::class.java).apply {
                putExtra("entryId", entryId)
            })
        }
    }

    // moodKo : Mood enum을 화면용 한글 라벨로 변환한다.
    // - activity_analysis_diary.xml의 "태그: 평온" 같은 문구 출력에 사용된다.
    private fun moodKo(m: Mood): String = when (m) {
        Mood.JOY -> "기쁨"
        Mood.CONFIDENCE -> "자신감"
        Mood.CALM -> "평온"
        Mood.NORMAL -> "평범"
        Mood.DEPRESSED -> "우울"
        Mood.ANGRY -> "분노"
        Mood.TIRED -> "피곤함"
    }

    // iconResOf : Mood에 따라 감정 아이콘 리소스를 선택한다.
    // - activity_analysis_diary.xml의 ivDetailEmotion에 표시된다.
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
