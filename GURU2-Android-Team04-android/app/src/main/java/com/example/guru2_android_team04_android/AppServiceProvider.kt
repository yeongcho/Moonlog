package com.example.guru2_android_team04_android

import android.content.Context

// AppServiceProvider: AppService 접근을 쉽게 해주는 확장 프로퍼티
val Context.appService: AppService
    get() = (applicationContext as MyApp).appService
