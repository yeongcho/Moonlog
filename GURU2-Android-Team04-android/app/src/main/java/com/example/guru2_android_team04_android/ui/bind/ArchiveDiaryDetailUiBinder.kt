package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.DiaryEntryReader
import com.example.guru2_android_team04_android.data.model.Mood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// ArchiveDiaryDetailUiBinder : activity_archive_diary_detail.xml ↔ AppService 연동 전담 클래스
class ArchiveDiaryDetailUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    private val reader = DiaryEntryReader(activity)

    fun bind(entryId: Long) {
        if (entryId <= 0L) {
            Toast.makeText(activity, "entryId가 올바르지 않아요.", Toast.LENGTH_SHORT).show()
            activity.finish()
            return
        }

        val tvDate = activity.findViewById<TextView>(R.id.tvDiaryDayText)
        val ivMood = activity.findViewById<ImageView>(R.id.ivDetailEmotion)
        val tvMood = activity.findViewById<TextView>(R.id.tvDetailEmotionTag)
        val tvTitle = activity.findViewById<TextView>(R.id.tvDiaryTitle)
        val tvContent = activity.findViewById<TextView>(R.id.tvDiaryContent)
        val ivFav = activity.findViewById<ImageView>(R.id.ivDetailFavorites)

        // 마음 카드
        val tvComfort = activity.findViewById<TextView>(R.id.tvNicknameMsg)
        val tvMission = activity.findViewById<TextView>(R.id.tvMissionText)

        // 분석/실천안
        val tvAnalysis = activity.findViewById<TextView>(R.id.tv_analysis_content)
        val tvTags = activity.findViewById<TextView>(R.id.tv_analysis_tags)

        val tvA1 = activity.findViewById<TextView>(R.id.tv_action_1)
        val tvA2 = activity.findViewById<TextView>(R.id.tv_action_2)
        val tvA3 = activity.findViewById<TextView>(R.id.tv_action_3)
        val tvFooter = activity.findViewById<TextView>(R.id.tv_action_footer)

        val btnSaveCard =
            activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save_card)

        // 날짜 이동(이 화면에서 prev/next는 “이전/다음 일기”로 해석)
        val btnPrev = activity.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNext = activity.findViewById<ImageView>(R.id.btnNextMonth)
        val tvHeader = activity.findViewById<TextView>(R.id.tvMonth)

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val entry = reader.getByIdOrNull(entryId)
            if (entry == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "일기를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
                return@launch
            }

            val preview = when (val r = appService.getMindCardPreviewByEntryIdSafe(entryId)) {
                is AppResult.Success -> r.data
                is AppResult.Failure -> null
            }

            val detail = when (val r = appService.getMindCardDetailByEntryIdSafe(entryId)) {
                is AppResult.Success -> r.data
                is AppResult.Failure -> null
            }

            // 같은 달 entry 목록(이전/다음 이동용)
            val ym = entry.dateYmd.take(7)
            val ownerId = entry.ownerId
            val monthEntries = appService.getEntriesByMonth(ownerId, ym).sortedBy { it.dateYmd }
            val idx = monthEntries.indexOfFirst { it.entryId == entryId }

            withContext(Dispatchers.Main) {
                // 헤더 날짜
                tvHeader.text = prettyDateHeader(entry.dateYmd)
                tvDate.text = prettyDateHeader(entry.dateYmd)

                tvTitle.text = entry.title
                tvContent.text = entry.content

                ivMood.setImageResource(iconResOf(entry.mood))
                tvMood.text = "태그: ${moodKo(entry.mood)}"

                // 마음 카드 프리뷰
                if (preview == null) {
                    tvComfort.text = "오늘도 기록해줘서 고마워요. 지금은 충분히 잘하고 있어요."
                    tvMission.text = "오늘의 미션: 천천히 숨 고르기"
                } else {
                    tvComfort.text = preview.comfortPreview
                    tvMission.text = "오늘의 미션: ${preview.mission}"
                }

                // 분석/해시태그/실천안
                if (detail == null) {
                    tvAnalysis.text = "분석을 불러오지 못했어요."
                    tvTags.text = ""
                    tvA1.text = "1. 천천히 숨 고르기"
                    tvA2.text = "2. 물 한 잔 마시기"
                    tvA3.text = "3. 가볍게 스트레칭"
                    tvFooter.text = "오늘은 여기까지도 충분해요."
                } else {
                    tvAnalysis.text = detail.fullText
                    tvTags.text = detail.hashtags.joinToString(" ") { "#$it" }
                    tvA1.text = "1. ${detail.missions.getOrNull(0).orEmpty()}"
                    tvA2.text = "2. ${detail.missions.getOrNull(1).orEmpty()}"
                    tvA3.text = "3. ${detail.missions.getOrNull(2).orEmpty()}"
                    tvFooter.text = detail.missionSummary
                }

                // 즐겨찾기 토글
                var fav = entry.isFavorite
                ivFav.setImageResource(if (fav) R.drawable.ic_favorites_o else R.drawable.ic_favorites_x)
                ivFav.setOnClickListener {
                    val next = !fav
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val ok = appService.setEntryFavorite(entry.ownerId, entry.entryId, next)
                        withContext(Dispatchers.Main) {
                            if (ok) {
                                fav = next
                                ivFav.setImageResource(if (fav) R.drawable.ic_favorites_o else R.drawable.ic_favorites_x)
                            } else {
                                Toast.makeText(activity, "즐겨찾기 변경에 실패했어요.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // 이미지 카드로 소장하기
                btnSaveCard.setOnClickListener {
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val r = appService.exportMindCardToGallerySafe(activity, entryId)
                        withContext(Dispatchers.Main) {
                            when (r) {
                                is AppResult.Success ->
                                    Toast.makeText(activity, "갤러리에 저장했어요!", Toast.LENGTH_SHORT).show()
                                is AppResult.Failure ->
                                    Toast.makeText(activity, r.error.userMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // 이전/다음 일기 이동
                btnPrev.setOnClickListener {
                    val prev = monthEntries.getOrNull(idx - 1) ?: return@setOnClickListener
                    activity.startActivity(Intent(activity, ArchiveDiaryDetailActivity::class.java).apply {
                        putExtra("entryId", prev.entryId)
                    })
                    activity.finish()
                }
                btnNext.setOnClickListener {
                    val next = monthEntries.getOrNull(idx + 1) ?: return@setOnClickListener
                    activity.startActivity(Intent(activity, ArchiveDiaryDetailActivity::class.java).apply {
                        putExtra("entryId", next.entryId)
                    })
                    activity.finish()
                }
            }
        }
    }

    private fun prettyDateHeader(ymd: String): String {
        val y = ymd.take(4).toIntOrNull() ?: return ymd
        val m = ymd.drop(5).take(2).toIntOrNull() ?: return ymd
        val d = ymd.takeLast(2).toIntOrNull() ?: return ymd
        val c = Calendar.getInstance()
        c.set(y, m - 1, d)
        val wd = when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "일요일"
            Calendar.MONDAY -> "월요일"
            Calendar.TUESDAY -> "화요일"
            Calendar.WEDNESDAY -> "수요일"
            Calendar.THURSDAY -> "목요일"
            Calendar.FRIDAY -> "금요일"
            Calendar.SATURDAY -> "토요일"
            else -> ""
        }
        return "${y}년 ${m}월 ${d}일 $wd"
    }

    private fun moodKo(m: Mood): String = when (m) {
        Mood.JOY -> "기쁨"
        Mood.CONFIDENCE -> "자신감"
        Mood.CALM -> "평온"
        Mood.NORMAL -> "평범"
        Mood.DEPRESSED -> "우울"
        Mood.ANGRY -> "분노"
        Mood.TIRED -> "피곤함"
    }

    private fun iconResOf(mood: Mood): Int = when (mood) {
        Mood.JOY -> R.drawable.emotion_joy
        Mood.CONFIDENCE -> R.drawable.emotion_confidence
        Mood.CALM -> R.drawable.emotion_calm
        Mood.NORMAL -> R.drawable.emotion_normal
        Mood.DEPRESSED -> R.drawable.emotion_sad
        Mood.ANGRY -> R.drawable.emotion_angry
        Mood.TIRED -> R.drawable.emotion_tired
    }
}
