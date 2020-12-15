package com.rwls.students;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("BOOTonReceive","Android 设备重启"+intent.getAction());
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent toIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            context.startActivity(toIntent);
        }
    }
}