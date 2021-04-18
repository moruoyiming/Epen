package org.delta.epen.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import org.delta.epen.listenner.NetWorkStateListener;

public class NetWorkStateReceiver extends BroadcastReceiver {
    private static final String TAG = NetWorkStateReceiver.class.getSimpleName();

    private NetWorkStateListener mNetWorkStateListener;

    private boolean wifiState = false;

    private boolean mobileState = false;

    public NetWorkStateReceiver(NetWorkStateListener netWorkStateListener) {
        this.mNetWorkStateListener = netWorkStateListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "NetWorkStateReceiver");
        Log.d(TAG, "NetWorkStateReceiver1" + intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION));
        Log.d(TAG, "NetWorkStateReceiver2" + intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION));
//        Log.d(TAG, "NetWorkStateReceiver3" +intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION));
        if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {
            int strength = getStrength(context);
            Log.d(TAG, "当前信号" + strength);
            if (mNetWorkStateListener != null) {
                mNetWorkStateListener.netStrength(strength);
            }
        } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            Log.d(TAG, "网络状态发生变化");
            //检测API是不是小于23，因为到了API23之后getNetworkInfo(int networkType)方法被弃用
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "API level 小于23");
                //获得ConnectivityManager对象
                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                //获取ConnectivityManager对象对应的NetworkInfo对象
                //获取WIFI连接的信息
                NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                //获取移动数据连接的信息
                NetworkInfo dataNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if (wifiNetworkInfo.isConnected() && dataNetworkInfo.isConnected()) {
                    Toast.makeText(context, "WIFI已连接,移动数据已连接", Toast.LENGTH_SHORT).show();
                } else if (wifiNetworkInfo.isConnected() && !dataNetworkInfo.isConnected()) {
                    Toast.makeText(context, "WIFI已连接,移动数据已断开", Toast.LENGTH_SHORT).show();
                } else if (!wifiNetworkInfo.isConnected() && dataNetworkInfo.isConnected()) {
                    Toast.makeText(context, "WIFI已断开,移动数据已连接", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "WIFI已断开,移动数据已断开", Toast.LENGTH_SHORT).show();
                }
                if (mNetWorkStateListener != null) {
                    mNetWorkStateListener.netWorkChange(wifiNetworkInfo.isConnected(), dataNetworkInfo.isConnected());
                }
                //API大于23时使用下面的方式进行网络监听
            } else {

                Log.d(TAG, "API level 大于23");
                //获得ConnectivityManager对象
                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                //获取所有网络连接的信息
                Network[] networks = connMgr.getAllNetworks();
                //用于存放网络连接信息  "WIFI" or "MOBILE"
                //通过循环将网络信息逐个取出来
                for (int i = 0; i < networks.length; i++) {
                    //获取ConnectivityManager对象对应的NetworkInfo对象
                    NetworkInfo networkInfo = connMgr.getNetworkInfo(networks[i]);
                    Log.d(TAG, networkInfo.getTypeName() + networkInfo.isConnected());
                    if ("WIFI".equals(networkInfo.getTypeName())) {
                        wifiState = networkInfo.isConnected();
                    } else if ("MOBILE".equals(networkInfo.getTypeName())) {
                        mobileState = networkInfo.isConnected();
                    }
                }
                if (mNetWorkStateListener != null) {
                    mNetWorkStateListener.netWorkChange(wifiState, mobileState);
                }
            }
        } else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            //WIFI开关
            int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
            if (wifistate == WifiManager.WIFI_STATE_DISABLED) {//如果关闭
                Log.d(TAG, "网络状态发生变化");
            }
        }


    }

    public int getStrength(Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info.getBSSID() != null) {
            int strength = WifiManager.calculateSignalLevel(info.getRssi(), 5);
            //链接速度
            int speed = info.getLinkSpeed();
            // 链接速度单位
            String units = WifiInfo.LINK_SPEED_UNITS;
            // Wifi源名称
            String ssid = info.getSSID();
            return strength;
        }
        return 0;
    }

}

