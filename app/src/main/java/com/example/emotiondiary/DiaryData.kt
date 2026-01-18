package com.example.emotiondiary

// 어디서든 접근할 수 있는 일기 데이터 저장소 (싱글톤)
object DiaryData {
    var isWritten: Boolean = false       // 일기를 썼는지 여부
    var date: String = "2026년 2월 6일"
    var emotionText: String = "평온"
    var emotionIcon: Int = R.drawable.emotion_normal
    var title: String = ""
    var content: String = ""
}