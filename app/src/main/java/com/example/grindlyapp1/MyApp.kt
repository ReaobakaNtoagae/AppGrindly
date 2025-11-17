package com.example.grindlyapp1

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MyApp", "🔥 MyApp initialized")
        FirebaseApp.initializeApp(this)
    }
}

