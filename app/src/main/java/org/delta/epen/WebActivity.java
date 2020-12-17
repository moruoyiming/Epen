package org.delta.epen;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.hero.webview.BaseWebFragment;
import com.hero.webview.CommandCallBack;
import com.hero.webview.WebViewFragment;
import com.hero.webview.command.Command;
import com.hero.webview.command.CommandsManager;
import com.hero.webview.utils.WebConstants;
import com.tstudy.blepenlib.BlePenStreamManager;
import com.tstudy.blepenlib.callback.BlePenStreamCallback;
import com.tstudy.blepenlib.data.BleDevice;
import com.tstudy.blepenlib.data.CoordinateInfo;
import com.tstudy.blepenlib.utils.SharedPreferencesUtil;

import org.delta.epen.databinding.ActivityCommonWeb2Binding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
    private static final String TAG = "Battery";
    private String title;
    private String url;
    private boolean showBar;
    private ActivityCommonWeb2Binding binding;
    private BaseWebFragment webviewFragment;

    public static final String KEY_DATA = "DEVICE_DATA";
    public static final String KEY_MODE = "DEVICE_MODE";
    private BleDevice bleDevice;
    private String mBleDeviceName;
    private BlePenStreamCallback mBlePenStreamCallback;
    private boolean isConnectedNow;
    private String writeString;
    private final int MAG_SCAN = 1;
    private MyHandle mHandle;

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
        CommandsManager.getInstance().registerCommand(titleUpdateCommand);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_common_web2);
        mHandle = new MyHandle();
        initData();
        initListener();
        initBle();
        startTime();
    }

    public void CallJsMethod(String cmd, Object params) {
        webviewFragment.CallJsMethod(cmd, params);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webviewFragment != null && webviewFragment instanceof BaseWebFragment) {
            boolean flag = webviewFragment.onKeyDown(keyCode, event);
            if (flag) {
                return flag;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initBle() {
        if (BlePenStreamManager.getInstance().isConnected(bleDevice)) {
            isConnectedNow = true;
            //开启笔输出流
            BlePenStreamManager.getInstance().openPenStream(bleDevice, mBlePenStreamCallback);
            mHandle.removeMessages(MAG_SCAN);
        }
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            bleDevice = intent.getParcelableExtra(KEY_DATA);
            int mode = Integer.parseInt(SharedPreferencesUtil.getInstance(WebActivity.this).getSP("mode"));
            mBleDeviceName = bleDevice.getName();
            Log.d(TAG, "initData: bleDevice: " + mBleDeviceName);
            title = intent.getStringExtra(WebConstants.INTENT_TAG_TITLE);
            url = intent.getStringExtra(WebConstants.INTENT_TAG_URL);
            showBar = intent.getBooleanExtra(WebConstants.INTENT_TAG_IS_SHOW_ACTION_BAR, false);
            binding.actionBars.setVisibility(showBar ? View.VISIBLE : View.GONE);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();
            webviewFragment = null;
            webviewFragment = WebViewFragment.newInstance(url, (HashMap<String, String>) getIntent().getExtras().getSerializable(WebConstants.INTENT_TAG_HEADERS), true);
            transaction.replace(com.hero.webview.R.id.web_view_fragment, webviewFragment).commit();
            setTitle(title);
        } else {
            Log.d(TAG, "initData: intent null  bleDevice null");
        }

    }

    private void initListener() {
        mBlePenStreamCallback = new BlePenStreamCallback() {
            @Override
            public void onOpenPenStreamStatus(boolean openSuccess, String message) {
                if (openSuccess) {
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
                        HashMap<String,String> hashMap=new HashMap<>();
                        hashMap.put("callbackname","onBattery");
                        hashMap.put("batteryPercent",String.valueOf(batteryPercent));
                        hashMap.put("memoryPercent",String.valueOf(memoryPercent));
                        CallJsMethod("onBattery",hashMap);
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

                        HashMap<String,String> hashMap=new HashMap<>();
                        hashMap.put("callbackname","onDraw");
                        hashMap.put("state",String.valueOf(coordinateInfo.state));
                        hashMap.put("pageAddress",String.valueOf(coordinateInfo.pageAddress));
                        hashMap.put("coordX",String.valueOf(coordinateInfo.coordX));
                        hashMap.put("coordY",String.valueOf(coordinateInfo.coordY));
                        hashMap.put("force",String.valueOf(coordinateInfo.coordForce));
                        hashMap.put("timeLong",String.valueOf(coordinateInfo.timeLong));
                        hashMap.put("stroke",String.valueOf(coordinateInfo.strokeNum));
                        CallJsMethod("onDraw",hashMap);
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!TextUtils.isEmpty(softVersion)) {
                            Toast.makeText(WebActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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
                Toast.makeText(WebActivity.this, formatTime, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onCurrentTime: " + formatTime);
            }
        };


    }

    /**
     * 页面路由
     */
    private final Command titleUpdateCommand = new Command() {
        @Override
        public String name() {
            return WebConstants.COMMAND_UPDATE_TITLE;
        }

        @Override
        public void exec(Context context, Map params, CommandCallBack resultBack) {
            if (params.containsKey(WebConstants.COMMAND_UPDATE_TITLE_PARAMS_TITLE)) {
                setTitle((String) params.get(WebConstants.COMMAND_UPDATE_TITLE_PARAMS_TITLE));
            }
        }
    };

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BlePenStreamManager.getInstance().disconnect(bleDevice);
        closeTimer();
    }

    private Disposable mDisposable;

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
                        BlePenStreamManager.getInstance().getPenInfo();
//                        HashMap<String,String> hashMap=new HashMap<>();
//                        hashMap.put("callbackname","onBattery");
//                        hashMap.put("batteryPercent",String.valueOf(20));
//                        hashMap.put("memoryPercent",String.valueOf(30));
//                        CallJsMethod("onBattery",hashMap);
//                        DrawInfo coordinateInfo = new DrawInfo(2,"xdsd",
//                                21,21,12,12,21,false,32,3);
//                        coordinateInfo.setCallbackname("onDraw");
//                        CallJsMethod("onDraw",coordinateInfo);
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
}