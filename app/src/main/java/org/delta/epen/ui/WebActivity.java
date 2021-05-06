package org.delta.epen.ui;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.hero.permission.annotation.Permission;
import com.hero.permission.annotation.PermissionCancel;
import com.hero.permission.annotation.PermissionDenied;
import com.hero.webview.BaseWebFragment;
import com.hero.webview.CommandCallBack;
import com.hero.webview.WebViewFragment;
import com.hero.webview.command.Command;
import com.hero.webview.command.CommandsManager;
import com.hero.webview.utils.WebConstants;
import com.tstudy.blepenlib.BlePenStreamManager;
import com.tstudy.blepenlib.callback.BleGattCallback;
import com.tstudy.blepenlib.callback.BlePenStreamCallback;
import com.tstudy.blepenlib.callback.BleScanCallback;
import com.tstudy.blepenlib.data.BleDevice;
import com.tstudy.blepenlib.data.CoordinateInfo;
import com.tstudy.blepenlib.exception.BleException;

import org.delta.epen.MyLicense;
import org.delta.epen.R;
import org.delta.epen.adapter.DeviceAdapter;
import org.delta.epen.databinding.ActivityCommonWeb2Binding;
import org.delta.epen.listenner.BatteryStateListener;
import org.delta.epen.listenner.BleStateListener;
import org.delta.epen.receiver.BatteryReceiver;
import org.delta.epen.listenner.NetWorkStateListener;
import org.delta.epen.receiver.BleReceiver;
import org.delta.epen.utils.AndroidBug5497Workaround;
import org.delta.epen.receiver.NetWorkStateReceiver;
import org.delta.epen.utils.ViewUtil;
import org.delta.epen.view.BleDialog;
import org.delta.epen.view.SettingDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static com.tstudy.blepenlib.constant.Constant.PEN_COODINAT_MESSAGE;
import static com.tstudy.blepenlib.constant.Constant.PEN_DOWN_MESSAGE;
import static com.tstudy.blepenlib.constant.Constant.PEN_UP_MESSAGE;
import static com.tstudy.blepenlib.constant.Constant.WARN_BATTERY;
import static com.tstudy.blepenlib.constant.Constant.WARN_MEMORY;

/**
 * <pre>
 *     author: jian
 *     Date  : 2020/5/25 2:13 PM
 *     Description:
 * </pre>
 */
public class WebActivity extends AppCompatActivity {
    private static final String TAG = "WebActivity";
    private String url;
    private ActivityCommonWeb2Binding binding;
    private BaseWebFragment webviewFragment;
    private HashMap<String, String> hashMap = new HashMap<>();
    public static final String KEY_DATA = "DEVICE_DATA";
    public static final String KEY_MODE = "DEVICE_MODE";
    private BleDevice mBleDevice;
    private String mBleDeviceName;
    private boolean isConnectedNow;
    private String writeString;
    private final int MAG_SCAN = 1;
    private MyHandle mHandle;
    private BleDialog.onConnectedListener onConnectedListener;
    private SettingDialog.onSetCallBack setOnSetCallBack;
    private BlePenStreamCallback mBlePenStreamCallback;
    private Disposable mDisposable;
    private NetWorkStateListener netWorkStateListener;
    private BatteryStateListener batteryStateListener;
    private BleStateListener bleStateListener;
    private NetWorkStateReceiver wifiStateReceiver;
    private BatteryReceiver batteryReceiver;
    private BleReceiver bleReceiver;
    private int mLevelPercent = 0;
    private int mNetworkRssi = 0;
    private boolean mWifiNetworkState = false;
    private boolean mDataNetworkState = false;
    private boolean mBleState = false;
    private BluetoothAdapter mBluetoothAdapter;

