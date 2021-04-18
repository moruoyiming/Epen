package org.delta.epen.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.delta.epen.listenner.BleStateListener;

public class BleReceiver extends BroadcastReceiver {
    private static final String TAG = BleReceiver.class.getSimpleName();
    BleStateListener bleStateListener;

    public BleReceiver(BleStateListener bleStateListener) {
        this.bleStateListener = bleStateListener;
    }

    @Override
    public void onReceive(Context arg0, Intent intent) {
        int action = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        switch (action) {
            case BluetoothAdapter.STATE_OFF:
                Log.d(TAG, "STATE_OFF 手机蓝牙关闭");
                if (bleStateListener != null) {
                    bleStateListener.onBleStateChange(false);
                }
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                Log.d(TAG, "STATE_TURNING_OFF 手机蓝牙正在关闭");
                break;
            case BluetoothAdapter.STATE_ON:
                Log.d(TAG, "STATE_ON 手机蓝牙开启");
                bleStateListener.onBleStateChange(true);
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                Log.d(TAG, "STATE_TURNING_ON 手机蓝牙正在开启");
                break;
        }
    }

}