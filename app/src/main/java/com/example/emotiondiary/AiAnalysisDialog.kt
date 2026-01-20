package com.example.emotiondiary

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager // ★ 추가된 import
import androidx.fragment.app.DialogFragment
import com.example.emotiondiary.databinding.DialogAiAnalysisBinding

class AiAnalysisDialog : DialogFragment() {

    private var _binding: DialogAiAnalysisBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAiAnalysisBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return

        // 1. 너비와 높이를 화면 전체로 설정
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // 2. 배경 투명 처리
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // ★ 3. [핵심] 상단 상태바 & 하단 내비게이션 바 영역까지 화면 확장 (검은색 바 제거)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}