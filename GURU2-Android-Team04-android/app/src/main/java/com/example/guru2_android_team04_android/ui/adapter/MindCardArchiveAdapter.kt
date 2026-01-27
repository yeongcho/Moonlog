package com.example.guru2_android_team04_android.ui.bind

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guru2_android_team04_android.R
import com.example.guru2_android_team04_android.data.model.MindCardPreview
import com.example.guru2_android_team04_android.util.MindCardTextUtil

// MindCardArchiveAdapter : 마음 카드 보관함 RecyclerView Adapter
// 용도:
// - MindCardPreview 목록을 RecyclerView 아이템(item_mindcard.xml)에 바인딩한다.
// - 아이템 내 클릭 이벤트(즐겨찾기 해제, 상세 보기 이동)를 외부(Binder)에서 주입받아 실행한다.
class MindCardArchiveAdapter(
    private val nicknameProvider: () -> String,
    private val onUnfavorite: (MindCardPreview) -> Unit,
    private val onOpenDetail: (MindCardPreview) -> Unit
) : RecyclerView.Adapter<MindCardArchiveAdapter.VH>() {

    // items : 현재 RecyclerView에 표시할 마음 카드 데이터 목록
    private val items = mutableListOf<MindCardPreview>()

    // submitList : 새 목록을 전달받아 화면을 갱신한다.
    // - 현재는 notifyDataSetChanged()를 사용하여 전체를 다시 그린다(간단하지만 효율은 낮음).
    // - 성능 개선이 필요하면 DiffUtil/ListAdapter로 교체할 수 있다.
    fun submitList(newItems: List<MindCardPreview>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // ViewHolder 생성: item_mindcard.xml을 inflate해서 VH로 감싼다.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mindcard, parent, false)
        return VH(v as ViewGroup)
    }

    // 데이터 바인딩: position에 해당하는 MindCardPreview 내용을 각 View에 세팅한다.
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // nicknameProvider를 통해 "현재 닉네임"을 가져온다.
        // - 프로필이 바뀌어도 Adapter 재생성 없이 최신 값을 반영하기 좋다.
        val nickname = nicknameProvider()

        // 아이템 상단 날짜 표시: "yyyy년 M월 d일 마음 카드🌙"
        // - DB에서 넘어오는 dateYmd("yyyy-MM-dd")를 한국어 날짜로 단순 변환한다.
        holder.tvDay.text = "${formatKoreanDate(item.dateYmd)} 마음 카드🌙"

        // 마음 카드 프리뷰 2줄 생성
        // - MindCardTextUtil.makeComfortLines(...)는 닉네임/프리뷰 문장을 조합해 2줄로 깔끔하게 만들어준다.
        val (line1, line2) = MindCardTextUtil.makeComfortLines(nickname, item.comfortPreview)
        holder.tvNick.text = line1
        holder.tvConsole.text = line2

        // 오늘의 미션: 서버/DB에 미션이 없으면 기본 문구로 대체한다.
        holder.tvMission.text = "오늘의 미션: ${item.mission ?: "천천히 숨 고르기"}"

        // 즐겨찾기(하트) 클릭 -> 즐겨찾기 해제 콜백 실행
        holder.ivFav.setOnClickListener { onUnfavorite(item) }

        // "마음 분석 상세 보기" 클릭 -> 상세 화면 이동 콜백 실행
        holder.tvLook.setOnClickListener { onOpenDetail(item) }
    }

    override fun getItemCount(): Int = items.size

    // VH(ViewHolder) : item_mindcard.xml에서 필요한 View 참조를 캐싱한다.
    // - findViewById를 매번 호출하지 않아서 스크롤 성능에 유리하다.
    class VH(root: ViewGroup) : RecyclerView.ViewHolder(root) {
        val tvDay: TextView = root.findViewById(R.id.tvDayMsg)
        val tvNick: TextView = root.findViewById(R.id.tvNicknameMsg)
        val tvConsole: TextView = root.findViewById(R.id.tvConsoleText)
        val tvMission: TextView = root.findViewById(R.id.tvMissionText)
        val ivFav: ImageView = root.findViewById(R.id.ivFavorite)
        val tvLook: TextView = root.findViewById(R.id.tvLookAnalysis)
    }

    // formatKoreanDate : "yyyy-MM-dd" 형식 문자열을 "yyyy년 M월 d일"로 단순 변환한다.
    // 예외처리) 월/일이 숫자로 파싱되지 않으면 1월 1일로 fallback 하여 앱 크래시를 방지한다.
    private fun formatKoreanDate(ymd: String): String {
        val y = ymd.take(4)
        val m = ymd.drop(5).take(2).toIntOrNull() ?: 1
        val d = ymd.takeLast(2).toIntOrNull() ?: 1
        return "${y}년 ${m}월 ${d}일"
    }
}
