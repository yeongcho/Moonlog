package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guru2_android_team04_android.AppService
import com.example.guru2_android_team04_android.ArchiveDiaryDetailActivity
import com.example.guru2_android_team04_android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// MindCardArchiveUiBinder : 마음 카드 보관함(activity_mindcard_archive.xml) <-> AppService 연동 전담 클래스
// 용도:
// - 마음 카드 보관함 화면에서 RecyclerView 구성, 클릭 이벤트 처리, 데이터 로딩/갱신을 담당한다.
// - Activity는 화면 표시(setContentView)와 생명주기만 관리하고, 실제 화면 로직은 Binder가 맡는다.
// 데이터 기준:
// - "즐겨찾기(하트)된 일기"에 대해서만 MindCardPreview 목록을 가져와 표시한다.
class MindCardArchiveUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    // ownerId : 현재 사용자 식별자
    // - 즐겨찾기 목록 조회/즐겨찾기 해제 API 호출에 필요하다.
    private var ownerId: String = ""

    // nickname : 마음 카드 문구 1줄 생성에 사용되는 사용자 닉네임
    // - MindCardTextUtil.makeComfortLines(...)에서 사용한다.
    private var nickname: String = ""

    // rv : 마음 카드들을 보여주는 RecyclerView
    private lateinit var rv: RecyclerView

    // adapter : 마음 카드 아이템 바인딩/클릭 이벤트(즐겨찾기 해제/상세 이동)를 담당
    private lateinit var adapter: MindCardArchiveAdapter

    // bind : 화면의 View를 찾아서 RecyclerView/Adapter를 연결하고, 최초 데이터를 로드한다.
    // 동작 흐름:
    // 1) RecyclerView와 Adapter를 연결한다.
    // 2) 프로필(ownerId/nickname)을 로드한다.
    // 3) 로그인 사용자(USER_)인지 검증한다.
    // 4) refresh()로 마음 카드 목록을 받아와 화면에 표시한다.
    fun bind() {
        // rvMindCards : activity_mindcard_archive.xml의 RecyclerView id
        rv = activity.findViewById(R.id.rvMindCards)

        // Adapter 생성
        // - nicknameProvider: onBind 시점에 최신 nickname을 가져오기 위한 람다
        // - onUnfavorite: 하트 클릭 시 즐겨찾기 해제 처리
        // - onOpenDetail: "마음 분석 상세 보기" 클릭 시 상세 화면으로 이동
        adapter = MindCardArchiveAdapter(
            nicknameProvider = { nickname },
            onUnfavorite = { item ->
                // 즐겨찾기 해제 버튼(하트) 클릭 처리
                // - UI 이벤트는 Main에서 시작되지만, 실제 DB/네트워크 작업은 IO로 이동한다.
                // - 작업 후에는 refresh()로 목록을 다시 로드해 보관함에서 제거된 상태를 반영한다.
                activity.lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            // setEntryFavorite(ownerId, entryId, false) : 즐겨찾기 해제
                            appService.setEntryFavorite(ownerId, item.entryId, false)
                        }.isSuccess
                    }

                    // 예외처리) 즐겨찾기 해제 실패 시 토스트로 안내하고, 그래도 목록 갱신은 시도한다.
                    if (!ok) {
                        Toast.makeText(activity, "즐겨찾기 해제에 실패했어요.", Toast.LENGTH_SHORT).show()
                    }

                    // 즐겨찾기 해제 결과를 반영하기 위해 목록 재조회
                    refresh()
                }
            },
            onOpenDetail = { item ->
                // 상세 화면 이동
                // - ArchiveDiaryDetailActivity는 entryId를 받아서 해당 일기 상세 + 분석 카드까지 보여준다.
                activity.startActivity(
                    Intent(activity, ArchiveDiaryDetailActivity::class.java).apply {
                        putExtra("entryId", item.entryId)
                    }
                )
            }
        )

        // RecyclerView 레이아웃/어댑터 연결
        // - LinearLayoutManager: 세로 스크롤 리스트 형태로 표시
        rv.layoutManager = LinearLayoutManager(activity)
        rv.adapter = adapter

        // 프로필 로드 + 첫 로딩
        // - ownerId/nickname은 이후 API 호출 및 문구 생성에 반드시 필요하다.
        activity.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) { appService.getUserProfile() }
            ownerId = profile.ownerId
            nickname = profile.nickname

            // 예외처리) 게스트(ownerId가 USER_로 시작하지 않음)는 보관함을 이용할 수 없으므로 안내 후 종료한다.
            if (!ownerId.startsWith("USER_")) {
                Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                activity.finish()
                return@launch
            }

            // 최초 목록 로드
            refresh()
        }
    }

    // refresh : 즐겨찾기된 마음 카드 목록을 다시 로드하여 RecyclerView에 반영한다.
    // - suspend 함수로 만들어, bind() / 즐겨찾기 해제 처리 후에 자연스럽게 재사용한다.
    private suspend fun refresh() {
        // 예외처리) ownerId가 아직 세팅되지 않았다면 API 호출을 할 수 없으므로 중단한다.
        if (ownerId.isBlank()) return

        // IO에서 목록 조회
        // - 실패 시 앱이 죽지 않도록 emptyList()로 대체한다.
        val items = withContext(Dispatchers.IO) {
            runCatching { appService.getMindCardArchive(ownerId) }
                .getOrElse { emptyList() }
        }

        // Main에서 UI 반영
        withContext(Dispatchers.Main) {
            adapter.submitList(items)

            // 아이템이 없으면 RecyclerView를 숨겨 빈 상태를 표현한다.
            rv.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}