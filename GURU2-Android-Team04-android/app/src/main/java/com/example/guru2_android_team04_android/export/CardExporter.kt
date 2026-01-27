package com.example.guru2_android_team04_android.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.guru2_android_team04_android.R
import com.example.guru2_android_team04_android.data.model.AiAnalysis
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import java.io.OutputStream

// CardExporter : 상세 분석 화면 카드를 생성하고 갤러리에 저장하는 유틸 객체
// 용도:
// - 분석 화면(일기/위로/실천안) 레이아웃(XML)을 그대로 Bitmap으로 렌더링해서 이미지로 저장한다.
// - 사용자가 분석 결과를 이미지 카드로 소장하거나 공유할 수 있게 한다.

// - 실제 화면을 스크린샷 하는 방식이 아니라, LayoutInflater로 XML View를 직접 생성한 뒤, 고정 크기로 measure/layout 후 Canvas에 draw해서 Bitmap을 만든다.
object CardExporter {

    // 저장 이미지 기본 해상도(세로형 카드)
    private const val EXPORT_W = 1080
    private const val EXPORT_H = 1920

    // renderLayoutToBitmap : XML 레이아웃을 Bitmap으로 변환하는 공통 함수
    private fun renderLayoutToBitmap(
        context: Context,
        layoutResId: Int,
        widthPx: Int = EXPORT_W,
        heightPx: Int = EXPORT_H,
        bind: (root: View) -> Unit
    ): Bitmap {
        // XML -> View 생성
        val root = LayoutInflater.from(context).inflate(layoutResId, null, false)

        // bind 콜백에서:
        // - TextView에 텍스트 주입
        // - ImageView에 아이콘 주입
        // - 저장 이미지에 불필요한 버튼/안내 문구 숨김 등을 수행한다.
        bind(root)

        // 고정 사이즈로 measure/layout
        // - 실제 화면 해상도/밀도와 관계없이 동일 비율의 이미지 카드 생성
        val wSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        root.measure(wSpec, hSpec)
        root.layout(0, 0, widthPx, heightPx)

        // View를 Bitmap에 렌더링
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        root.draw(canvas)
        return bmp
    }

    // renderAnalysisDiaryScreen : 1) 분석 시작(일기 카드) 화면을 Bitmap으로 생성한다.
    fun renderAnalysisDiaryScreen(context: Context, entry: DiaryEntry): Bitmap {
        return renderLayoutToBitmap(context, R.layout.activity_analysis_diary) { root ->

            // 저장 이미지에서는 탭 안내 버튼을 숨겨서 카드가 깔끔하게 보이도록 한다.
            root.findViewById<View>(R.id.btnTapContinue)?.visibility = View.GONE

            // 날짜/감정/제목/본문을 화면 요소에 바인딩한다.
            // - moodKo / iconResOf는 분석 화면(AnalysisDiaryActivity)과 동일한 매핑을 사용한다.
            root.findViewById<TextView>(R.id.tvDiaryDayText).text = entry.dateYmd
            root.findViewById<TextView>(R.id.tvDetailEmotionTag).text = "태그: ${moodKo(entry.mood)}"
            root.findViewById<ImageView>(R.id.ivDetailEmotion)
                .setImageResource(iconResOf(entry.mood))
            root.findViewById<TextView>(R.id.tvDiaryTitle).text = entry.title
            root.findViewById<TextView>(R.id.tvDiaryContent).text = entry.content
        }
    }

