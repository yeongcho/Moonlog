package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.data.db.AppDbHelper
import com.example.guru2_android_team04_android.data.db.MonthlyDao
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// MonthlySummaryUiBinder : activity_monthly_summary.xml ↔ AppService 연동 전담 클래스
class MonthlySummaryUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    private val helper = AppDbHelper(activity.applicationContext)

    fun bind(yearMonth: String) {
        val tvMonth = activity.findViewById<TextView>(R.id.tvMonth)
        val btnPrev = activity.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNext = activity.findViewById<ImageView>(R.id.btnNextMonth)

        val btnTabSummary = activity.findViewById<TextView>(R.id.btnTabSummary)
        val btnTabCalendar = activity.findViewById<TextView>(R.id.btnTabCalendar)
        val btnTabList = activity.findViewById<TextView>(R.id.btnTabList)

        val ivSticker = activity.findViewById<ImageView>(R.id.ivMonthSticker)
        val scroll = activity.findViewById<ScrollView>(R.id.scrollViewContent)
        val tvEmpty = activity.findViewById<TextView>(R.id.tv_empty_message)

        val tvTitle = activity.findViewById<TextView>(R.id.tv_summary_title)
        val tvMost = activity.findViewById<TextView>(R.id.tv_most_emotion)
        val ivMost = activity.findViewById<ImageView>(R.id.iv_most_emotion)
        val tvFlow = activity.findViewById<TextView>(R.id.iv_most_flow)
        val tvOneLine = activity.findViewById<TextView>(R.id.tv_one_line_summary)
        val tvDetail = activity.findViewById<TextView>(R.id.tv_detail_summary)
        val tvKw1 = activity.findViewById<TextView>(R.id.tv_keyword_1)
        val tvKw2 = activity.findViewById<TextView>(R.id.tv_keyword_2)
        val tvKw3 = activity.findViewById<TextView>(R.id.tv_keyword_3)

        val (yy, mm) = parseYm(yearMonth)
        tvMonth.text = "${mm.toString().padStart(2, '0')}월 $yy"
        ivSticker.setImageResource(stickerResOf(mm))
        tvTitle.text = "${mm}월 월간 요약"

        // 탭
        btnTabSummary.setOnClickListener { /* 현재 */ }
        btnTabCalendar.setOnClickListener {
            activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
            activity.finish()
        }
        btnTabList.setOnClickListener {
            activity.startActivity(Intent(activity, DiaryListActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
            activity.finish()
        }

        // 월 이동
        btnPrev.setOnClickListener {
            val prev = shiftYm(yearMonth, -1)
            activity.startActivity(Intent(activity, MonthlySummaryActivity::class.java).apply {
                putExtra("yearMonth", prev)
            })
            activity.finish()
        }
        btnNext.setOnClickListener {
            val next = shiftYm(yearMonth, +1)
            activity.startActivity(Intent(activity, MonthlySummaryActivity::class.java).apply {
                putExtra("yearMonth", next)
            })
            activity.finish()
        }

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val ownerId = appService.getUserProfile().ownerId

            // 1) 캐시 조회
            val cached = MonthlyDao(helper.readableDatabase).get(ownerId, yearMonth)

            // 2) 만약 "지난달"이면 없을 때 생성(네 AppService가 제공하는 함수 활용)
            val summary = if (cached != null) {
                cached
            } else {
                if (yearMonth == DateUtil.previousMonthYm()) {
                    appService.ensureLastMonthMonthlySummary(ownerId)
                } else null
            }

            withContext(Dispatchers.Main) {
                if (summary == null) {
                    scroll.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "${mm}월 요약이 아직 작성되지 않았어요.\n일기를 더 써보세요!"
                    return@withContext
                }

                scroll.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE

                tvOneLine.text = summary.oneLineSummary
                tvDetail.text = summary.detailSummary

                tvMost.text = moodKo(summary.dominantMood)
                ivMost.setImageResource(iconResOf(summary.dominantMood))
                tvFlow.text = "'${summary.emotionFlow}'"

                val kws = summary.keywords.take(3)
                val chips = listOf(tvKw1, tvKw2, tvKw3)

                for (i in chips.indices) {
                    val tv = chips[i]
                    tv.text = kws.getOrNull(i).orEmpty()
                    tv.visibility = if (tv.text.isNullOrBlank()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun parseYm(ym: String): Pair<Int, Int> {
        val y = ym.take(4).toIntOrNull() ?: 1970
        val m = ym.drop(5).take(2).toIntOrNull() ?: 1
        return y to m
    }

    private fun shiftYm(ym: String, deltaMonth: Int): String {
        val (y, m) = parseYm(ym)
        val c = Calendar.getInstance()
        c.set(y, m - 1, 1)
        c.add(Calendar.MONTH, deltaMonth)
        val ny = c.get(Calendar.YEAR)
        val nm = c.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(ny, nm)
    }

    private fun stickerResOf(month: Int): Int = when (month) {
        1 -> R.drawable.month_1
        2 -> R.drawable.month_2
        3 -> R.drawable.month_3
        4 -> R.drawable.month_4
        5 -> R.drawable.month_5
        6 -> R.drawable.month_6
        7 -> R.drawable.month_7
        8 -> R.drawable.month_8
        9 -> R.drawable.month_9
        10 -> R.drawable.month_10
        11 -> R.drawable.month_11
        12 -> R.drawable.month_12
        else -> R.drawable.month_1
    }

    private fun moodKo(m: Mood): String = when (m) {
        Mood.JOY -> "기쁨"
        Mood.CONFIDENCE -> "자신감"
        Mood.CALM -> "평온"
        Mood.NORMAL -> "평범"
        Mood.DEPRESSED -> "우울"
        Mood.ANGRY -> "분노"
        Mood.TIRED -> "피곤함"
    }

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
