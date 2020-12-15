package com.tstudy.blependemo;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i("BOOTonReceive","onAccessibilityEvent");
    }

    @Override
    public void onInterrupt() {
        Log.i("BOOTonReceive","onInterrupt");
    }
}