    // renderAnalysisComfortScreen : 2) 위로/격려 화면을 Bitmap으로 생성한다.
    // 용도:
    // - AI 분석의 상세 텍스트와 해시태그를 카드 형태로 저장한다.
    // 입력:
    // - analysis가 null일 수 있으므로(분석 실패/미생성) null-safe 처리를 포함한다.
    fun renderAnalysisComfortScreen(
        context: Context, entry: DiaryEntry, analysis: AiAnalysis?
    ): Bitmap {
        return renderLayoutToBitmap(context, R.layout.activity_analysis_comfort) { root ->

            // 저장 이미지에서는 탭 안내 버튼을 숨긴다.
            root.findViewById<View>(R.id.btnTapContinue)?.visibility = View.GONE

            // analysis가 없으면 안내 문구를 넣어 "빈 화면"을 방지한다.
            // 예외처리) 분석이 null인 경우에도 UI가 깨지지 않도록 기본 텍스트를 사용한다.
            val content = analysis?.fullText ?: "분석 결과가 없어요. 네트워크 상태를 확인한 후 다시 시도해주세요."

            // 해시태그는 "#태그" 형태로 공백 구분하여 하나의 문자열로 만든다.
            // 예외처리) analysis 또는 hashtags가 null이면 빈 문자열로 처리한다.
            val tags = analysis?.hashtags?.joinToString(" ") { "#$it" }.orEmpty()

            root.findViewById<TextView>(R.id.tv_analysis_content).text = content
            root.findViewById<TextView>(R.id.tv_tags).text = tags
        }
    }

    // renderAnalysisActionsScreen : 3) 오늘의 실천안 화면을 Bitmap으로 생성한다.
    // 용도:
    // - 미션 3개 + 미션 요약을 대표 마음 카드 이미지로 저장하기 위해 사용한다.
    // 입력:
    // - analysis가 null일 수 있으므로, actions/missionSummary에 기본값을 제공한다.
    fun renderAnalysisActionsScreen(
        context: Context, entry: DiaryEntry, analysis: AiAnalysis?
    ): Bitmap {
        return renderLayoutToBitmap(context, R.layout.activity_analysis_actions) { root ->

            // 저장 이미지에서는 화면 이동 버튼/안내 버튼을 숨겨 카드 자체만 남긴다.
            root.findViewById<View>(R.id.btnTapContinue)?.visibility = View.GONE

            // “이미지 카드로 소장하기 / 로그인 후…” 같은 버튼 영역은 이미지 저장 시 불필요하므로 숨긴다.
            root.findViewById<View>(R.id.layoutButtons)?.visibility = View.GONE

            // actions 리스트가 없거나 3개 미만일 수 있으므로 안전하게 기본값을 채운다.
            // 예외처리) getOrNull을 사용해 IndexOutOfBounds를 방지한다.
            val actions = analysis?.actions ?: emptyList()
            val a1 = actions.getOrNull(0) ?: "천천히 숨 고르기"
            val a2 = actions.getOrNull(1) ?: "따뜻한 물 한 잔 마시기"
            val a3 = actions.getOrNull(2) ?: "잠깐 스트레칭하기"

            root.findViewById<TextView>(R.id.tv_analysis_content1).text = "1. $a1"
            root.findViewById<TextView>(R.id.tv_analysis_content2).text = "2. $a2"
            root.findViewById<TextView>(R.id.tv_analysis_content3).text = "3. $a3"

            // missionSummary가 없으면 기본 메시지로 대체해 빈 문구를 방지한다.
            root.findViewById<TextView>(R.id.tv_tags).text =
                analysis?.missionSummary ?: "오늘의 꿀잠은 도망이 아니라, 내일을 위한 도움닫기예요."
        }
    }

    // renderMindCard(entry, analysis) : 이전 구조와의 호환을 위해 남겨둔 함수
    // 배경:
    // - 기존 AppService는 Bitmap 1장을 반환하는 renderMindCard(entry, analysis)를 호출하고 있었다.
    // 동작:
    // - 잘못된 사용을 조기에 잡기 위해 예외를 던지고, 올바른 오버로드(renderMindCard(context, entry, analysis)) 사용을 유도한다.
    fun renderMindCard(entry: DiaryEntry, analysis: AiAnalysis?): Bitmap {
        // 예외처리) Context 없이 XML을 inflate할 수 없으므로 런타임 예외로 명확히 안내한다.
        throw IllegalStateException(
            "renderMindCard(entry, analysis) 호출 대신 renderMindCard(context, entry, analysis)를 사용하세요."
        )
    }

