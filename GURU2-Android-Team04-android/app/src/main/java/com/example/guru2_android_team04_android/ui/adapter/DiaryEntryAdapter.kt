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

class DiaryEntryAdapter(
    private val onClick: (DiaryEntry) -> Unit
) : ListAdapter<DiaryEntry, DiaryEntryAdapter.VH>(diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDiaryEntryBinding.inflate(inflater, parent, false)
        return VH(binding, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val b: ItemDiaryEntryBinding,
        private val onClick: (DiaryEntry) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(entry: DiaryEntry) {
            b.ivEmotion.setImageResource(iconResOf(entry.mood))
            b.tvDiaryTitle.text = entry.title
            b.tvDay.text = entry.dateYmd.takeLast(2).toIntOrNull()?.toString().orEmpty()
            b.tvDow.text = weekdayShort(entry.dateYmd)

            b.root.setOnClickListener { onClick(entry) }
        }

        private fun weekdayShort(ymd: String): String {
            val y = ymd.take(4).toIntOrNull() ?: return ""
            val m = ymd.drop(5).take(2).toIntOrNull() ?: return ""
            val d = ymd.takeLast(2).toIntOrNull() ?: return ""
            val c = Calendar.getInstance()
            c.set(y, m - 1, d)
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
        private val diff = object : DiffUtil.ItemCallback<DiaryEntry>() {
            override fun areItemsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry): Boolean =
                oldItem.entryId == newItem.entryId

            override fun areContentsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry): Boolean =
                oldItem == newItem
        }
    }
}
