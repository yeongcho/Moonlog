package com.example.emotiondiary

class MonthlySummaryFragment : androidx.fragment.app.Fragment() {

    private var _binding: com.example.emotiondiary.databinding.FragmentMonthlySummaryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): android.view.View {
        _binding = _root_ide_package_.com.example.emotiondiary.databinding.FragmentMonthlySummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 캘린더에서 보내준 "몇 월?" 정보 받기 (없으면 기본값 1월)
        val selectedMonth = arguments?.getInt("selected_month") ?: 1

        // 2. 받은 월에 따라서 화면 내용 바꾸기
        checkAndShowSummary(selectedMonth)

        // 3. 탭 기능 설정 (다시 캘린더로 돌아가기)
        setupTabs()
    }

    private fun checkAndShowSummary(month: Int) {
        // [테스트 로직] 1월만 데이터가 있다고 가정
        if (month == 1) {
            // == 1월일 때 ==
            binding.scrollViewContent.visibility = View.VISIBLE // 내용 보이기
            binding.tvEmptyMessage.visibility = View.GONE       // 안내문 숨기기

            binding.tvSummaryTitle.text = "1월 월간 요약"
            setMonthImage(1)
            setMostFrequentEmotion() // 평온 이모티콘 설정

        } else {
            // == 1월이 아닐 때 (2월, 3월...) ==
            binding.scrollViewContent.visibility = View.GONE    // 내용 숨기기
            binding.tvEmptyMessage.visibility = View.VISIBLE    // 안내문 보이기
            binding.tvEmptyMessage.text = "${month}월 요약이 아직 작성되지 않았어요.\n일기를 더 써보세요!"
        }
    }

    private fun setMostFrequentEmotion() {
        binding.tvMostEmotion.text = "평온"
        binding.ivMostEmotion.setImageResource(R.drawable.emotion_calm)
    }

    private fun setMonthImage(month: Int) {
        val resourceName = "img_month_$month"
        val resourceId = resources.getIdentifier(resourceName, "drawable", requireContext().packageName)
        if (resourceId != 0) {
            binding.ivMonthTitle.setImageResource(resourceId)
        } else {
            binding.ivMonthTitle.setImageResource(R.drawable.img_month_1)
        }
    }

    private fun setupTabs() {
        // '캘린더' 탭 누르면 돌아가기
        binding.btnTabCalendar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, CalendarFragment())
                .commit()
        }

        binding.layoutTopTabs.getChildAt(2).setOnClickListener {
            Toast.makeText(context, "리스트 화면 준비 중!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}