    // renderMindCard(context, entry, analysis) : 실제 사용되는 대표 1장 생성 함수
    // 정책:
    // - 3장 중 어떤 화면을 대표로 할지 정해야 하므로, 현재는 actions 화면(실천안)을 대표 마음 카드로 사용한다.
    fun renderMindCard(context: Context, entry: DiaryEntry, analysis: AiAnalysis?): Bitmap {
        return renderAnalysisActionsScreen(context, entry, analysis)
    }

    // renderMindCard3Screens : 일기/위로/실천안 3장의 Bitmap을 한 번에 생성한다.
    // 용도:
    // - 사용자가 "3장 모두 저장"을 선택했을 때 사용한다.
    fun renderMindCard3Screens(
        context: Context, entry: DiaryEntry, analysis: AiAnalysis?
    ): List<Bitmap> {
        return listOf(
            renderAnalysisDiaryScreen(context, entry),
            renderAnalysisComfortScreen(context, entry, analysis),
            renderAnalysisActionsScreen(context, entry, analysis)
        )
    }

    // save3ScreensToGallery : 3장 Bitmap을 각각 갤러리에 저장하고 Uri 리스트를 반환한다.
    // 반환값:
    // - 저장된 이미지의 Uri 리스트(저장 성공 시 MediaStore 경로)
    // 파일명 규칙:
    // - baseName_1, baseName_2, baseName_3 형태로 저장한다.
    fun save3ScreensToGallery(
        context: Context, entry: DiaryEntry, analysis: AiAnalysis?, baseName: String
    ): List<Uri> {
        val bitmaps = renderMindCard3Screens(context, entry, analysis)
        return bitmaps.mapIndexed { idx, bmp ->
            saveToGallery(context, bmp, "${baseName}_${idx + 1}")
        }
    }

    // saveToGallery : Bitmap 1장을 MediaStore(갤러리)에 저장하고 Uri를 반환한다.
    // 동작 흐름:
    // 1) ContentValues로 파일 메타데이터(TITLE/DISPLAY_NAME/MIME_TYPE) 설정
    // 2) MediaStore에 insert하여 저장할 Uri를 생성
    // 3) openOutputStream(uri)로 스트림을 열고 JPEG로 compress하여 기록
    // 4) 성공 시 해당 Uri 반환
    fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, displayName)
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        // 예외처리) insert 결과가 null이면 저장 공간/권한/MediaStore 문제일 수 있으므로 예외로 처리한다.
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: throw RuntimeException("MediaStore insert failed")

        var os: OutputStream? = null
        try {
            os = context.contentResolver.openOutputStream(uri)

            // 예외처리) OutputStream을 열지 못하면 파일 기록이 불가능하므로 예외로 처리한다.
            if (os == null) throw RuntimeException("openOutputStream failed")

            // JPEG로 압축 저장
            // - quality 92: 용량/품질 절충(너무 낮으면 글자가 깨질 수 있음)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, os)
            os.flush()
            return uri
        } finally {
            // 예외처리) 스트림 close 과정에서 예외가 나더라도 앱이 죽지 않도록 try/catch로 방어한다.
            try {
                os?.close()
            } catch (_: Exception) {
            }
        }
    }

    // moodKo : 감정 enum을 한국어 문자열로 변환한다.
    // 용도:
    // - 일기 카드 화면에서 감정 라벨을 표시할 때 사용한다.
    private fun moodKo(m: Mood): String = when (m) {
        Mood.JOY -> "기쁨"
        Mood.CONFIDENCE -> "자신감"
        Mood.CALM -> "평온"
        Mood.NORMAL -> "평범"
        Mood.DEPRESSED -> "우울"
        Mood.ANGRY -> "분노"
        Mood.TIRED -> "피곤함"
    }

    // iconResOf : 감정 enum에 대응하는 아이콘 리소스 id를 반환한다.
    // 용도:
    // - 감정별로 다른 표정 아이콘을 표시하기 위해 사용한다.
    // - XML 캡처 방식에서도 setImageResource로 동일한 아이콘을 넣는다.
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
