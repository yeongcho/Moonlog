// WriteActivity.kt
package com.example.guru2_android_team04_android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil

class WriteActivity : AppCompatActivity() {

    private var selectedEmotionDrawable: Int = R.drawable.emotion_normal
    private var selectedEmotionText: String = "평온"

    private var editingEntryId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_editor)

        window.statusBarColor = getColor(R.color.main_bg_sage)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        val appService = (application as MyApp).appService
        val ownerId = appService.currentOwnerIdOrNull() ?: appService.startAnonymousSession()

        val layoutWriteMode = findViewById<View>(R.id.layout_write_mode)
        val tvTagGuide = findViewById<TextView>(R.id.tv_tag_guide)
        val ivEmotionPreview = findViewById<ImageView>(R.id.iv_emotion_preview)
        val scrollMood = findViewById<HorizontalScrollView>(R.id.scroll_mood)
        val rgMood = findViewById<RadioGroup>(R.id.rg_mood)
        val etTitle = findViewById<EditText>(R.id.et_title)
        val etContent = findViewById<EditText>(R.id.et_content)
        val btnComplete = findViewById<Button>(R.id.btn_complete)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvLoadingText = findViewById<TextView>(R.id.tv_loading_text)

        scrollMood.visibility = View.GONE

        // 아이콘 크기 강제 조절 코드 (40dp)
        val emotionMap = mapOf(
            R.id.rb_joy to R.drawable.emotion_joy,
            R.id.rb_confidence to R.drawable.emotion_confidence,
            R.id.rb_calm to R.drawable.emotion_calm,
            R.id.rb_normal to R.drawable.emotion_normal,
            R.id.rb_depression to R.drawable.emotion_sad,
            R.id.rb_anger to R.drawable.emotion_angry,
            R.id.rb_fatigue to R.drawable.emotion_tired
        )

        emotionMap.forEach { (viewId, drawableId) ->
            val radioButton = findViewById<RadioButton>(viewId)
            val sizePx = (40 * resources.displayMetrics.density).toInt()
            val bitmap = BitmapFactory.decodeResource(resources, drawableId)
            if (bitmap != null) {
                val scaledBitmap: Bitmap = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
                val scaledDrawable = BitmapDrawable(resources, scaledBitmap)
                radioButton.setCompoundDrawablesWithIntrinsicBounds(null, scaledDrawable, null, null)
            }
        }

        // ✅ (수정 모드) HomeFragment에서 넘긴 extras로 프리필
        editingEntryId = intent.getLongExtra("entry_id", 0L)
        val preTitle = intent.getStringExtra("prefill_title").orEmpty()
        val preContent = intent.getStringExtra("prefill_content").orEmpty()
        val preMood = intent.getStringExtra("prefill_mood_label").orEmpty()

        if (editingEntryId > 0L) {
            etTitle.setText(preTitle)
            etContent.setText(preContent)

            if (preMood.isNotBlank()) {
                // 프리필 감정 세팅
                when (preMood) {
                    "기쁨", "JOY" -> { selectedEmotionDrawable = R.drawable.emotion_joy; selectedEmotionText = "기쁨"; rgMood.check(R.id.rb_joy) }
                    "자신감", "CONFIDENCE" -> { selectedEmotionDrawable = R.drawable.emotion_confidence; selectedEmotionText = "자신감"; rgMood.check(R.id.rb_confidence) }
                    "평온", "CALM" -> { selectedEmotionDrawable = R.drawable.emotion_calm; selectedEmotionText = "평온"; rgMood.check(R.id.rb_calm) }
                    "평범", "NORMAL" -> { selectedEmotionDrawable = R.drawable.emotion_normal; selectedEmotionText = "평범"; rgMood.check(R.id.rb_normal) }
                    "우울", "슬픔", "DEPRESSION", "SAD" -> { selectedEmotionDrawable = R.drawable.emotion_sad; selectedEmotionText = "우울"; rgMood.check(R.id.rb_depression) }
                    "분노", "ANGER" -> { selectedEmotionDrawable = R.drawable.emotion_angry; selectedEmotionText = "분노"; rgMood.check(R.id.rb_anger) }
                    "피곤함", "FATIGUE", "TIRED" -> { selectedEmotionDrawable = R.drawable.emotion_tired; selectedEmotionText = "피곤함"; rgMood.check(R.id.rb_fatigue) }
                    else -> { selectedEmotionDrawable = R.drawable.emotion_normal; selectedEmotionText = "평온"; rgMood.check(R.id.rb_normal) }
                }
            }
            tvTagGuide.text = "#$selectedEmotionText"
            ivEmotionPreview.setImageResource(selectedEmotionDrawable)
        }

        val toggleListener = View.OnClickListener {
            scrollMood.visibility = if (scrollMood.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        tvTagGuide.setOnClickListener(toggleListener)
        ivEmotionPreview.setOnClickListener(toggleListener)

        rgMood.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_joy -> { selectedEmotionDrawable = R.drawable.emotion_joy; selectedEmotionText = "기쁨" }
                R.id.rb_confidence -> { selectedEmotionDrawable = R.drawable.emotion_confidence; selectedEmotionText = "자신감" }
                R.id.rb_calm -> { selectedEmotionDrawable = R.drawable.emotion_calm; selectedEmotionText = "평온" }
                R.id.rb_normal -> { selectedEmotionDrawable = R.drawable.emotion_normal; selectedEmotionText = "평범" }
                R.id.rb_depression -> { selectedEmotionDrawable = R.drawable.emotion_sad; selectedEmotionText = "우울" }
                R.id.rb_anger -> { selectedEmotionDrawable = R.drawable.emotion_angry; selectedEmotionText = "분노" }
                R.id.rb_fatigue -> { selectedEmotionDrawable = R.drawable.emotion_tired; selectedEmotionText = "피곤함" }
            }
            tvTagGuide.text = "#$selectedEmotionText"
            ivEmotionPreview.setImageResource(selectedEmotionDrawable)
            scrollMood.visibility = View.GONE
        }

        btnComplete.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 로딩 UI
            layoutWriteMode.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            tvLoadingText.visibility = View.VISIBLE

            // ✅ 저장/분석 로직은 AppService를 그대로 사용
            Thread {
                val entry = DiaryEntry(
                    entryId = if (editingEntryId > 0L) editingEntryId else 0L,
                    ownerId = ownerId,
                    dateYmd = DateUtil.todayYmd(),
                    title = title,
                    content = content,
                    mood = moodFromLabel(selectedEmotionText),
                    tags = listOf(selectedEmotionText),
                    isFavorite = false,
                    isTemporary = ownerId.startsWith("ANON_"),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val r = appService.saveEntryAndPrepareMindCardSafe(entry)

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    tvLoadingText.visibility = View.GONE
                    layoutWriteMode.visibility = View.VISIBLE

                    when (r) {
                        is AppResult.Success -> {
                            // 홈에서 DB 기반으로 다시 로드해서 보여주므로 여기서는 종료만
                            finish()
                        }
                        is AppResult.Failure -> {
                            Toast.makeText(this, "저장 실패: ${r.error}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.start()
        }
    }

    private fun moodFromLabel(label: String): Mood {
        // Mood enum이 한글 enum명이라면 그대로 valueOf 가능
        try {
            return Mood.valueOf(label)
        } catch (_: Exception) {
            // 영문 enum명일 수도 있으니 추가 매핑 시도
            return when (label) {
                "기쁨" -> safeMoodByName("JOY")
                "자신감" -> safeMoodByName("CONFIDENCE")
                "평온" -> safeMoodByName("CALM")
                "평범" -> safeMoodByName("NORMAL")
                "우울", "슬픔" -> safeMoodByName("DEPRESSION")
                "분노" -> safeMoodByName("ANGER")
                "피곤함" -> safeMoodByName("FATIGUE")
                else -> Mood.values().first()
            }
        }
    }

    private fun safeMoodByName(name: String): Mood {
        return Mood.values().firstOrNull { it.name == name } ?: Mood.values().first()
    }
}
