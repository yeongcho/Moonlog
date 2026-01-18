package com.example.emotiondiary

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class WriteActivity : AppCompatActivity() {

    private var selectedEmotionDrawable: Int = R.drawable.emotion_normal
    private var selectedEmotionText: String = "평온"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        window.statusBarColor = getColor(R.color.main_bg_sage)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

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

        // ★ [핵심] 아이콘 크기 강제 조절 코드 (40dp) ★
        // 이 코드가 없으면 아이콘이 엄청 크게 나올 수 있습니다.
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
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
                val scaledDrawable = BitmapDrawable(resources, scaledBitmap)
                radioButton.setCompoundDrawablesWithIntrinsicBounds(null, scaledDrawable, null, null)
            }
        }
        // -----------------------------------------------------------

        if (DiaryData.isWritten) {
            etTitle.setText(DiaryData.title)
            etContent.setText(DiaryData.content)
            selectedEmotionText = DiaryData.emotionText
            selectedEmotionDrawable = DiaryData.emotionIcon

            tvTagGuide.text = "#$selectedEmotionText"
            ivEmotionPreview.setImageResource(selectedEmotionDrawable)

            val buttonId = when(DiaryData.emotionText) {
                "기쁨" -> R.id.rb_joy
                "자신감" -> R.id.rb_confidence
                "평온" -> R.id.rb_calm
                "평범" -> R.id.rb_normal
                "우울" -> R.id.rb_depression
                "분노" -> R.id.rb_anger
                "피곤함" -> R.id.rb_fatigue
                else -> R.id.rb_normal
            }
            rgMood.check(buttonId)
        }

        val toggleListener = View.OnClickListener {
            if (scrollMood.visibility == View.VISIBLE) scrollMood.visibility = View.GONE
            else scrollMood.visibility = View.VISIBLE
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
            val title = etTitle.text.toString()
            val content = etContent.text.toString()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            layoutWriteMode.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            tvLoadingText.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                DiaryData.isWritten = true
                DiaryData.title = title
                DiaryData.content = content
                DiaryData.emotionText = selectedEmotionText
                DiaryData.emotionIcon = selectedEmotionDrawable

                finish()
            }, 3000)
        }
    }
}