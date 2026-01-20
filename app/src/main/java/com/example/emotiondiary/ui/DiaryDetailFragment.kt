package com.example.emotiondiary

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.emotiondiary.databinding.FragmentDiaryDetailBinding

class DiaryDetailFragment : Fragment() {

    private var _binding: FragmentDiaryDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // [상세 분석 보러가기] 버튼 클릭 이벤트
        binding.btnGoAnalysis.setOnClickListener {
            // 1. 로딩 창 띄우기
            val loadingDialog = LoadingDialog()
            loadingDialog.show(parentFragmentManager, "LoadingDialog")

            // 2. 2초(2000ms) 뒤에 로딩 끄고 분석 결과 띄우기
            Handler(Looper.getMainLooper()).postDelayed({
                loadingDialog.dismiss() // 로딩 닫기

                // 아까 만든 밤하늘 분석 팝업 띄우기
                val analysisDialog = AiAnalysisDialog()
                analysisDialog.show(parentFragmentManager, "AiAnalysisDialog")
            }, 2000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}