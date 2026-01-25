package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// DiaryCalendarUiBinder : activity_diary_calendar.xml ↔ AppService 연동 전담 클래스
class DiaryCalendarUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {

    fun bind(yearMonth: String) {
        val tvMonth = activity.findViewById<TextView>(R.id.tvMonth)
        val btnPrev = activity.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNext = activity.findViewById<ImageView>(R.id.btnNextMonth)

        val btnTabSummary = activity.findViewById<TextView>(R.id.btnTabSummary)
        val btnTabCalendar = activity.findViewById<TextView>(R.id.btnTabCalendar)
        val btnTabList = activity.findViewById<TextView>(R.id.btnTabList)

        val ivSticker = activity.findViewById<ImageView>(R.id.ivMonthSticker)
        val grid = activity.findViewById<GridLayout>(R.id.calendarGrid)
        val pie = activity.findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.pieChart)

        val (yy, mm) = parseYm(yearMonth)
        tvMonth.text = "${mm.toString().padStart(2, '0')}월 $yy"
        ivSticker.setImageResource(stickerResOf(mm))

        // 탭 이동
        btnTabSummary.setOnClickListener {
            activity.startActivity(Intent(activity, MonthlySummaryActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
        }
        btnTabCalendar.setOnClickListener { /* 현재 화면 */ }
        btnTabList.setOnClickListener {
            activity.startActivity(Intent(activity, DiaryListActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
        }

        // 월 이동
        btnPrev.setOnClickListener {
            val prev = shiftYm(yearMonth, -1)
            activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java).apply {
                putExtra("yearMonth", prev)
            })
            activity.finish()
        }
        btnNext.setOnClickListener {
            val next = shiftYm(yearMonth, +1)
            activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java).apply {
                putExtra("yearMonth", next)
            })
            activity.finish()
        }

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val ownerId = appService.getUserProfile().ownerId
            val entries = appService.getEntriesByMonth(ownerId, yearMonth).sortedBy { it.dateYmd }
            val byYmd = entries.associateBy { it.dateYmd }
            val moodCount = entries.groupingBy { it.mood }.eachCount()

            withContext(Dispatchers.Main) {
                // 캘린더 그리드 바인딩 (GridLayout 35칸 가정)
                bindCalendarGrid(grid, yy, mm, byYmd)

                // 파이차트(월간 감정 분포)
                val pieEntries = buildList {
                    for ((m, c) in moodCount) add(PieEntry(c.toFloat(), moodKo(m)))
                }
                if (pieEntries.isEmpty()) {
                    pie.clear()
                    pie.setNoDataText("이번 달 데이터가 없어요.")
                } else {
                    val ds = PieDataSet(pieEntries, "")
                    ds.valueTextSize = 12f
                    pie.data = PieData(ds)
                    pie.description.isEnabled = false
                    pie.legend.isEnabled = false
                    pie.invalidate()
                }
            }
        }
    }

    private fun bindCalendarGrid(
        grid: GridLayout,
        year: Int,
        month: Int, // 1..12
        byYmd: Map<String, com.example.guru2_android_team04_android.data.model.DiaryEntry>
    ) {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val firstDow = cal.get(Calendar.DAY_OF_WEEK) // 1=일..7=토
        val offset = (firstDow - Calendar.SUNDAY) // 0..6 (일 시작)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 35칸(1..35) 각각의 날짜 계산: (i - offset) => month day
        for (i in 1..35) {
            val cell = grid.getChildAt(i - 1) ?: continue
            val tvId = activity.resources.getIdentifier("tvDay$i", "id", activity.packageName)
            val ivId = activity.resources.getIdentifier("ivEmotion$i", "id", activity.packageName)

            val tvDay = cell.findViewById<TextView>(tvId)
            val ivEmotion = cell.findViewById<ImageView>(ivId)

            val dayNum = i - offset
            if (dayNum in 1..daysInMonth) {
                tvDay.text = dayNum.toString()
                val ymd = "%04d-%02d-%02d".format(year, month, dayNum)

                val entry = byYmd[ymd]
                if (entry == null) {
                    ivEmotion.visibility = View.INVISIBLE
                    cell.setOnClickListener {
                        Toast.makeText(activity, "이 날짜의 일기가 없어요.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    ivEmotion.visibility = View.VISIBLE
                    ivEmotion.setImageResource(iconResOf(entry.mood))
                    cell.setOnClickListener {
                        activity.startActivity(Intent(activity, ArchiveDiaryDetailActivity::class.java).apply {
                            putExtra("entryId", entry.entryId)
                        })
                    }
                }
            } else {
                // 이번 달이 아닌 칸(이전/다음달 표시 영역) — UI상 숫자/아이콘 숨김
                tvDay.text = ""
                ivEmotion.visibility = View.INVISIBLE
                cell.setOnClickListener(null)
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
