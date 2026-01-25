// HomeFragment.kt  (⚠️ package가 com.example.guru2_android_team04_android 인 파일)
package com.example.guru2_android_team04_android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil
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

    private var currentEntryId: Long? = null
    private var currentTitle: String? = null
    private var currentContent: String? = null
    private var currentMoodLabel: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // XML 화면 연결
        val view = inflater.inflate(R.layout.activity_home, container, false)

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
            val entryId = currentEntryId
            if (entryId == null) {
                Snackbar.make(view, "삭제할 일기가 없어요.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("일기 삭제")
            builder.setMessage("정말 삭제 하시겠습니까?")

            builder.setPositiveButton("예") { _, _ ->
                val appService = (requireActivity().application as MyApp).appService
                appService.deleteEntry(entryId)
                Snackbar.make(view, "삭제되었습니다.", Snackbar.LENGTH_SHORT).show()
                updateUI()
            }
            builder.setNegativeButton("아니오", null)
            builder.show()
        }

        // 3. 수정 버튼 클릭 시 (작성 화면으로 이동하되, 내용은 extras로 넘겨 채워넣기)
        btnEdit.setOnClickListener {
            val entryId = currentEntryId
            if (entryId == null) {
                Snackbar.make(view, "수정할 일기가 없어요.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(activity, WriteActivity::class.java).apply {
                putExtra("entry_id", entryId)
                putExtra("prefill_title", currentTitle.orEmpty())
                putExtra("prefill_content", currentContent.orEmpty())
                putExtra("prefill_mood_label", currentMoodLabel.orEmpty())
            }
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
        val appService = (requireActivity().application as MyApp).appService
        val ownerId = appService.currentOwnerIdOrNull() ?: appService.startAnonymousSession()

        val todayYmd = DateUtil.todayYmd()
        tvDate.text = todayYmd

        val yearMonth = todayYmd.take(7) // "YYYY-MM" 가정 (DateUtil 구현에 맞춰 사용)
        val entries = appService.getEntriesByMonth(ownerId, yearMonth)

        // 홈은 "오늘 일기" 우선, 없으면 가장 최근(updatedAt)으로
        val entry = entries.firstOrNull { it.dateYmd == todayYmd }
            ?: entries.maxByOrNull { it.updatedAt }

        if (entry == null) {
            // 미작성 상태
            currentEntryId = null
            currentTitle = null
            currentContent = null
            currentMoodLabel = null

            layoutEmpty.visibility = View.VISIBLE
            layoutFilled.visibility = View.GONE

            tvAnalysis.text = "아직 일기를 작성하지 않았어요.\n이야기를 작성하고 마음 답장을 확인해요."
            btnMission.visibility = View.GONE
            return
        }

        // 작성된 상태
        currentEntryId = entry.entryId
        currentTitle = entry.title
        currentContent = entry.content
        currentMoodLabel = entry.mood.name

        layoutEmpty.visibility = View.GONE
        layoutFilled.visibility = View.VISIBLE

        tvTitle.text = entry.title
        tvContent.text = entry.content
        tvTag.text = "#${entry.mood.name}"
        ivEmotion.setImageResource(moodToDrawable(entry.mood))

        // ✅ 분석/미션은 AppService 로직 그대로 사용 (실패해도 기본 문구/미션으로 안전 처리됨)
        when (val r = appService.getMindCardPreviewByEntryIdSafe(entry.entryId)) {
            is AppResult.Success -> {
                tvAnalysis.text = r.data.comfortPreview
                btnMission.visibility = View.VISIBLE
                btnMission.text = "오늘의 미션: ${r.data.mission}"
            }

            is AppResult.Failure -> {
                tvAnalysis.text = "오늘도 기록해줘서 고마워요. 지금은 충분히 잘하고 있어요."
                btnMission.visibility = View.VISIBLE
                btnMission.text = "오늘의 미션: 천천히 숨 고르기"
            }
        }
    }

    private fun moodToDrawable(mood: Mood): Int {
        // Mood enum 이름이 한글/영문 어떤 형태든 최대한 안전하게 매핑
        return when (mood.name) {
            "기쁨", "JOY" -> R.drawable.emotion_joy
            "자신감", "CONFIDENCE" -> R.drawable.emotion_confidence
            "평온", "CALM" -> R.drawable.emotion_calm
            "평범", "NORMAL" -> R.drawable.emotion_normal
            "우울", "슬픔", "DEPRESSION", "SAD" -> R.drawable.emotion_sad
            "분노", "ANGER" -> R.drawable.emotion_angry
            "피곤함", "FATIGUE", "TIRED" -> R.drawable.emotion_tired
            else -> R.drawable.emotion_normal
        }
    }
}
