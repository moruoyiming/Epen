package org.delta.epen;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.tstudy.blepenlib.BlePenStreamManager;
import com.tstudy.blepenlib.callback.BleCheckVersionCallback;
import com.tstudy.blepenlib.callback.BlePenStreamCallback;
import com.tstudy.blepenlib.callback.BleUpdateCallback;
import com.tstudy.blepenlib.data.BleDevice;
import com.tstudy.blepenlib.data.CoordinateInfo;
import com.tstudy.blepenlib.utils.SharedPreferencesUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
 * Created by sunwei on 2018/7/7 0007.
 */
public class DrawActivity extends AppCompatActivity {

    public static final String KEY_DATA = "DEVICE_DATA";
    public static final String KEY_MODE = "DEVICE_MODE";
    private BleDevice bleDevice;
    private static final String TAG = "DrawActivity_tag";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS", Locale.CHINA);
    private int mWidth = 1920;
    private int mHeight = 1080;
    private Object mSyncObject = new Object();
    private TextView txt_coordinate;
    private TextView txt_force;
    private TextView txt_paper_addres;
    private TextView txt_write;
    private String mBleDeviceName;
    private BlePenStreamCallback mBlePenStreamCallback;
    private String writeString;
    private boolean openStandardMode;
    private Button btn_hover_mode;
    private TextView txt_battery;
    private TextView txt_memory;
    private ImageView imgHold;
    private Context mContext;
    private ProgressDialog progressDialog;
    private final int MAG_SCAN = 1;
    private MyHandle mHandle;
    private boolean isConnectedNow;
    private MyProgress myProgress;
    private TextView txt_progress;
    private TextView txt_time;
    private ProgressDialog mProgressDialog;
    private String mUrlData;
    private LinearLayout layout_mode;
    private LinearLayout freeDrawLayout;
    private CanvasFrame.SignatureView mStrokeView;
    private CanvasFrame canvasFrame;
    private LinearLayout layout_update;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        mContext = DrawActivity.this;
        mHandle = new MyHandle();
        File cacheDir = mContext.getCacheDir();
        initView();
        initData();
        initListener();
        initBle();
        startTime();
    }

    private void initView() {
        //绘图背景初始化
        //获取屏幕的宽高
        WindowManager systemService = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (systemService != null) {
            Display dis = systemService.getDefaultDisplay();
            mWidth = dis.getWidth();
            mHeight = dis.getHeight();
        }
        txt_time = findViewById(R.id.txt_time);
        myProgress = findViewById(R.id.pgsBar);
        txt_progress = findViewById(R.id.txt_progress);
        txt_battery = findViewById(R.id.txt_battery);
        txt_memory = findViewById(R.id.txt_memory);
        imgHold = findViewById(R.id.img_hold);
        txt_write = findViewById(R.id.txt_write);
        txt_paper_addres = findViewById(R.id.txt_paper_addres);
        txt_force = findViewById(R.id.txt_force);
        txt_coordinate = findViewById(R.id.txt_coordinate);

        freeDrawLayout = findViewById(R.id.note);

        freeDrawLayout.post(new Runnable() {
            @Override
            public void run() {
                int height = mHeight;
                int width = mWidth;
                if (mHeight > (int) (1.414 * mWidth)) {
                    //手机屏幕较A4缩放高有剩余
                    height = (int) (1.414 * mWidth);
                } else {
                    //手机屏幕较A4缩放宽有剩余
                    width = (int) (mHeight / 1.414);
                }
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
                params.addRule(RelativeLayout.CENTER_IN_PARENT);
                freeDrawLayout.setLayoutParams(params);
                canvasFrame = new CanvasFrame(mContext);
                mStrokeView = canvasFrame.bDrawl;
                Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.a51).copy(Bitmap.Config.ARGB_4444, true);
                canvasFrame.bDrawl.setSignatureBitmap(bmp);

                freeDrawLayout.addView(canvasFrame);
                canvasFrame.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                Log.d(TAG, "onGlobalLayout: ");
                                canvasFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                canvasFrame.setLayout(freeDrawLayout.getWidth(), freeDrawLayout.getHeight());
                                freeDrawLayout.setOnTouchListener(canvasFrame.mTouchListener);
                            }
                        });
            }
        });

        Button btn_close_stream = findViewById(R.id.btn_close_stream);
        btn_close_stream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BlePenStreamManager.getInstance().getPenRTC();
            }
        });
        Button btn_disconnect_ble = findViewById(R.id.btn_disconnect_ble);
        btn_disconnect_ble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS", Locale.CHINA);

                long timeMillis = System.currentTimeMillis();
                BlePenStreamManager.getInstance().setPenRTC(timeMillis);
                String formatTime = simpleDateFormat.format(timeMillis);
                Toast.makeText(mContext, formatTime, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onCurrentTime: " + formatTime);
            }
        });
        Button btn_clear_canvas = findViewById(R.id.btn_clear_canvas);
        //清空画布
        btn_clear_canvas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStrokeView.clear();
            }
        });
        //获取笔端信息
        Button btn_pen_info = findViewById(R.id.btn_pen_info);
        btn_pen_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BlePenStreamManager.getInstance().getPenInfo();
            }
        });

        //清空缓存
        Button btn_delete_memory_cache = findViewById(R.id.btn_delete_memory_cache);
        btn_delete_memory_cache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BlePenStreamManager.getInstance().clearMemoryCache();
            }
        });
        layout_mode = findViewById(R.id.layout_mode);

        //悬浮开关按钮
        btn_hover_mode = findViewById(R.id.btn_standard_hover_mode);
        btn_hover_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (openStandardMode) {
                    BlePenStreamManager.getInstance().setHoverMode();
                    openStandardMode = false;
                    btn_hover_mode.setText("应用坐标");
                    imgHold.setVisibility(View.VISIBLE);
                } else {
                    BlePenStreamManager.getInstance().setStandMode();
                    openStandardMode = true;
                    btn_hover_mode.setText("悬浮坐标");
                    imgHold.setVisibility(View.GONE);
                }

            }
        });
        Button btn_check_updata = findViewById(R.id.btn_check_updata);
        btn_check_updata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean testCheckUpdate = true;
                BlePenStreamManager.getInstance().checkVersion(testCheckUpdate, new BleCheckVersionCallback() {
                    @Override
                    public void onCheckVersion(int state, String urlData, final String responseMessage) {
                        mUrlData = urlData;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, responseMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
        Button btn_updata = findViewById(R.id.btn_updata);
        btn_updata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mUrlData = "http:本地文件";
                if (TextUtils.isEmpty(mUrlData)) {
                    Toast.makeText(mContext, "请先检查有新版本再更新", Toast.LENGTH_SHORT).show();
                    return;
                }
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
                alertDialog.setTitle("注意")
                        .setMessage("将进入刷机模式？")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (TextUtils.isEmpty(mUrlData)) {
                                    Toast.makeText(mContext, "请先检查有新版本再更新", Toast.LENGTH_SHORT).show();
                                } else {
                                    //开始更新
                                    BlePenStreamManager.getInstance().updata(mUrlData, new BleUpdateCallback() {
                                        @Override
                                        public void onUpdateStart() {
                                            Log.d(TAG, "onUpdateStart: ");
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(mContext, "开始升级版本", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onUpdating(int state, final String updateMesage, final int percentage) {
                                            Log.d(TAG, "onUpdating: " + updateMesage + "    %" + percentage);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    onProgressShow(false, updateMesage, percentage);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onUpdateFinished(final int state, final String updateMesage) {
                                            Log.d(TAG, "onUpdateFinished: " + state);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    onProgressShow(true, updateMesage, 100);
                                                    if (state == 0) {
                                                        Toast.makeText(mContext, "版本升级成功,请重新进入程序使用", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(mContext, state + ",版本升级失败,请重新进入程序重新关开机再来一遍", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();

            }
        });
        Button btn_off_time = findViewById(R.id.btn_off_time);


        btn_off_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDialogShow();
            }
        });

        layout_update = findViewById(R.id.layout_update);

    }

    private void initBle() {
        if (BlePenStreamManager.getInstance().isConnected(bleDevice)) {
            isConnectedNow = true;
            //开启笔输出流
            BlePenStreamManager.getInstance().openPenStream(bleDevice, mBlePenStreamCallback);
            mHandle.removeMessages(MAG_SCAN);
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
                //TODO 通知H5
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_battery.setText(batteryPercent + "%");
                        txt_memory.setText(memoryPercent + "%，已使用字节数：" + byteNum);
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
                        mStrokeView.addDot(coordinateInfo);
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

                        txt_time.setText("time:" + simpleDateFormat.format(new Date(coordinateInfo.timeLong)));
                        txt_write.setText(writeString);
                        txt_coordinate.setText(coordinateInfo.coordX + "/" + coordinateInfo.coordY);
                        txt_force.setText(coordinateInfo.coordForce + "");
                        txt_paper_addres.setText(coordinateInfo.pageAddress);
                        if (coordinateInfo.isOFFLine) {
                            if (myProgress.getVisibility() == View.GONE) {
                                Log.d(TAG, "run:离线传输开始 ");
                                myProgress.setVisibility(View.VISIBLE);
                                txt_progress.setVisibility(View.VISIBLE);
                            }
                            if (coordinateInfo.offLineDataAllSize != 0) {
                                myProgress.setProgress(100 * coordinateInfo.offLineDateCurrentSize / coordinateInfo.offLineDataAllSize);
                                txt_progress.setText(" 上传进度：" + coordinateInfo.offLineDateCurrentSize + "/" + coordinateInfo.offLineDataAllSize);
                            }
                            if (coordinateInfo.offLineDateCurrentSize == coordinateInfo.offLineDataAllSize && myProgress.getVisibility() == View.VISIBLE) {
                                myProgress.setVisibility(View.GONE);
                                Log.d(TAG, "run:离线传输完毕 ");
                            }
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!TextUtils.isEmpty(softVersion)) {
                            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(mContext, formatTime, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onCurrentTime: " + formatTime);
            }
        };


    }

    private void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            bleDevice = intent.getParcelableExtra(KEY_DATA);
            int mode = Integer.parseInt(SharedPreferencesUtil.getInstance(mContext).getSP("mode"));

            if (mode == 1) {
                layout_mode.setVisibility(View.VISIBLE);
                layout_update.setVisibility(View.GONE);

            } else if (mode == 2) {
                layout_update.setVisibility(View.VISIBLE);
                layout_mode.setVisibility(View.GONE);
            } else {
                layout_mode.setVisibility(View.GONE);
                layout_update.setVisibility(View.GONE);
            }
            mBleDeviceName = bleDevice.getName();
            Log.d(TAG, "initData: bleDevice: " + mBleDeviceName);
        } else {
            Log.d(TAG, "initData: intent null  bleDevice null");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (progressDialog != null) {
            progressDialog = new ProgressDialog(mContext);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
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

    public void onProgressShow(boolean finished, String title, int percentage) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            if (finished) {
                mProgressDialog.dismiss();
            } else {
                mProgressDialog.setProgress(percentage);
                mProgressDialog.setTitle(title);

            }

        } else {
            mProgressDialog = new ProgressDialog(mContext);
            // 设置进度条的形式为圆形转动的进度条
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            // 设置是否可以通过点击Back键取消
            mProgressDialog.setCancelable(false);
            // 设置在点击Dialog外是否取消Dialog进度条
            mProgressDialog.setCanceledOnTouchOutside(false);
            // 设置提示的title的图标，默认是没有的，如果没有设置title的话只设置Icon是不会显示图标的
            mProgressDialog.setTitle(title);
            mProgressDialog.setMessage("固件升级提示:升级过程中请保持手机常亮状态，预计时间2-3分钟，请耐心等待");
            mProgressDialog.show();
        }
    }

    private void onDialogShow() {
        final EditText et = new EditText(this);
        et.setInputType(EditorInfo.TYPE_CLASS_PHONE);

        new AlertDialog.Builder(this).setTitle("设置关机时间")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = et.getText().toString();
                        if (TextUtils.isEmpty(input)) {
                            Toast.makeText(getApplicationContext(), "输入数字不能为空！" + input, Toast.LENGTH_LONG).show();
                        } else if (Integer.parseInt(input) >= 0 && Integer.parseInt(input) <= 360) {
                            BlePenStreamManager.getInstance().setPenOFFTime(Integer.parseInt(input));
                        } else {
                            Toast.makeText(getApplicationContext(), "请输入数字范围0-360！当前是" + input, Toast.LENGTH_LONG).show();

                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
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
        Observable.interval(1,  TimeUnit.MINUTES)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(Long value) {
                        BlePenStreamManager.getInstance().getPenInfo();
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
