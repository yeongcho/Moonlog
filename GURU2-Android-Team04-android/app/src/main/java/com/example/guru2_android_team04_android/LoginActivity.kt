package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.core.AppResult
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// LoginActivity : 회원 로그인 화면 Activity
// 용도:
// - 사용자가 이메일/비밀번호를 입력해 로그인한다.
// - 로그인 성공 시 AppService가 현재 세션 ownerId를 "USER_<id>" 형태로 설정하고, 이후 화면(StartActivity)에서 해당 ownerId 기준으로 데이터를 조회/저장한다.
// 설계:
// - 로그인 결과는 AppResult(Success/Failure)로 받아 예외를 던지지 않고 안전하게 분기 처리한다.
// - DB 조회/해시 검증 같은 무거운 작업은 Dispatchers.IO에서 수행하고, UI 변경(화면 이동/Toast)은 Main 스레드에서 수행한다.
class LoginActivity : AppCompatActivity() {

    // onCreate : 로그인 화면 초기화
    // 동작 흐름:
    // 1) 입력창(TextInputEditText) 참조를 얻는다.
    // 2) 로그인 버튼 클릭 시 입력값을 가져와 appService.login을 호출한다.
    // 3) 결과가 성공이면 StartActivity로 이동, 실패면 오류 메시지를 Toast로 표시한다.
    // 4) 회원가입 버튼 클릭 시 SignupActivity로 이동한다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 이메일/비밀번호 입력 필드 참조
        val etId = findViewById<TextInputEditText>(R.id.et_id)
        val etPw = findViewById<TextInputEditText>(R.id.et_password)

        // 로그인 버튼 클릭 처리
        findViewById<Button>(R.id.btn_login).setOnClickListener {

            // 입력값 추출
            // - text가 null일 수 있으므로 orEmpty()로 안전하게 문자열을 만든다.
            val email = etId.text?.toString().orEmpty()

            // 비밀번호는 CharArray로 전달
            val pw = etPw.text?.toString().orEmpty().toCharArray()

            // lifecycleScope : Activity 생명주기와 함께 관리되는 코루틴 스코프
            lifecycleScope.launch(Dispatchers.IO) {

                // 로그인 수행(내부에서 이메일 정규화/비밀번호 해시 검증/세션 ownerId 세팅 등)
                val r = appService.login(email, pw)

                // UI 처리는 Main 스레드에서 수행해야 한다.
                withContext(Dispatchers.Main) {
                    when (r) {

                        // 로그인 성공:
                        // - 메인 시작 화면(StartActivity)로 이동
                        // - finish()로 로그인 화면을 백스택에서 제거(뒤로가기 시 돌아오지 않게)
                        is AppResult.Success -> {
                            startActivity(Intent(this@LoginActivity, StartActivity::class.java))
                            finish()
                        }

                        // 로그인 실패:
                        // - AppError에 표준화된 userMessage를 Toast로 표시한다.
                        is AppResult.Failure -> {
                            Toast.makeText(
                                this@LoginActivity, r.error.userMessage, Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        // 회원가입 버튼 클릭 처리
        // - 회원가입 화면으로 이동한다.
        findViewById<Button>(R.id.btn_signup).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
