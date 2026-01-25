package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.ArchiveDiaryDetailUiBinder

class ArchiveDiaryDetailActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }
    private lateinit var binder: ArchiveDiaryDetailUiBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive_diary_detail)

        val entryId = intent.getLongExtra("entryId", -1L)

        binder = ArchiveDiaryDetailUiBinder(this, appService)
        binder.bind(entryId)
    }
}
