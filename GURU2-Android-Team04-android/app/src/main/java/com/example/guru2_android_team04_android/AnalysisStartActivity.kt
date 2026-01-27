package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.data.db.DiaryEntryReader
import com.example.guru2_android_team04_android.util.DateUtil
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

// AnalysisStartActivity : 상세 분석 시작 안내 화면 Activity
// 용도:
// - 상세 분석을 시작하기 전 안내 화면을 제공한다.
// - 분석 대상 일기의 날짜를 화면에 표시한다.
// - 분석 시작하기 버튼으로 다음 화면(AnalysisDiaryActivity)로 이동한다.
// 동작 흐름:
// 1) Intent로 전달된 entryId를 검증한다.
// 2) entryId로 DB에서 일기를 조회하여, 일기 작성 날짜(entry.dateYmd)를 "예쁜 한국어 날짜"로 변환해 표시한다.
// 3) 버튼 클릭 시 entryId를 그대로 다음 화면으로 전달한다.
// 설계:
// - 일기 조회는 DiaryEntryReader(로컬 DB 읽기 헬퍼)를 사용한다.
// - DB 조회/날짜 변환은 IO 스레드에서 처리하고, 화면 반영은 Main 스레드에서 처리한다.
class AnalysisStartActivity : AppCompatActivity() {

    // reader : entryId로 일기 데이터를 읽기 위한 DB Reader
    // - 분석 화면 흐름에서 "분석 대상 일기"를 확인/표시할 때 사용한다.
    private val reader by lazy { DiaryEntryReader(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_start)

        // entryId : 분석할 일기의 PK
        // - 이전 화면(TodayDiaryDetailActivity 등)에서 putExtra("entryId", ...)로 전달받는다.
        val entryId = intent.getLongExtra("entryId", -1L)

        // tv_analysis_date : XML에서 분석 대상 날짜를 보여주는 TextView
        // - activity_analysis_start.xml에 id가 존재하므로 findViewById로 직접 접근한다.
        val tvDate = findViewById<TextView>(R.id.tv_analysis_date)

        // 예외처리) entryId가 없거나 0 이하이면 분석 대상이 없으므로 토스트 후 화면을 종료한다.
        if (entryId <= 0L) {
            Toast.makeText(this, "entryId가 올바르지 않아요.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 분석 대상 날짜 표시
        // - entry.dateYmd("yyyy-MM-dd")를 화면용 "yyyy년 M월 d일 E요일"로 변환해 보여준다.
        // - 예외: 일기를 찾지 못하면(삭제/오류) 오늘 날짜로 fallback 한다.
        lifecycleScope.launch(Dispatchers.IO) {
            val entry = reader.getByIdOrNull(entryId)

            val pretty = if (entry != null) {
                // entry.dateYmd를 "한국어 예쁜 날짜"로 변환
                ymdToPrettyKo(entry.dateYmd)
            } else {
                // 예외처리) entryId는 유효했지만 DB에서 못 찾는 경우가 있을 수 있으므로 오늘로 대체한다.
                DateUtil.todayPrettyKo()
            }

            withContext(Dispatchers.Main) {
                tvDate.text = pretty
            }
        }

        // 분석 시작하기 버튼
        // - 다음 화면(AnalysisDiaryActivity)으로 이동하면서 entryId를 그대로 전달한다.
        findViewById<MaterialButton>(R.id.btn_start_analysis).setOnClickListener {
            startActivity(Intent(this, AnalysisDiaryActivity::class.java).apply {
                putExtra("entryId", entryId)
            })
        }
    }

    // ymdToPrettyKo : "yyyy-MM-dd" 문자열을 화면용 "yyyy년 M월 d일 E요일"로 변환한다.
    // 용도:
    // - 분석 시작 화면에서 "분석 대상 날짜"를 사람이 읽기 쉬운 형태로 표시한다.
    // 예외처리) 파싱 실패(형식 불일치/빈 문자열 등) 시 앱이 죽지 않도록 오늘 날짜로 대체한다.
    private fun ymdToPrettyKo(ymd: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
            val outFmt = SimpleDateFormat("yyyy년 M월 d일 E요일", Locale.KOREA)
            outFmt.format(inFmt.parse(ymd)!!)
        } catch (_: Exception) {
            // 예외처리) 날짜 파싱 실패 시 오늘 날짜로 fallback
            DateUtil.todayPrettyKo()
        }
    }
}
