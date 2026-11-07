package com.project.maru

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyApplication : Application() {
    companion object {
        lateinit var auth: FirebaseAuth
            private set
        lateinit var db: FirebaseFirestore
            private set
    }

    override fun onCreate() {
        super.onCreate()
        auth = FirebaseAuth.getInstance()
        auth.useAppLanguage()
        db = FirebaseFirestore.getInstance()
    }
}