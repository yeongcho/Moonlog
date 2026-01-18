package com.example.emotiondiary.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.emotiondiary.R

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val tvSelectedDate = view.findViewById<TextView>(R.id.tv_selected_date)
        val tvDiaryPreview = view.findViewById<TextView>(R.id.tv_diary_preview)

        // 달력 날짜를 클릭했을 때 할 일
        // 달력 날짜를 클릭했을 때 할 일
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->

            // 1. 클릭한 날짜로 꼬리표를 만듭니다. (예: 2026-2-14)
            // 주의: month는 0부터 시작하므로 +1 해줘야 합니다.
            val clickedDateKey = "$year-${month + 1}-$dayOfMonth"

            // 화면에 보여줄 예쁜 날짜 글자 (예: 2026년 2월 14일)
            val prettyDate = "${year}년 ${month + 1}월 ${dayOfMonth}일"
            tvSelectedDate.text = prettyDate

            // 2. 수첩을 꺼냅니다.
            val sharedPref = requireActivity().getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)

            // 3. 클릭한 날짜 꼬리표로 제목과 내용을 찾습니다.
            val savedTitle = sharedPref.getString("${clickedDateKey}_title", "")
            val savedContent = sharedPref.getString("${clickedDateKey}_content", "")

            // 4. 내용이 있는지 확인! (제목이 비어있지 않으면 쓴 걸로 간주)
            if (!savedTitle.isNullOrEmpty()) {
                // 내용이 있다면 보여줌
                tvDiaryPreview.text = "[제목] $savedTitle\n\n$savedContent"
                tvDiaryPreview.setTextColor(resources.getColor(R.color.black, null))
            } else {
                // 내용이 없다면
                tvDiaryPreview.text = "작성된 일기가 없습니다."
                tvDiaryPreview.setTextColor(resources.getColor(R.color.text_gray, null))
            }
        }

        return view
    }
}