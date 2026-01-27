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

// SignupActivity : 회원가입 화면 Activity
// 용도:
// - 사용자가 닉네임/이메일/비밀번호/비밀번호 확인을 입력해 신규 회원을 생성한다.
// - 회원가입 성공 시 AppService가 세션 ownerId를 "USER_<id>" 형태로 설정하고, 이후 메인 시작 화면(StartActivity)부터는 회원 데이터로 앱이 동작한다.
// 설계:
// - 회원가입 결과는 AppResult로 받아 호출부에서 안전하게 분기한다.
// - DB 저장/해시 생성 같은 작업은 Dispatchers.IO에서 실행하고, 화면 이동/Toast 같은 UI 작업은 Main 스레드에서 처리한다.
class SignupActivity : AppCompatActivity() {

    // onCreate : 회원가입 화면 초기화
    // 동작 흐름:
    // 1) 입력 필드(닉네임/이메일/비밀번호/비밀번호 확인)를 참조한다.
    // 2) 회원가입 버튼 클릭 시 입력값을 수집해 appService.signUp을 호출한다.
    // 3) 성공이면 StartActivity로 이동하고, 실패면 오류 메시지를 Toast로 표시한다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // 입력 필드 참조
        val etNick = findViewById<TextInputEditText>(R.id.et_signup_username)
        val etEmail = findViewById<TextInputEditText>(R.id.et_signup_id)
        val etPw = findViewById<TextInputEditText>(R.id.et_signup_password)
        val etPw2 = findViewById<TextInputEditText>(R.id.et_signup_check_password)

        // 회원가입 버튼 클릭 처리
        findViewById<Button>(R.id.btn_signup).setOnClickListener {

            // 입력값 추출
            // - text가 null일 수 있으므로 orEmpty()로 안전하게 처리한다.
            val nick = etNick.text?.toString().orEmpty()
            val email = etEmail.text?.toString().orEmpty()

            // 비밀번호는 CharArray로 전달
            val pw = etPw.text?.toString().orEmpty().toCharArray()
            val pw2 = etPw2.text?.toString().orEmpty().toCharArray()

            // lifecycleScope : Activity 생명주기에 묶인 코루틴 스코프
            lifecycleScope.launch(Dispatchers.IO) {

                // 회원가입 수행
                // - 내부에서 입력값 검증(빈 값/형식/비밀번호 정책/불일치 등)과 이메일 중복 체크, 비밀번호 해시 저장, 사용자 생성까지 수행한다.
                val r = appService.signUp(email, pw, pw2, nick)

                // UI 처리는 Main 스레드에서 수행
                withContext(Dispatchers.Main) {
                    when (r) {

                        // 회원가입 성공:
                        // - signUp 내부에서 세션 ownerId를 USER_xxx로 전환해준다.
                        // - StartActivity로 이동 후 회원 상태로 앱 사용 가능
                        // - finish()로 회원가입 화면을 백스택에서 제거한다.
                        is AppResult.Success -> {
                            startActivity(Intent(this@SignupActivity, StartActivity::class.java))
                            finish()
                        }

                        // 회원가입 실패:
                        // - AppError에 표준화된 userMessage를 Toast로 표시한다.
                        is AppResult.Failure -> {
                            Toast.makeText(
                                this@SignupActivity, r.error.userMessage, Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
