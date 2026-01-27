package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.util.DateUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// StartActivity : 홈 진입 직전 짧게 노출되는 환영/인트로 화면 Activity
// 용도:
// - 오늘 요일에 맞춘 인사 문구를 보여주고, 잠시 후 홈(HomeActivity)으로 자동 이동한다.
// - 앱의 분위기(감정 기록 앱)를 전달하는 전환 화면 역할
// 설계:
// - 화면에 표시되는 문구는 DateUtil.todayWeekdayKo()를 이용해 요일을 동적으로 반영한다.
// - 1.2초 후 HomeActivity로 이동하며, finish()로 백스택에 남지 않게 한다.
// - 특정 TextView id를 고정하지 않고 "첫 번째 TextView"를 찾아 문구를 교체한다.
class StartActivity : AppCompatActivity() {

    // onCreate : 인트로 화면 초기화
    // 동작 흐름:
    // 1) activity_start 레이아웃을 표시한다.
    // 2) 레이아웃 트리에서 첫 TextView를 찾아 인사 문구를 동적으로 설정한다.
    // 3) 잠시 대기 후 HomeActivity로 이동하고, 현재 화면은 종료한다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // 레이아웃 내 첫 번째 TextView를 찾아 문구를 교체한다.
        // - window.decorView는 현재 Activity 화면의 최상위 View(root)이다.
        // - findFirstTextView()는 View 트리를 DFS 방식으로 탐색한다.
        val tv = findFirstTextView(window.decorView)

        // 요일을 한국어로 가져와 환영 문구에 삽입한다.
        val weekday = DateUtil.todayWeekdayKo()
        tv?.text = "좋은 ${weekday}이에요! \n오늘 당신의 하루는 어떤 색이었나요?"

        // 일정 시간 후 홈(HomeActivity)으로 이동
        // - lifecycleScope를 사용해 Activity 생명주기와 함께 코루틴이 관리되게 한다.
        lifecycleScope.launch {

            // delay는 메인 스레드를 막지 않는 대기 방식
            // 예외처리) Thread.sleep을 쓰면 UI가 멈출 수 있으므로 delay를 사용한다.
            delay(1200)

            startActivity(Intent(this@StartActivity, HomeActivity::class.java))

            // finish : StartActivity를 백스택에서 제거
            // - 사용자가 뒤로가기를 눌러도 인트로 화면으로 돌아오지 않는다.
            finish()
        }
    }

    // findFirstTextView : View 트리에서 가장 먼저 발견되는 TextView를 찾아 반환한다.
    // 용도:
    // - activity_start 레이아웃에서 특정 id를 고정하지 않고도,
    //   첫 TextView의 문구를 동적으로 바꾸기 위해 사용한다.
    // 동작 방식:
    // - v가 TextView면 즉시 반환
    // - v가 ViewGroup이면 자식들을 순서대로 재귀 탐색
    // - 끝까지 못 찾으면 null 반환
    private fun findFirstTextView(v: View?): TextView? {

        // 예외처리) 입력 View가 null이면 탐색 불가이므로 null 반환
        if (v == null) return null

        // 현재 View가 TextView면 정답이므로 반환
        if (v is TextView) return v

        // ViewGroup이면 자식 View를 순회하며 재귀적으로 탐색한다.
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                val found = findFirstTextView(v.getChildAt(i))
                if (found != null) return found
            }
        }

        // 끝까지 못 찾으면 null
        return null
    }
}
