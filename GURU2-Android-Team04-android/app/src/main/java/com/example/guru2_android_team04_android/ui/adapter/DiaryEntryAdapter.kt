package com.example.guru2_android_team04_android.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guru2_android_team04_android.R
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.databinding.ItemDiaryEntryBinding
import java.util.Calendar

// DiaryEntryAdapter : 주차별 일기 리스트(RecyclerView)에 들어갈 일기 카드 아이템 어댑터
// 용도:
// - DiaryEntry(일기 1개)를 item_diary_entry.xml(= ItemDiaryEntryBinding)과 연결한다.
// - 각 카드에 감정 아이콘/제목/일자/요일(SUN~SAT)을 표시한다.
// - 카드 클릭 시 onClick(entry)를 호출하여 상세 화면으로 이동하도록 확장 가능하게 만든다.
// 구현:
// - ListAdapter + DiffUtil을 사용해 리스트 변경 시 필요한 부분만 갱신
class DiaryEntryAdapter(
    // onClick : 아이템 클릭 시 실행할 동작을 외부에서 주입
    private val onClick: (DiaryEntry) -> Unit
) : ListAdapter<DiaryEntry, DiaryEntryAdapter.VH>(diff) {

    // ViewHolder 생성: item_diary_entry.xml을 ViewBinding으로 inflate
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDiaryEntryBinding.inflate(inflater, parent, false)
        return VH(binding, onClick)
    }

    // position에 해당하는 DiaryEntry를 ViewHolder에 바인딩
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    // VH(ViewHolder) : 화면에 보이는 1개 카드의 View들을 관리
    class VH(
        private val b: ItemDiaryEntryBinding, private val onClick: (DiaryEntry) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {

        // entry 1개 데이터를 카드 UI에 표시
        fun bind(entry: DiaryEntry) {
            // 감정 아이콘 표시
            b.ivEmotion.setImageResource(iconResOf(entry.mood))

            // 일기 제목 표시
            b.tvDiaryTitle.text = entry.title

            // 날짜(일): "yyyy-MM-dd"의 마지막 2자리를 가져와 숫자로 변환 후 표시
            // 예외처리) 형식이 깨져 변환 실패하면 빈 문자열로 표시한다.
            b.tvDay.text = entry.dateYmd.takeLast(2).toIntOrNull()?.toString().orEmpty()

            // 요일(SUN~SAT) 계산
            b.tvDow.text = weekdayShort(entry.dateYmd)

            // 카드 클릭 시 entry를 콜백으로 전달
            b.root.setOnClickListener { onClick(entry) }
        }

        // weekdayShort : "yyyy-MM-dd" 문자열을 Calendar로 변환하여 요일 축약(SUN~SAT) 반환
        private fun weekdayShort(ymd: String): String {
            // 예외처리) 숫자 파싱 실패 시 요일을 계산할 수 없으므로 빈 문자열 반환
            val y = ymd.take(4).toIntOrNull() ?: return ""
            val m = ymd.drop(5).take(2).toIntOrNull() ?: return ""
            val d = ymd.takeLast(2).toIntOrNull() ?: return ""

            val c = Calendar.getInstance()
            c.set(y, m - 1, d) // Calendar의 month는 0부터 시작하므로 -1

            return when (c.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "SUN"
                Calendar.MONDAY -> "MON"
                Calendar.TUESDAY -> "TUE"
                Calendar.WEDNESDAY -> "WED"
                Calendar.THURSDAY -> "THU"
                Calendar.FRIDAY -> "FRI"
                Calendar.SATURDAY -> "SAT"
                else -> ""
            }
        }

        // mood -> drawable 리소스 매핑
        // - 리스트 카드에서 감정 이모티콘을 보여주기 위해 사용한다.
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

    companion object {
        // diff : ListAdapter가 아이템이 같은지 / 내용이 같은지 비교할 기준
        // - areItemsTheSame: entryId가 같으면 같은 일기(같은 아이템)로 판단
        // - areContentsTheSame: 데이터 클래스 equals로 내용 비교(완전히 동일하면 true)
        private val diff = object : DiffUtil.ItemCallback<DiaryEntry>() {
            override fun areItemsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry): Boolean =
                oldItem.entryId == newItem.entryId

            override fun areContentsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry): Boolean =
                oldItem == newItem
        }
    }
}
