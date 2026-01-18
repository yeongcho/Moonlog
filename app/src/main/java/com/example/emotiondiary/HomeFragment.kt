package com.example.emotiondiary

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    // UI 요소 변수 선언
    private lateinit var layoutEmpty: ConstraintLayout
    private lateinit var layoutFilled: ConstraintLayout
    private lateinit var tvDate: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvContent: TextView
    private lateinit var tvTag: TextView
    private lateinit var ivEmotion: android.widget.ImageView
    private lateinit var btnWrite: AppCompatButton
    private lateinit var btnDelete: TextView
    private lateinit var btnEdit: TextView
    private lateinit var tvAnalysis: TextView
    private lateinit var btnMission: AppCompatButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // XML 화면 연결
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // ID 찾기
        layoutEmpty = view.findViewById(R.id.layout_empty_state)
        layoutFilled = view.findViewById(R.id.layout_filled_state)
        tvDate = view.findViewById(R.id.tv_home_date)

        tvTitle = view.findViewById(R.id.tv_diary_title)
        tvContent = view.findViewById(R.id.tv_diary_content)
        tvTag = view.findViewById(R.id.tv_diary_tag)
        ivEmotion = view.findViewById(R.id.iv_diary_emotion)

        btnWrite = view.findViewById(R.id.btn_go_write)
        btnDelete = view.findViewById(R.id.tv_btn_delete)
        btnEdit = view.findViewById(R.id.tv_btn_edit)

        tvAnalysis = view.findViewById(R.id.tv_analysis_result)
        btnMission = view.findViewById(R.id.btn_mission)

        // 1. "이야기를 들려주세요" 버튼 누르면 작성 화면으로 이동
        btnWrite.setOnClickListener {
            val intent = Intent(activity, WriteActivity::class.java)
            startActivity(intent)
        }

        // 2. 삭제 버튼 클릭 시
        btnDelete.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("일기 삭제")
            builder.setMessage("정말 삭제 하시겠습니까?")

            builder.setPositiveButton("예") { _, _ ->
                // 데이터 초기화
                DiaryData.isWritten = false
                DiaryData.title = ""
                DiaryData.content = ""

                // 스낵바로 알림 (아이콘 없는 깔끔한 알림)
                Snackbar.make(view, "삭제되었습니다.", Snackbar.LENGTH_SHORT).show()

                updateUI() // 화면 새로고침
            }
            builder.setNegativeButton("아니오", null)
            builder.show()
        }

        // 3. 수정 버튼 클릭 시 (작성 화면으로 이동하되, 내용은 유지됨)
        btnEdit.setOnClickListener {
            val intent = Intent(activity, WriteActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    // 화면이 다시 보일 때마다 UI 업데이트 (작성하고 돌아왔을 때 등)
    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        if (DiaryData.isWritten) {
            // 작성된 상태: 빈 화면 숨기고, 채워진 카드 보여주기
            layoutEmpty.visibility = View.GONE
            layoutFilled.visibility = View.VISIBLE

            // 데이터 채우기
            tvTitle.text = DiaryData.title
            tvContent.text = DiaryData.content
            tvTag.text = "#${DiaryData.emotionText}"
            ivEmotion.setImageResource(DiaryData.emotionIcon)

            // 분석 결과 멘트 바꾸기 (예시)
            tvAnalysis.text = "${DiaryData.emotionText}을(를) 느낀 하루였군요.\n내일은 더 좋은 일이 생길 거예요!"
            btnMission.visibility = View.VISIBLE
            btnMission.text = "오늘의 미션: 따뜻한 차 한 잔 마시기"

        } else {
            // 미작성 상태: 채워진 카드 숨기고, 빈 화면 보여주기
            layoutEmpty.visibility = View.VISIBLE
            layoutFilled.visibility = View.GONE

            tvAnalysis.text = "아직 일기를 작성하지 않았어요.\n이야기를 작성하고 마음 답장을 확인해요."
            btnMission.visibility = View.GONE
        }
    }
}