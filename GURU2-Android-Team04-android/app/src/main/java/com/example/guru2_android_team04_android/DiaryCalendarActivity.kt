package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.DiaryCalendarUiBinder
import com.example.guru2_android_team04_android.util.DateUtil

class DiaryCalendarActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }
    private lateinit var binder: DiaryCalendarUiBinder

    private var yearMonth: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_calendar)

        yearMonth = intent.getStringExtra("yearMonth") ?: DateUtil.thisMonthYm()

        binder = DiaryCalendarUiBinder(this, appService)
        binder.bind(yearMonth)
    }
}