    public static void startCommonWeb(Context context, BleDevice bleDevice, String title, String url) {
        Intent intent = new Intent(context, WebActivity.class);
        intent.putExtra(DrawActivity.KEY_DATA, bleDevice);
        intent.putExtra(WebConstants.INTENT_TAG_TITLE, title);
        intent.putExtra(WebConstants.INTENT_TAG_URL, url);
        if (context instanceof Service) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        //定义全屏参数
//        int flag=WindowManager.LayoutParams.FLAG_FULLSCREEN;
////        设置当前窗体为全屏显示
//        getWindow().setFlags(flag, flag);
        //让虚拟键盘一直不显示
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;
        window.setAttributes(params);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        CommandsManager.getInstance().registerCommand(checkBle);
        CommandsManager.getInstance().registerCommand(battery);
        CommandsManager.getInstance().registerCommand(networkState);
        CommandsManager.getInstance().registerCommand(networkRssi);
        CommandsManager.getInstance().registerCommand(bleState);
        CommandsManager.getInstance().registerCommand(bleOperation);
        CommandsManager.getInstance().registerCommand(screenSize);
        mBleState=mBluetoothAdapter.isEnabled();
        System.out.println("mBleState "+mBleState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_common_web2);
        mHandle = new MyHandle();
        initData();
        initListener();
        openPenStream();
        startTime();
        initReceiver();
        AndroidBug5497Workaround.assistActivity(this);
    }

