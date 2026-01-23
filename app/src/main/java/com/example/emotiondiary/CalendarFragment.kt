package com.example.emotiondiary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.emotiondiary.databinding.FragmentCalendarBinding
import java.util.Calendar

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    // ★ 현재 보고 있는 달력을 추적하는 변수 (중요!)
    private val currentCalendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 처음 화면 켜질 때 현재 달 이미지 & 달력 맞추기
        updateCalendarAndImage()

        // 2. [왼쪽 화살표] 클릭 -> 1달 빼기
        binding.btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendarAndImage()
        }

        // 3. [오른쪽 화살표] 클릭 -> 1달 더하기
        binding.btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendarAndImage()
        }

        // 4. 달력 날짜를 직접 클릭했을 때도 날짜 동기화
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            currentCalendar.set(year, month, dayOfMonth)
            // 날짜를 누르면 이미지만 업데이트 (달력은 이미 이동했으므로)
            updateImageOnly()
        }

        // 5. ★ [핵심] '요약' 탭 누르면 -> 현재 보고 있는 '월' 정보를 가지고 이동!
        binding.btnTabSummary.setOnClickListener {
            val selectedMonth = currentCalendar.get(Calendar.MONTH) + 1 // 1월=0이라서 +1

            val fragment = MonthlySummaryFragment()
            val bundle = Bundle()
            bundle.putInt("selected_month", selectedMonth) // "몇 월"인지 정보를 가방(bundle)에 담음
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, fragment)
                .commit()
        }

        // 6. 리스트 탭
        binding.layoutTopTabs.getChildAt(2).setOnClickListener {
            Toast.makeText(context, "리스트 화면 준비 중!", Toast.LENGTH_SHORT).show()
        }
    }

    // 화면(달력+이미지)을 갱신하는 함수
    private fun updateCalendarAndImage() {
        // 1. 달력 위치 이동
        binding.calendarView.date = currentCalendar.timeInMillis

        // 2. 상단 월 이미지 변경
        updateImageOnly()
    }

    // 이미지만 바꾸는 함수
    private fun updateImageOnly() {
        val month = currentCalendar.get(Calendar.MONTH) + 1
        val resourceName = "img_month_$month"
        val resourceId = resources.getIdentifier(resourceName, "drawable", requireContext().packageName)

        if (resourceId != 0) {
            binding.ivMonthTitle.setImageResource(resourceId)
        } else {
            binding.ivMonthTitle.setImageResource(R.drawable.img_month_1) // 없으면 1월 기본
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}