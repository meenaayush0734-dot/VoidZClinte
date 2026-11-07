package com.voidx.client

import android.app.Application
import com.voidx.client.data.AppContainer

class VoidXApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
