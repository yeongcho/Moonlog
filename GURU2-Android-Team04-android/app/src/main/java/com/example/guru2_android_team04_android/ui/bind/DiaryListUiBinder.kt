package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.ui.adapter.DiaryEntryAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DiaryListUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    private var weekIndex = 0
    private lateinit var weeks: List<Pair<String, String>>

    // RecyclerView Adapter (확장 포인트)
    private val adapter = DiaryEntryAdapter { entry ->
        activity.startActivity(Intent(activity, ArchiveDiaryDetailActivity::class.java).apply {
            putExtra("entryId", entry.entryId)
        })
    }

    fun bind(yearMonth: String) {
        val tvMonth = activity.findViewById<TextView>(R.id.tvMonth)
        val btnPrevMonth = activity.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNextMonth = activity.findViewById<ImageView>(R.id.btnNextMonth)

        val btnTabSummary = activity.findViewById<TextView>(R.id.btnTabSummary)
        val btnTabCalendar = activity.findViewById<TextView>(R.id.btnTabCalendar)
        val btnTabList = activity.findViewById<TextView>(R.id.btnTabList)

        val ivSticker = activity.findViewById<ImageView>(R.id.ivMonthSticker)

        val btnPrevWeek = activity.findViewById<ImageView>(R.id.btnPrevWeek)
        val btnNextWeek = activity.findViewById<ImageView>(R.id.btnNextWeek)
        val tvWeek = activity.findViewById<TextView>(R.id.tvWeek)

        // RecyclerView 세팅
        val rv = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDiaryList)
        rv.layoutManager = LinearLayoutManager(activity)
        rv.adapter = adapter

        val (yy, mm) = parseYm(yearMonth)
        tvMonth.text = "${mm.toString().padStart(2, '0')}월 $yy"
        ivSticker.setImageResource(stickerResOf(mm))

        // 탭
        btnTabSummary.setOnClickListener {
            activity.startActivity(Intent(activity, MonthlySummaryActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
            activity.finish()
        }
        btnTabCalendar.setOnClickListener {
            activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
            activity.finish()
        }
        btnTabList.setOnClickListener { /* 현재 */ }

        // 월 이동
        btnPrevMonth.setOnClickListener {
            val prev = shiftYm(yearMonth, -1)
            activity.startActivity(Intent(activity, DiaryListActivity::class.java).apply {
                putExtra("yearMonth", prev)
            })
            activity.finish()
        }
        btnNextMonth.setOnClickListener {
            val next = shiftYm(yearMonth, +1)
            activity.startActivity(Intent(activity, DiaryListActivity::class.java).apply {
                putExtra("yearMonth", next)
            })
            activity.finish()
        }

        // 주차 계산
        weeks = computeWeeks(yearMonth)
        weekIndex = weekIndex.coerceIn(0, weeks.size - 1)

        fun renderWeek() {
            tvWeek.text = weekLabel(weekIndex)
            val (startYmd, endYmd) = weeks[weekIndex]
            bindWeekEntries(startYmd, endYmd)
        }

        btnPrevWeek.setOnClickListener {
            weekIndex = (weekIndex - 1).coerceAtLeast(0)
            renderWeek()
        }
        btnNextWeek.setOnClickListener {
            weekIndex = (weekIndex + 1).coerceAtMost(weeks.size - 1)
            renderWeek()
        }

        renderWeek()
    }

    private fun bindWeekEntries(startYmd: String, endYmd: String) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val ownerId = appService.getUserProfile().ownerId
            val list = appService.getEntriesByWeek(ownerId, startYmd, endYmd)
                .sortedBy { it.dateYmd }

            withContext(Dispatchers.Main) {
                adapter.submitList(list)
            }
        }
    }

    // --- 아래는 너 원래 코드 그대로 (주차/월 계산) ---
    private fun computeWeeks(yearMonth: String): List<Pair<String, String>> {
        val (y, m) = parseYm(yearMonth)
        val c = Calendar.getInstance()
        c.set(y, m - 1, 1, 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)

        val daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH)

        val ranges = mutableListOf<Pair<String, String>>()
        var start = 1
        while (start <= daysInMonth) {
            val end = (start + 6).coerceAtMost(daysInMonth)
            ranges.add(
                "%04d-%02d-%02d".format(y, m, start) to "%04d-%02d-%02d".format(y, m, end)
            )
            start += 7
        }
        return ranges
    }

    private fun weekLabel(i: Int): String = when (i) {
        0 -> "첫째 주"
        1 -> "둘째 주"
        2 -> "셋째 주"
        3 -> "넷째 주"
        else -> "${i + 1}째 주"
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
}
