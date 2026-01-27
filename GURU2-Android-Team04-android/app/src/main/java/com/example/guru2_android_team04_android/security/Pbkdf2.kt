package com.example.guru2_android_team04_android.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Pbkdf2 : 비밀번호를 안전하게 해시하고 검증하기 위한 보안 유틸리티
// 용도:
// - 회원 가입 시 비밀번호를 평문으로 저장하지 않고, PBKDF2 해시로 변환하여 DB에 저장한다.
// - 로그인 시 입력된 비밀번호를 동일한 방식으로 해시하여 저장된 값과 비교한다.
// 설계:
// - 단순 해시(SHA-256 등)는 무차별 대입 공격에 취약
// - PBKDF2는 반복 연산과 salt를 사용해 공격 비용을 크게 높임
object Pbkdf2 {

    // 해시 반복 횟수
    private const val ITER = 120_000

    // 생성할 키 길이(bit 단위)
    private const val KEY_LEN = 256

    // 비밀번호를 해시 문자열로 변환한다.
    // 입력:
    // - password: 사용자가 입력한 비밀번호(CharArray)
    // 반환:
    // - "pbkdf2$ITER$SALT$KEY" 형식의 문자열
    fun hash(password: CharArray): String {

        // salt 생성
        // - 사용자마다 다른 랜덤 salt를 사용해 동일 비밀번호라도 해시 결과가 달라지게 한다.
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        // PBKDF2로 비밀번호 → 키(ByteArray) 생성
        val key = pbkdf2(password, salt, ITER, KEY_LEN)

        // $는 문자열 구분자로 사용되므로 Kotlin 문자열에서 \$로 escape 해야 한다.
        // 포맷: pbkdf2$ITER$SALT$KEY  (총 4파트)
        // - SALT와 KEY는 Base64로 인코딩하여 문자열로 저장한다.
        return "pbkdf2\$${ITER}\$${b64(salt)}\$${b64(key)}"
    }

    // 입력된 비밀번호가 저장된 해시 문자열과 일치하는지 검증한다.
    // 입력:
    // - password: 로그인 시 사용자가 입력한 비밀번호
    // - stored: DB에 저장된 해시 문자열
    // 반환:
    // - true: 비밀번호 일치
    // - false: 비밀번호 불일치 또는 형식 오류
    fun verify(password: CharArray, stored: String): Boolean {

        // "$" 기준으로 분리
        // 기대 포맷:
        // ["pbkdf2", "120000", "<saltBase64>", "<keyBase64>"]
        val parts = stored.split('$')

        // 예외처리) 포맷이 다르면 즉시 실패
        if (parts.size != 4) return false
        if (parts[0] != "pbkdf2") return false

        // iteration 값 파싱
        val iter = parts[1].toIntOrNull() ?: return false

        // Base64 디코딩으로 salt / 기대 key 복원
        val salt = b64d(parts[2])
        val expected = b64d(parts[3])

        // 동일한 조건(iter, salt)으로 입력 비밀번호를 다시 해시
        val actual = pbkdf2(password, salt, iter, expected.size * 8)

        // 예외처리) 타이밍 공격을 방지하기 위해 constant-time 비교 사용
        return constantTimeEquals(expected, actual)
    }

    // PBKDF2 핵심 연산 함수
    // 입력:
    // - pw: 비밀번호(CharArray)
    // - salt: 랜덤 salt
    // - iter: 반복 횟수
    // - keyLenBits: 생성할 키 길이(bit)
    // 반환:
    // - 생성된 키(ByteArray)
    private fun pbkdf2(
        pw: CharArray, salt: ByteArray, iter: Int, keyLenBits: Int
    ): ByteArray {

        // PBEKeySpec: 비밀번호 기반 키 생성 스펙
        val spec = PBEKeySpec(pw, salt, iter, keyLenBits)

        // PBKDF2WithHmacSHA256 알고리즘 사용
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

        return skf.generateSecret(spec).encoded
    }

    // ByteArray -> Base64 문자열
    // - DB 저장 및 문자열 처리 편의성을 위해 사용
    private fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)

    // Base64 문자열 -> ByteArray
    private fun b64d(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    // constant-time byte array 비교
    // 목적:
    // - 일반 equals 비교는 중간에 다른 값이 나오면 바로 종료되어
    //   타이밍 공격(side-channel attack)에 취약할 수 있다.
    // - 모든 바이트를 끝까지 비교하여 실행 시간을 일정하게 유지한다.
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {

        // 예외처리) 길이가 다르면 즉시 실패
        if (a.size != b.size) return false

        var r = 0
        for (i in a.indices) {
            r = r or (a[i].toInt() xor b[i].toInt())
        }
        return r == 0
    }
}