    public void initReceiver() {
        netWorkStateListener = new NetWorkStateListener() {
            @Override
            public void netWorkChange(boolean wifiNetworkState, boolean dataNetworkState) {
                Log.d(TAG, "WebActivity netWorkChange" + wifiNetworkState + dataNetworkState);
                mWifiNetworkState = wifiNetworkState;
                mDataNetworkState = dataNetworkState;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_NETWORK_STATE);
                        hashMap.put("wifiNetworkState", String.valueOf(mWifiNetworkState));
                        hashMap.put("dataNetworkState", String.valueOf(mDataNetworkState));
                        CallJsMethod(WebConstants.ON_NETWORK_STATE, hashMap);
                    }
                });
            }

            @Override
            public void netStrength(int strength) {
                Log.d(TAG, "WebActivity netStrength=" + strength);
                mNetworkRssi = strength;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_NETWORK_RSSI);
                        hashMap.put("mNetworkRssi", String.valueOf(mNetworkRssi));
                        CallJsMethod(WebConstants.ON_NETWORK_RSSI, hashMap);
                    }
                });
            }
        };
        wifiStateReceiver = new NetWorkStateReceiver(netWorkStateListener);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, intentFilter);
        batteryStateListener = new BatteryStateListener() {
            @Override
            public void levelPercent(int levelPercent) {
                Log.d(TAG, "WebActivity levelPercent=" + levelPercent);
                mLevelPercent = levelPercent;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.MOBILE_BATTERY);
                        hashMap.put("levelPercent", String.valueOf(mLevelPercent));
                        CallJsMethod(WebConstants.ON_MOBILE_BATTERY, hashMap);
                    }
                });
            }
        };

        batteryReceiver = new BatteryReceiver(batteryStateListener);
        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, batteryFilter);

        bleStateListener = new BleStateListener() {
            @Override
            public void onBleStateChange(boolean code) {
                mBleState = code;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_BLE_STATE);
                        hashMap.put("bleState", String.valueOf(mBleState));
                        CallJsMethod(WebConstants.ON_BLE_STATE, hashMap);
                    }
                });
            }
        };
        bleReceiver = new BleReceiver(bleStateListener);
        IntentFilter bleFilter = new IntentFilter();
        bleFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bleReceiver, bleFilter);
    }

    public void unReceiver() {
        unregisterReceiver(wifiStateReceiver);
        unregisterReceiver(batteryReceiver);
        unregisterReceiver(bleReceiver);
        netWorkStateListener = null;
        batteryStateListener = null;
        bleStateListener = null;
        wifiStateReceiver = null;
        batteryReceiver = null;
        bleReceiver = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideNavigation();//菅书舟
    }

    private void initData() {
//        url = "http://14.18.63.234:9024";
        url = "http://192.168.2.146:8080";
//        url = "file:///android_asset/demo.html";
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        webviewFragment = null;//(HashMap<String, String>) intent.getExtras().getSerializable(WebConstants.INTENT_TAG_HEADERS)
        webviewFragment = WebViewFragment.newInstance(url, null, true);
        transaction.replace(com.hero.webview.R.id.web_view_fragment, webviewFragment).commit();
        if (mBleDevice != null) {
            mBleDeviceName = mBleDevice.getName();
            Log.d(TAG, "initData: bleDevice: " + mBleDeviceName);
        } else {
            initBlePen();
        }
    }


    private void initBlePen() {
        boolean initSuccess = BlePenStreamManager.getInstance().init(getApplication(), MyLicense.getBytes());
        if (!initSuccess) {
            Toast.makeText(this, "初始化失败，请到开放平台申请授权或检查设备是否支持蓝牙BLE", Toast.LENGTH_LONG).show();
        }
        //lib 日志开关 true 打开  默认false
        BlePenStreamManager.getInstance().enableLog(true);
    }

    private void checkBle(int status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_INIT_BLE);
                hashMap.put("status", String.valueOf(status));
                CallJsMethod(WebConstants.ON_INIT_BLE, hashMap);
            }
        });
    }

    private void openPenStream() {
        if (BlePenStreamManager.getInstance().isConnected(mBleDevice)) {
            isConnectedNow = true;
            BlePenStreamManager.getInstance().openPenStream(mBleDevice, mBlePenStreamCallback);
            mHandle.removeMessages(MAG_SCAN);
        }
    }

    class MyHandle extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    break;
                case MAG_SCAN:
                    if (!isConnectedNow) {
                        mHandle.sendEmptyMessageDelayed(MAG_SCAN, 15 * 1000);
                    }
                    break;
                default:

            }
        }
    }


    public void showRecordDialog() {
        BleDialog editNameDialog = new BleDialog();
        editNameDialog.setOnConnectedListener(onConnectedListener);
        editNameDialog.setCancelable(false);
        editNameDialog.show(getSupportFragmentManager(), "EditNameDialog");
    }

    public void showSettingDialog() {
        SettingDialog settingDialog = new SettingDialog();
        settingDialog.setOnSetCallBack(setOnSetCallBack);
        settingDialog.setCancelable(false);
        settingDialog.show(getSupportFragmentManager(), "settingDialog");
    }

    public void CallJsMethod(String cmd, Object params) {
        webviewFragment.CallJsMethod(cmd, params);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webviewFragment != null) {
            boolean flag = webviewFragment.onKeyDown(keyCode, event);
            if (flag) {
                return true;
            }
        }
        return true;
    }

    private Command checkBle = new Command() {
        @Override
        public String name() {
            return WebConstants.INIT_BLE;
        }

        @Override
        public void exec(Context context, Map params, CommandCallBack callBack) {
            if (!BlePenStreamManager.getInstance().isConnected(mBleDevice)) {
                checkBle(WebConstants.BLE_STATUS_NORMAL);
                requestPermission();
            } else {
                checkBle(WebConstants.BLE_STATUS_CONNECTED);
            }
        }
    };

    private Command battery = new Command() {
        @Override
        public String name() {
            return WebConstants.MOBILE_BATTERY;
        }

        @Override
        public void exec(Context context, Map params, CommandCallBack callBack) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_MOBILE_BATTERY);
                    hashMap.put("levelPercent", String.valueOf(mLevelPercent));
                    CallJsMethod(WebConstants.ON_MOBILE_BATTERY, hashMap);
                }
            });
        }
    };

    private Command networkRssi = new Command() {
        @Override
        public String name() {
            return WebConstants.NETWORK_RSSI;
        }

        @Override
        public void exec(Context context, Map params, CommandCallBack callBack) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_NETWORK_RSSI);
                    hashMap.put("mNetworkRssi", String.valueOf(mNetworkRssi));
                    CallJsMethod(WebConstants.ON_NETWORK_RSSI, hashMap);
                }
            });
        }
    };

    private Command networkState = new Command() {
        @Override
        public String name() {
            return WebConstants.NETWORK_STATE;
        }

        @Override
        public void exec(Context context, Map params, CommandCallBack callBack) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_NETWORK_STATE);
                    hashMap.put("wifiNetworkState", String.valueOf(mWifiNetworkState));
                    hashMap.put("dataNetworkState", String.valueOf(mDataNetworkState));
                    CallJsMethod(WebConstants.ON_NETWORK_STATE, hashMap);
                }
            });
        }
    };

    private Command bleState = new Command() {
        @Override
        public String name() {
            return WebConstants.BLE_STATE;
        }

        @Override
        public void exec(Context context, Map params, CommandCallBack callBack) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_BLE_STATE);
                    hashMap.put("bleState", String.valueOf(mBleState));
                    CallJsMethod(WebConstants.ON_BLE_STATE, hashMap);
                }
            });
        }
    };

    private Command bleOperation = new Command() {
        @Override
        public String name() {
            return WebConstants.BLE_OPERATION;
        }

        @Override //0 = 打开， 1= 关闭 2 = 开始搜索蓝牙  3 = 取消搜索蓝牙  4 = 连接蓝牙  5 = 断开蓝牙
        public void exec(Context context, Map params, CommandCallBack callBack) {
            String what = (String) params.get("operation");
            Log.i(TAG, "  what " + what);
            switch (what) {
                case "0":
                    requestPermission();
                    break;
                case "1":
                    if (mBluetoothAdapter != null) {
                        mBluetoothAdapter.disable();
                        Toast.makeText(WebActivity.this, "蓝牙关闭", Toast.LENGTH_LONG).show();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                HashMap<String, String> hashMap = new HashMap<>();
                                hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_BLE_OPERATION);
                                hashMap.put("operation", String.valueOf(7));
                                CallJsMethod(WebConstants.ON_BLE_OPERATION, hashMap);
                            }
                        });
                    }
                    break;
                case "2":
                    startScan();
                    break;
                case "3":
                    BlePenStreamManager.getInstance().cancelScan();
                    break;
                case "4":
                    String conMac = (String) params.get("mac");
                    connect(conMac);
                    break;
                case "5":
                    String disMac = (String) params.get("mac");
                    BlePenStreamManager.getInstance().disconnect(disMac);
                    break;

            }
        }
    };

    private Command screenSize = new Command() {

        @Override
        public String name() {
            return WebConstants.SCREEN_SIZE;
        }

        @Override
        public void exec(Context context, Map params, CommandCallBack callBack) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_SCREEN_SIZE);
                    int width = ViewUtil.getScreenWidth(WebActivity.this);
                    int height = ViewUtil.getScreenHeight(WebActivity.this);
                    hashMap.put("width", String.valueOf(width));
                    hashMap.put("height", String.valueOf(height));
                    CallJsMethod(WebConstants.ON_SCREEN_SIZE, hashMap);
                }
            });
        }
    };


    private void initListener() {
        mBlePenStreamCallback = new BlePenStreamCallback() {
            @Override
            public void onOpenPenStreamStatus(boolean openSuccess, String message) {
                if (openSuccess) {
                    checkBle(WebConstants.BLE_STATUS_OPEN_STREAM);
                    BlePenStreamManager.getInstance().setStandMode();
                    Log.d(TAG, "onOpenPenStreamSuccess: ");
                } else {
                    Log.d(TAG, "onOpenPenStreamFailure: " + message);
                }
            }

            @Override
            public void onRemainBatteryAndMemory(final int batteryPercent, final int memoryPercent, final int byteNum) {
                Log.d(TAG, "每隔五分钟调用一次  onRemainBatteryAndMemory: " + batteryPercent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_BATTERY);
                        hashMap.put("batteryPercent", String.valueOf(batteryPercent));
                        hashMap.put("memoryPercent", String.valueOf(memoryPercent));
                        CallJsMethod(WebConstants.ON_BATTERY, hashMap);
                    }
                });
            }

            @Override
            public void onCoordDraw(final CoordinateInfo coordinateInfo) {
                //TODO 绘制图像
                /**
                 * 输出离线数据坐标信息
                 * @param state down/move/up
                 * @param pageAddress 点阵地址
                 * @param nX 坐标X
                 * @param nY 坐标Y
                 * @param nForce 压力值
                 * @param strokeNum 笔画数
                 * @param time 时间戳
                 * @param offLineDataAllSize 总离线字节数
                 * @param offLineDateCurrentSize 已上传字节数
                 */

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //其中笔的状态为move时坐标携带的坐标信息（点阵地址、坐标、压力）有效，
                        // down、up时坐标信息（点阵地址、坐标、压力）无效，绘制一般只取落笔抬笔事件用于重置绘制状态
                        switch (coordinateInfo.state) {
                            case PEN_DOWN_MESSAGE:
                                writeString = "down";
                                break;
                            case PEN_COODINAT_MESSAGE:
                                writeString = "move";
                                break;
                            case PEN_UP_MESSAGE:
                                writeString = " up ";
                                break;
                            default:
                                writeString = " up ";
                        }

                        Log.d("onCoordDrawMessage_tag", "onCoordDraw: x=" + coordinateInfo.coordX + "  y=" + coordinateInfo.coordY + "  force=" + coordinateInfo.coordForce +
                                "  pageAddress=" + coordinateInfo.pageAddress + "  time=" + coordinateInfo.timeLong + "  stroke=" + coordinateInfo.strokeNum + "  state=" + writeString);
                        if (hashMap != null) {
                            hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_DRAW);
                            hashMap.put("state", String.valueOf(coordinateInfo.state));
                            hashMap.put("pageAddress", String.valueOf(coordinateInfo.pageAddress));
                            hashMap.put("coordX", String.valueOf(coordinateInfo.coordX));
                            hashMap.put("coordY", String.valueOf(coordinateInfo.coordY));
                            hashMap.put("force", String.valueOf(coordinateInfo.coordForce));
                            hashMap.put("isOFFLine", String.valueOf(coordinateInfo.isOFFLine));
                            hashMap.put("timeLong", String.valueOf(coordinateInfo.timeLong));
                            hashMap.put("stroke", String.valueOf(coordinateInfo.strokeNum));
                            CallJsMethod(WebConstants.ON_DRAW, hashMap);
                        }
                    }
                });

            }

            @Override
            public void onWarnActiveReport(final int statusNum) {
                //0x05  电池电量低警告  0x08 存储空间警告
                switch (statusNum) {
                    case WARN_BATTERY:
                        Log.d(TAG, "handleActiveReport: 电池电量低警告");
                        break;
                    case WARN_MEMORY:
                        Log.d(TAG, "handleActiveReport: 存储空间警告");
                        break;
                    default:

                }
            }

            @Override
            public void onVersionAndserialNumber(String hardVersion, final String softVersion, String serialNumber) {
                final String msg = "hardVersion：" + hardVersion + "  softVersion:" + softVersion + "   serialNumber:" + serialNumber;
                Log.d(TAG, "onVersionAndserialNumber: " + msg);
            }


            @Override
            public void onCurrentTime(long penTime) {
                //如果与当前系统时间差一分钟以上，可以同步笔端时间
                long timeMillis = System.currentTimeMillis();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS", Locale.CHINA);
                if (Math.abs(penTime - timeMillis) > 1000 * 60) {
//                    BlePenStreamManager.getInstance().setPenRTC(timeMillis);
                }
                String formatTime = simpleDateFormat.format(new Date(penTime));
                Log.d(TAG, "onCurrentTime: " + formatTime);
            }
        };

        onConnectedListener = new BleDialog.onConnectedListener() {
            @Override
            public void onConnected(BleDevice ble) {
                Toast.makeText(WebActivity.this, R.string.connected, Toast.LENGTH_LONG).show();
                Log.d(TAG, "onConnected: " + ble);
                mBleDevice = ble;
                mBleDeviceName = mBleDevice.getName();
                checkBle(WebConstants.BLE_STATUS_CONNECTED);
                openPenStream();
            }

            @Override
            public void onDisConnected() {
                Toast.makeText(WebActivity.this, R.string.active_disconnected, Toast.LENGTH_LONG).show();
                Log.d(TAG, "onDisConnected: ");
                mBleDevice = null;
                mBleDeviceName = null;
                checkBle(WebConstants.BLE_STATUS_DISCONNECTED);
                showRecordDialog();
            }
        };

        setOnSetCallBack = new SettingDialog.onSetCallBack() {
            @Override
            public void onResult() {
                requestPermission();
            }
        };
    }

    /**
     * 启动定时器
     */
    public void startTime() {
        Observable.interval(1, TimeUnit.MINUTES)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(Long value) {
                        if (BlePenStreamManager.getInstance().isConnected(mBleDevice)) {
                            BlePenStreamManager.getInstance().getPenInfo();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        closeTimer();
                    }
                });
    }

    /**
     * 关闭定时器
     */
    public void closeTimer() {
        if (mDisposable != null) {
            mDisposable.dispose();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBleDevice != null) {
            boolean isConnected = BlePenStreamManager.getInstance().isConnected(mBleDevice);
            if (isConnected) {
                BlePenStreamManager.getInstance().disconnect(mBleDevice);
            }
        }
        unReceiver();
        closeTimer();
    }

    /**
     * 这里写的要特别注意，denied方法，必须是带有一个int参数的方法，下面的也一样
     *
     * @param requestCode
     */
    @PermissionCancel
    public void denied(int requestCode) {
        Log.e(TAG, "权限请求拒绝");
    }

    @PermissionDenied
    public void deniedForever(int requestCode) {
        Log.e(TAG, "权限请求拒绝，用户永久拒绝");
        showSettingDialog();
    }

    @Permission(permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, requestCode = 1)
    public void requestPermission() {
        Log.i("Permission", "权限请求成功");
//        showRecordDialog();
        if (mBluetoothAdapter == null) {
            Toast.makeText(WebActivity.this, "检查设备是否支持蓝牙BLE", Toast.LENGTH_LONG).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(WebActivity.this, "正在开启蓝牙", Toast.LENGTH_LONG).show();
            boolean ble = mBluetoothAdapter.enable();
            if (ble) {
                Toast.makeText(WebActivity.this, "蓝牙开启成功", Toast.LENGTH_LONG).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_BLE_OPERATION);
                        hashMap.put("operation", String.valueOf(6));
                        CallJsMethod(WebConstants.ON_BLE_OPERATION, hashMap);
                    }
                });
            } else {
                Toast.makeText(WebActivity.this, "蓝牙开启失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void hideNavigation() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    List<HashMap<String, Object>> what = new ArrayList<>();


    private void startScan() {
        //扫描回调
        BleScanCallback callback = new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                Log.d(TAG, "onScanStarted: " + success);
                what.clear();
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                Log.d(TAG, "onScanning: " + bleDevice);
                HashMap<String, Object> params = new HashMap<>();
                params.put("name", bleDevice.getName());
                params.put("mac", bleDevice.getMac());
                params.put("rssi", bleDevice.getRssi());
                what.add(params);
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                Log.i(TAG, what.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_BLE_OPERATION);
                        hashMap.put("operation", String.valueOf(8));
                        hashMap.put("list", what.toString());
                        CallJsMethod(WebConstants.ON_BLE_OPERATION, hashMap);
                    }
                });
            }
        };
        BlePenStreamManager.getInstance().scan(callback);
    }

    private void connect(String mac) {
        //连接回调
        BleGattCallback bleGattCallback = new BleGattCallback() {
            @Override
            public void onStartConnect() {
                Log.d(TAG, "onStartConnect: ");
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                Log.d(TAG, "onConnectFail: ");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_BLE_OPERATION);
                        hashMap.put("operation", String.valueOf(10));
                        CallJsMethod(WebConstants.ON_BLE_OPERATION, hashMap);
                    }
                });
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                Log.d(TAG, "onConnectSuccess: " + BlePenStreamManager.getInstance().isConnected(bleDevice) + "   " + bleDevice);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_BLE_OPERATION);
                        hashMap.put("operation", String.valueOf(9));
                        CallJsMethod(WebConstants.ON_BLE_OPERATION, hashMap);
                    }
                });
                mBleDevice = bleDevice;
                if (BlePenStreamManager.getInstance().isConnected(mBleDevice)) {
                    Toast.makeText(WebActivity.this, R.string.connected, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "onConnected: " + mBleDevice);
                    mBleDeviceName = mBleDevice.getName();
                    checkBle(WebConstants.BLE_STATUS_CONNECTED);
                    openPenStream();
                }
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                Toast.makeText(WebActivity.this, R.string.active_disconnected, Toast.LENGTH_LONG).show();
                Log.d(TAG, "onDisConnected: ");
                mBleDeviceName = null;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(WebConstants.NATIVE2WEB_CALLBACK, WebConstants.ON_BLE_OPERATION);
                        hashMap.put("operation", String.valueOf(11));
                        CallJsMethod(WebConstants.ON_BLE_OPERATION, hashMap);
                    }
                });
                checkBle(WebConstants.BLE_STATUS_DISCONNECTED);
            }
        };
        BlePenStreamManager.getInstance().connect(mac, bleGattCallback);
    }
}