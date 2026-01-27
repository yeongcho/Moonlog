package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.util.DateUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// BottomNavBinder : 하단 BottomNavigationView 클릭 이벤트를 화면 이동 규칙에 맞게 연결해주는 바인더
// 용도:
// - 각 Activity에서 공통으로 쓰는 하단 네비게이션 로직을 한 곳에 모아 중복 코드를 줄인다.
// - 메뉴 클릭 시 이동할 화면과 게스트/회원 접근 제한 규칙을 적용한다.
// - 일기 화면은 오늘 일기 존재 여부에 따라 작성 화면/상세 화면으로 분기한다.
// 동작 흐름:
// 1) 현재 Activity에서 nav_view를 찾고(selectedItemId를 반영) 선택 상태를 표시한다.
// 2) 메뉴 클릭 시 itemId에 따라 이동할 Activity를 결정한다.
// 3) 필요한 경우 AppService를 통해 사용자 상태(게스트/회원)나 오늘 일기 존재 여부를 조회한다.
// 4) 조회는 IO 스레드에서 처리하고, 화면 이동(startActivity/finish)은 Main 스레드에서 처리한다.
object BottomNavBinder {

    // bind : 현재 화면(Activity)에 BottomNavigationView 이벤트를 연결한다.
    // 파라미터:
    // - activity: 네비게이션을 붙일 대상 Activity
    // - selectedItemId: 현재 화면에서 선택되어 있어야 하는 메뉴 id(선택 강조 표시용)
    fun bind(activity: AppCompatActivity, selectedItemId: Int) {

        // 예외처리) 현재 레이아웃에 nav_view가 없으면(예: 네비게이션 없는 화면) 아무 것도 하지 않고 종료한다.
        val nav = activity.findViewById<BottomNavigationView>(R.id.nav_view) ?: return

        // 현재 화면이 어떤 탭인지 UI에 선택 상태로 반영한다.
        nav.selectedItemId = selectedItemId

        // 하단 탭 클릭 이벤트 처리
        nav.setOnItemSelectedListener { item ->

            // AppService : 화면 이동 결정에 필요한 사용자/일기 정보를 조회하기 위한 서비스
            // - MyApp이 앱 전역으로 들고 있는 단일 인스턴스를 사용한다.
            val appService = (activity.application as MyApp).appService

            when (item.itemId) {

                // 홈 탭: HomeActivity로 이동
                R.id.navigation_home -> {
                    // 같은 탭을 다시 눌렀을 때 불필요한 재진입을 막는다.
                    if (selectedItemId != R.id.navigation_home) {
                        activity.startActivity(Intent(activity, HomeActivity::class.java))
                        // 네비게이션 이동은 보통 "현재 화면 종료"로 스택을 깔끔히 유지한다.
                        activity.finish()
                    }
                    true
                }

                // 일기 탭: 오늘 일기 유무에 따라 작성 화면/상세 화면으로 분기
                R.id.navigation_diary -> {
                    // 같은 탭이면 아무 것도 하지 않는다.
                    if (selectedItemId == R.id.navigation_diary) return@setOnItemSelectedListener true

                    // 오늘 일기 조회는 DB/스토리지 접근이 섞일 수 있으니 IO 스레드에서 처리한다.
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        // 프로필에서 ownerId를 가져와 "내 데이터" 기준으로 조회한다.
                        val profile = appService.getUserProfile()
                        val ownerId = profile.ownerId

                        // 오늘 날짜(yyyy-MM-dd)와 이번 달(yyyy-MM)을 만든다.
                        val todayYmd = DateUtil.todayYmd()
                        val ym = todayYmd.take(7)

                        // 이번 달 엔트리 중 오늘 날짜와 같은 일기가 있는지 찾는다.
                        val todayEntry = appService.getEntriesByMonth(ownerId, ym)
                            .firstOrNull { it.dateYmd == todayYmd }

                        // 화면 이동은 Main 스레드에서 처리해야 안전하다.
                        withContext(Dispatchers.Main) {
                            if (todayEntry == null) {
                                // 오늘 일기가 없으면 새로 작성 화면으로 이동
                                activity.startActivity(Intent(activity, DiaryEditorActivity::class.java))
                            } else {
                                // 오늘 일기가 있으면 상세 화면으로 이동(entryId 전달)
                                activity.startActivity(
                                    Intent(activity, TodayDiaryDetailActivity::class.java).apply {
                                        putExtra("entryId", todayEntry.entryId)
                                    }
                                )
                            }
                            // 네비게이션 이동이므로 현재 화면은 종료
                            activity.finish()
                        }
                    }
                    true
                }

                // 캘린더 탭: 게스트는 접근 제한
                R.id.navigation_calendar -> {

                    // currentOwnerIdOrNull()로 현재 사용자 ownerId를 가져온다.
                    // - ownerId가 "USER_"로 시작하면 회원, 그 외(또는 null)이면 게스트로 간주한다.
                    val ownerId = appService.currentOwnerIdOrNull()
                    val isMember = ownerId?.startsWith("USER_") == true

                    // 예외처리) 게스트는 캘린더/보관함 기능을 쓰지 못하므로 토스트 안내 후 선택을 취소한다.
                    if (!isMember) {
                        Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                        // false를 반환하면 해당 탭 선택이 적용되지 않는다.
                        return@setOnItemSelectedListener false
                    }

                    // 같은 탭이면 이동하지 않는다.
                    if (selectedItemId != R.id.navigation_calendar) {
                        activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java))
                        activity.finish()
                    }
                    true
                }

                // 마이페이지 탭: MyPageActivity로 이동
                R.id.navigation_mypage -> {
                    // 같은 탭이면 이동하지 않는다.
                    if (selectedItemId != R.id.navigation_mypage) {
                        activity.startActivity(Intent(activity, MyPageActivity::class.java))
                        activity.finish()
                    }
                    true
                }

                // 예외처리) 정의되지 않은 itemId가 들어오면 처리하지 않는다.
                else -> false
            }
        }
    }
}
