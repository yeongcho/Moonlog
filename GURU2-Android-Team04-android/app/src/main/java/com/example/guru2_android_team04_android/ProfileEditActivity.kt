package com.example.guru2_android_team04_android

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.ProfileEditUiBinder

// ProfileEditActivity : 프로필 편집 화면 Activity
// 용도:
// - 사용자 프로필(닉네임/프로필 이미지)을 수정하고 저장하는 화면(activity_profile_edit.xml)을 제공한다.
// - 프로필 이미지 클릭 시 갤러리(문서 선택기)를 열어 이미지를 선택하게 한다.
// - 선택된 이미지 URI에 대해 persistable permission을 획득하여 앱 재실행 후에도 접근 가능하게 한다.
// 동작 흐름:
// 1) Activity가 시작되면 binder.bind()로 현재 프로필 정보를 화면에 표시한다.
// 2) 프로필 이미지 클릭 시 OpenDocument 런처로 이미지 선택을 요청한다.
// 3) 이미지 선택 성공 시 takePersistableUriPermission으로 읽기 권한을 영구 유지한다.
// 4) 선택된 URI를 binder.setSelectedProfileImageUri로 전달해 미리보기 및 저장 대상으로 등록한다.
// 5) 저장 버튼 클릭 시 Binder가 닉네임/이미지 업데이트를 처리하고 종료한다.
// 설계:
// - Activity는 "이미지 선택(런처)"과 화면 진입만 담당한다.
// - 실제 데이터 조회/저장 및 UI 반영은 ProfileEditUiBinder로 분리한다.
class ProfileEditActivity : AppCompatActivity() {

    // appService : 프로필 조회/닉네임 변경/프로필 이미지 URI 저장을 담당하는 서비스 계층
    private val appService by lazy { (application as MyApp).appService }

    // binder : activity_profile_edit.xml ↔ AppService 연결 및 버튼 이벤트 로직 담당
    private lateinit var binder: ProfileEditUiBinder

    // pickImageLauncher : 이미지 선택을 위한 ActivityResultLauncher
    // - 기존 GetContent 대신 OpenDocument를 사용한다.
    // - OpenDocument는 persistable permission을 지원하여, 선택한 사진 URI 접근 권한을 영구 유지할 수 있다.
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->

            // 예외처리) 사용자가 선택을 취소했거나 URI가 없으면 아무 작업도 하지 않는다.
            if (uri == null) return@registerForActivityResult

            // 선택한 URI에 대해 읽기 권한을 영구적으로 유지한다.
            // - 앱이 종료되거나 재시작되어도 해당 이미지 URI를 다시 읽을 수 있게 하기 위함이다.
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            // 선택한 이미지를 Binder에 전달한다.
            // - Binder는 화면 미리보기 반영 + 저장 시 업데이트 대상 URI로 사용한다.
            binder.setSelectedProfileImageUri(uri.toString())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 프로필 편집 레이아웃(activity_profile_edit.xml)을 화면에 부착한다.
        setContentView(R.layout.activity_profile_edit)

        // Binder 생성 후 바인딩 실행
        // - 현재 프로필 정보 표시(닉네임/이메일/D-day/배지/프로필 이미지)
        // - 저장 버튼 클릭 로직 연결
        binder = ProfileEditUiBinder(this, appService)
        binder.bind()

        // 프로필 이미지 클릭 시 갤러리(문서 선택기)를 연다.
        findViewById<android.widget.ImageView>(R.id.iv_profile_image).setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }
    }

    override fun onResume() {
        super.onResume()
        // 다른 화면에서 복귀했을 때 최신 프로필 정보로 갱신한다.
        binder.bind()
    }
}
