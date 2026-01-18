package com.example.emotiondiary // ★ 본인 패키지명으로 꼭 수정하세요!

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.emotiondiary.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    // 선택된 배지 번호 (기본: 4번 감정 로그 수집가)
    private var selectedIndex = 4

    // 배지 이름들
    private val badgeNames = listOf(
        "시작과 꾸준함", "작심삼일 마스터", "한 달의 조각들",
        "돌아온 여행자", "감정 로그 수집가", "감정 소믈리에"
    )

    // 배지 이미지 리소스 ID들
    private val badgeDrawables = listOf(
        R.drawable.ic_badge_start,
        R.drawable.ic_badge_three_days,
        R.drawable.ic_badge_month,
        R.drawable.ic_badge_traveler,
        R.drawable.ic_badge_emotion_log,
        R.drawable.ic_badge_wine
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 체크 표시 이미지뷰 목록
        val checkMarks = listOf(
            binding.ivCheck1, binding.ivCheck2, binding.ivCheck3,
            binding.ivCheck4, binding.ivCheck5, binding.ivCheck6
        )

        // 2. 배지 레이아웃 목록 (클릭 영역)
        val layouts = listOf(
            binding.layoutBadge1, binding.layoutBadge2, binding.layoutBadge3,
            binding.layoutBadge4, binding.layoutBadge5, binding.layoutBadge6
        )

        // 3. 클릭 이벤트 설정
        layouts.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                // (1) 선택된 인덱스 변경
                selectedIndex = index

                // (2) 모든 체크 숨기고, 선택된 것만 보이기
                checkMarks.forEach { it.visibility = View.GONE }
                checkMarks[index].visibility = View.VISIBLE

                // (3) 상단 미리보기 카드 업데이트
                binding.tvCurrentBadgeName.text = badgeNames[index]
                binding.ivCurrentBadgeImg.setImageResource(badgeDrawables[index])
            }
        }

        // 4. 초기 상태 설정 (기본값 보여주기)
        checkMarks.forEach { it.visibility = View.GONE }
        checkMarks[selectedIndex].visibility = View.VISIBLE
        binding.tvCurrentBadgeName.text = badgeNames[selectedIndex]
        binding.ivCurrentBadgeImg.setImageResource(badgeDrawables[selectedIndex])

        // 5. 저장 버튼 기능
        binding.btnSaveBadge.setOnClickListener {
            // 실제 앱에서는 여기서 서버나 DB에 저장해야 합니다.
            Toast.makeText(context, "'${badgeNames[selectedIndex]}' 배지로 변경했습니다!", Toast.LENGTH_SHORT).show()

            // 마이페이지로 돌아가기
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}