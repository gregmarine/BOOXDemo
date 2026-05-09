package com.booxdemo

import android.app.Application
import org.lsposed.hiddenapibypass.HiddenApiBypass

class BOOXDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // BOOX SDK uses reflection to call hidden system APIs (VMRuntime, RawInputManager, etc.)
        // Android 14+ blocks VMRuntime.setHiddenApiExemptions, so the SDK can't bootstrap itself.
        // This call bypasses the enforcement at the JNI level before the SDK initializes.
        HiddenApiBypass.addHiddenApiExemptions("")
    }
}
