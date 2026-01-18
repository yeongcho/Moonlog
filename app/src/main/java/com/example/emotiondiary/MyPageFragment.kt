package com.example.emotiondiary // ★ 패키지 이름 확인하세요!

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.emotiondiary.databinding.FragmentMypageBinding

class MyPageFragment : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    // 기본 상태: 비회원 (false)
    private var isLoggedIn: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 시작하자마자 현재 상태(비회원)로 화면 세팅
        updateUI(isLoggedIn)

        // ---------------------------------------------------------
        // [이벤트 리스너]
        // ---------------------------------------------------------

        // (1) "로그인하고..." 글씨 클릭 시 -> ★ 로그인 팝업 띄우기 ★
        binding.tvGuestInfo.setOnClickListener {
            showLoginDialog()
        }

        // (2) 로그아웃 버튼
        binding.btnLogout.setOnClickListener {
            isLoggedIn = false
            updateUI(false) // 비회원 화면으로 복귀
            Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // (3) 회원 탈퇴 버튼
        binding.btnDeleteAccount.setOnClickListener {
            Toast.makeText(context, "회원 탈퇴 기능", Toast.LENGTH_SHORT).show()
        }

        // (4) 프로필 편집 버튼
        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(context, "프로필 편집", Toast.LENGTH_SHORT).show()

            // MyPageFragment.kt 안에 onViewCreated 함수 안쪽

            binding.btnEditProfile.setOnClickListener {
                if (isLoggedIn) {
                    // 로그인 상태면 -> 편집 화면으로 이동!
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, EditProfileFragment()) // fragment_container ID 확인 필요
                        .addToBackStack(null) // 뒤로가기 가능하게 설정
                        .commit()
                } else {
                    Toast.makeText(context, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ★ [핵심 기능] 로그인 팝업창 띄우기
    private fun showLoginDialog() {
        // 1. 아까 만든 dialog_login.xml 모양을 가져옴
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_login, null)
        val etId = dialogView.findViewById<EditText>(R.id.et_login_id)
        val etPw = dialogView.findViewById<EditText>(R.id.et_login_pw)

        // 2. 팝업창 만들기
        val builder = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                // 확인 버튼 눌렀을 때 할 일
                val inputId = etId.text.toString()
                val inputPw = etPw.text.toString()

                // ★ 임시 로그인 검사 (원하는 아이디/비번으로 바꾸세요)
                if (inputId == "swu" && inputPw == "1234") {
                    // 로그인 성공!
                    isLoggedIn = true
                    updateUI(true) // 화면을 회원 모드로 변경
                    Toast.makeText(context, "환영합니다, ${inputId}님!", Toast.LENGTH_SHORT).show()
                } else {
                    // 로그인 실패
                    Toast.makeText(context, "아이디 또는 비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }

        // 3. 팝업창 보여주기
        builder.show()
    }

    // 화면 상태 변경 함수 (아까와 동일)
    private fun updateUI(isLogin: Boolean) {
        if (isLogin) {
            // [로그인 상태] -> 정보 보여주기
            binding.layoutMemberInfo.visibility = View.VISIBLE
            binding.ivBadgeFrameMypage.visibility = View.VISIBLE
            binding.btnLogout.visibility = View.VISIBLE
            binding.btnDeleteAccount.visibility = View.VISIBLE
            binding.tvGuestInfo.visibility = View.GONE

            // (옵션) 로그인한 아이디로 닉네임 변경하고 싶다면:
            // binding.tvNickname.text = "멋진슈니"
        } else {
            // [비회원 상태] -> 정보 숨기기
            binding.layoutMemberInfo.visibility = View.GONE
            binding.ivBadgeFrameMypage.visibility = View.GONE
            binding.btnLogout.visibility = View.GONE
            binding.btnDeleteAccount.visibility = View.GONE
            binding.tvGuestInfo.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}