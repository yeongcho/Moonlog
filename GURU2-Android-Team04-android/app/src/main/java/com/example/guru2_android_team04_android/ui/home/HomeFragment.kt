package com.example.guru2_android_team04_android.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.guru2_android_team04_android.DiaryData
import com.example.guru2_android_team04_android.R
import com.example.guru2_android_team04_android.WriteActivity

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.activity_home, container, false)

        // "이야기를 들려주세요" 버튼 (새 글 쓰기)
        val btnGoWrite = root.findViewById<Button>(R.id.btn_go_write)
        btnGoWrite.setOnClickListener {
            // 새 글을 쓸 때는 데이터를 초기화하고 이동
            DiaryData.isWritten = false
            val intent = Intent(activity, WriteActivity::class.java)
            startActivity(intent)
        }
        return root
    }

    // 화면이 다시 보일 때마다(글 쓰고 왔을 때) 실행됨
    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val view = view ?: return

        val layoutEmpty = view.findViewById<View>(R.id.layout_empty_state)
        val layoutFilled = view.findViewById<View>(R.id.layout_filled_state)
        val tvAnalysis = view.findViewById<TextView>(R.id.tv_analysis_result)
        val btnMission = view.findViewById<Button>(R.id.btn_mission)
        val btnDetail = view.findViewById<Button>(R.id.btn_go_detail)

        // 수정, 삭제 버튼 가져오기
        val btnEdit = view.findViewById<TextView>(R.id.tv_btn_edit) // XML에 이 아이디가 있어야 함
        val btnDelete = view.findViewById<TextView>(R.id.tv_btn_delete)

        if (DiaryData.isWritten) {
            // 1. 일기 보여주기
            layoutEmpty.visibility = View.GONE
            layoutFilled.visibility = View.VISIBLE
            btnMission.visibility = View.VISIBLE
            btnDetail.visibility = View.VISIBLE

            view.findViewById<TextView>(R.id.tv_diary_tag).text = "#: ${DiaryData.emotionText}"
            view.findViewById<TextView>(R.id.tv_diary_title).text = DiaryData.title
            view.findViewById<TextView>(R.id.tv_diary_content).text = DiaryData.content
            view.findViewById<ImageView>(R.id.iv_diary_emotion).setImageResource(DiaryData.emotionIcon)

            val (message, mission) = analyzeEmotion(DiaryData.emotionText)
            tvAnalysis.text = "닉네임님, $message"
            btnMission.text = "오늘의 미션: $mission"

            // ★ [기능 1] 수정 버튼 클릭 시
            btnEdit.setOnClickListener {
                // 데이터를 유지한 채로(isWritten = true) 작성 화면으로 이동
                // WriteActivity에서 이 값을 보고 내용을 채워넣음
                val intent = Intent(activity, WriteActivity::class.java)
                startActivity(intent)
            }

            // ★ [기능 2] 삭제 버튼 클릭 시
            btnDelete.setOnClickListener {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("일기 삭제")
                builder.setMessage("삭제 하시겠습니까?")

                // "예" 버튼
                builder.setPositiveButton("예") { dialog, _ ->
                    // 데이터 지우기
                    DiaryData.isWritten = false
                    DiaryData.title = ""
                    DiaryData.content = ""

                    // ★  Toast 대신 Snackbar 사용
                    // view는 updateUI 함수 맨 윗줄에 있는 변수입니다.
                    com.google.android.material.snackbar.Snackbar.make(view, "삭제되었습니다.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()

                    updateUI() // 화면 새로고침
                }

                builder.setNegativeButton("아니오") { dialog, _ ->
                    dialog.dismiss()
                }

                builder.show()
            }

        } else {
            // 안 썼을 때 (고양이 화면)
            layoutEmpty.visibility = View.VISIBLE
            layoutFilled.visibility = View.GONE
            btnMission.visibility = View.GONE
            btnDetail.visibility = View.GONE
            tvAnalysis.text = "아직 일기를 작성하지 않았어요.\n이야기를 작성하고 마음 답장을 확인해요."
        }
    }

    private fun analyzeEmotion(emotion: String): Pair<String, String> {
        return when (emotion) {
            "기쁨" -> Pair("오늘 정말 행복한 하루였네요! 이 기분을 오래 간직해요.", "오늘의 행복을 사진으로 남겨두기 📸")
            "자신감" -> Pair("멋진 하루였어요! 당신의 능력을 믿으세요.", "거울 보고 '난 멋져!' 3번 외치기 ✨")
            "평온" -> Pair("잔잔한 호수 같은 하루였군요. 편안한 밤 보내세요.", "따뜻한 차 한 잔 마시기 🍵")
            "우울", "슬픔" -> Pair("오늘은 여기서 멈춰도 괜찮아요.\n충분히 애썼어요. 무거운 마음은 여기에 두고 가요.", "걱정 스위치 끄고 푹 잠들기 🌙")
            "분노" -> Pair("화나는 일이 있었군요. 심호흡 한번 크게 해볼까요?", "좋아하는 음악 들으며 멍 때리기 🎧")
            "피곤함" -> Pair("정말 고생 많았어요. 오늘은 무조건 휴식이 필요해요.", "스마트폰 끄고 10분 일찍 눕기 🛌")
            else -> Pair("오늘 하루도 수고 많았어요.", "나 자신에게 칭찬 한마디 해주기 👏")
        }
    }
}