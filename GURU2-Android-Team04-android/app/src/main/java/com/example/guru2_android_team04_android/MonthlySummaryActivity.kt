package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.MonthlySummaryUiBinder
import com.example.guru2_android_team04_android.util.DateUtil

class MonthlySummaryActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }
    private lateinit var binder: MonthlySummaryUiBinder

    private var yearMonth: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_summary)

        yearMonth = intent.getStringExtra("yearMonth") ?: DateUtil.previousMonthYm()

        binder = MonthlySummaryUiBinder(this, appService)
        binder.bind(yearMonth)
    }
}
