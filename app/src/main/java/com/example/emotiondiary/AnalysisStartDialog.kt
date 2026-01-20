package com.example.emotiondiary

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.example.emotiondiary.databinding.DialogAnalysisStartBinding

class AnalysisStartDialog : DialogFragment() {

    private var _binding: DialogAnalysisStartBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAnalysisStartBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartAnalysis.setOnClickListener {
            // 1. 로딩 화면을 먼저 띄웁니다. (이때 초록 화면은 아직 켜진 상태 유지)
            val loadingDialog = LoadingDialog()
            loadingDialog.show(parentFragmentManager, "LoadingDialog")

            // 2. 2초 뒤에 실행
            Handler(Looper.getMainLooper()).postDelayed({

                // (1) 로딩 끄기
                if (loadingDialog.isAdded) {
                    loadingDialog.dismiss()
                }

                // (2) 밤하늘 결과 화면 띄우기
                // ★ 중요: 부모 FragmentManager가 살아있는지 확인하고 띄움
                if (parentFragmentManager != null && !isStateSaved) {
                    val resultDialog = AiAnalysisDialog()
                    resultDialog.show(parentFragmentManager, "AiAnalysisDialog")
                }

                // (3) 이제 임무를 다했으니 초록색 시작 화면도 닫기
                dismiss()

            }, 2000)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}