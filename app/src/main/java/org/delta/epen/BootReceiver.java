package org.delta.epen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("BOOTonReceive","Android 设备重启"+intent.getAction());
        //android.intent.action.BOOT_COMPLETED
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent toIntent = new Intent(context, MainActivity.class);
            toIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(toIntent);
        }
    }
}

