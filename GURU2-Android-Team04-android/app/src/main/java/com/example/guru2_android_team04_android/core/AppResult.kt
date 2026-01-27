package com.example.guru2_android_team04_android.core

// AppResult : 앱 전반에서 "작업 결과"를 표현하기 위한 공통 결과 타입
// 목적:
// - 함수 실행 결과를 Success / Failure 두 가지 경우로 명확히 구분한다.
// - 예외(Exception)를 직접 던지지 않고, 호출부(UI, ViewModel 등)에서 안전하게 처리할 수 있도록 한다.
// 사용 방식:
// when (result) {
//   is AppResult.Success -> 성공 처리
//   is AppResult.Failure -> error.userMessage를 사용자에게 표시
// }
sealed class AppResult<out T> {

    // 성공 결과를 나타내는 클래스
    // - data: 실제 성공 시 반환할 데이터
    data class Success<T>(val data: T) : AppResult<T>()

    // 실패 결과를 나타내는 클래스
    // - error: 실패 원인을 나타내는 AppError
    // - 모든 실패는 AppError를 통해 표준화
    data class Failure(val error: AppError) : AppResult<Nothing>()
}

// AppError : 앱에서 발생할 수 있는 모든 오류 상황을 정의한 sealed class
// 설계:
// - 오류를 문자열(String)이 아닌 "의미 있는 타입"으로 관리
// - 어떤 종류의 실패인지 코드 레벨에서 명확히 구분 가능
// - 각 오류는 사용자에게 보여줄 메시지(userMessage)를 포함
// UI에서는 error.userMessage만 사용해도 일관된 오류 메시지를 표시할 수 있다.
sealed class AppError(val userMessage: String) {
    // 예외처리) 네트워크 / 시스템 관련 오류
    // 네트워크가 연결되어 있지 않은 경우
    // - AI 분석 요청, 서버 통신 시 사용
    object NetworkUnavailable : AppError("인터넷 연결이 필요해요. 네트워크 상태를 확인해 주세요.")

    // AI(Gemini) API 호출 실패
    // - HTTP 상태 코드(code)를 함께 저장해 디버깅에 활용 가능
    data class ApiError(val code: Int) : AppError("AI 분석 요청에 실패했어요. (코드: $code)")

    // AI 응답을 JSON으로 파싱하지 못한 경우
    // - 응답 포맷이 깨졌거나 예상 구조와 다를 때 발생
    object ParseError : AppError("AI 응답을 해석하는 데 실패했어요. 다시 시도해 주세요.")

    // 갤러리 저장 권한이 없는 경우
    // - Android 런타임 권한 거부 시 사용
    object PermissionDenied : AppError("저장 권한이 거부되어 갤러리에 저장할 수 없어요.")

    // 이미지 저장 중 오류 발생
    // - 저장 공간 부족, MediaStore 실패 등
    object StorageError : AppError("이미지 저장에 실패했어요. 저장 공간/권한을 확인해 주세요.")

    // 요청한 데이터가 존재하지 않는 경우
    // - 삭제된 일기 접근, 잘못된 ID 접근 등
    object NotFound : AppError("요청한 데이터를 찾을 수 없어요.")


    // 예외처리) 인증 / 입력값 검증 관련 오류
    // 이메일이 입력되지 않은 경우
    object EmptyEmail : AppError("이메일을 입력해 주세요.")

    // 비밀번호가 입력되지 않은 경우
    object EmptyPassword : AppError("비밀번호를 입력해 주세요.")

    // 비밀번호 확인이 입력되지 않은 경우
    object EmptyPasswordConfirm : AppError("비밀번호 확인을 입력해 주세요.")

    // 닉네임이 입력되지 않은 경우
    object EmptyNickname : AppError("닉네임을 입력해 주세요.")

    // 이메일 형식이 올바르지 않은 경우
    // - Android Patterns.EMAIL_ADDRESS 기반 검증 실패
    object InvalidEmail : AppError("이메일 형식이 올바르지 않아요.")

    // 이미 사용 중인 이메일로 회원가입 시도
    object EmailAlreadyUsed : AppError("이미 사용 중인 이메일이에요.")

    // 비밀번호와 비밀번호 확인 값이 다른 경우
    object PasswordMismatch : AppError("비밀번호와 비밀번호 확인이 일치하지 않아요.")

    // 비밀번호 정책을 만족하지 못한 경우
    // - 10~15자
    // - 영문 + 숫자 반드시 포함
    object WeakPassword : AppError("비밀번호는 10~15자 사이의 영문+숫자를 포함해야 해요.")

    // 로그인 실패
    // - 이메일이 없거나
    // - 비밀번호 해시 검증 실패 시 사용
    object AuthFailed : AppError("이메일 또는 비밀번호가 올바르지 않아요.")

    // 예외처리) 기타 / 알 수 없는 오류
    // 위에서 정의되지 않은 예외 상황
    // - Exception 메시지를 detail로 받아 디버깅에 활용
    data class Unknown(val detail: String) : AppError("오류가 발생했어요: $detail")
}
