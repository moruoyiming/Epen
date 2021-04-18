package org.delta.epen.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import org.delta.epen.listenner.BatteryStateListener;

public class BatteryReceiver extends BroadcastReceiver {
    private static final String TAG = BatteryReceiver.class.getSimpleName();
    BatteryStateListener batteryStateListener;

    public BatteryReceiver(BatteryStateListener batteryStateListener) {
        this.batteryStateListener = batteryStateListener;
    }

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        int level = arg1.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = arg1.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
        int levelPercent = (int) (((float) level / scale) * 100);
        Log.d(TAG, "电量：" + levelPercent + "%");
        if (batteryStateListener != null) {
            batteryStateListener.levelPercent(levelPercent);
        }
    }

}