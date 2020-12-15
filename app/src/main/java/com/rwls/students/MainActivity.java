package com.rwls.students;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.permission.annotation.Permission;
import com.example.permission.annotation.PermissionCancel;
import com.example.permission.annotation.PermissionDenied;
import com.tstudy.blepenlib.BlePenStreamManager;
import com.tstudy.blepenlib.callback.BleGattCallback;
import com.tstudy.blepenlib.callback.BleScanCallback;
import com.tstudy.blepenlib.data.BleDevice;
import com.tstudy.blepenlib.exception.BleException;
import com.tstudy.blepenlib.utils.SharedPreferencesUtil;
import com.tstudy.students.R;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity_tag";
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    private static final int REQUEST_OPEN_BT_CODE = 3;
    private Button btn_scan;
    private ImageView img_loading;
    private Animation operatingAnim;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog progressDialog;
    private Context mContext;
    private BleDevice mBleDevice;
    //0 调试模式；1 编辑模式；2 精简模式
    private int mode;
    private BluetoothAdapter mBluetoothAdapter;
    private RelativeLayout ly_location_warn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "权限请求拒绝");
        setContentView(R.layout.activity_main);
        initView();
        mContext = MainActivity.this;
        boolean initSuccess = BlePenStreamManager.getInstance().init(getApplication(), MyLicense.getBytes());
        if (!initSuccess) {
            Toast.makeText(this, "初始化失败，请到开放平台申请授权或检查设备是否支持蓝牙BLE", Toast.LENGTH_LONG).show();
        }
        //lib 日志开关 true 打开  默认false
        BlePenStreamManager.getInstance().enableLog(true);
        SharedPreferencesUtil.getInstance(mContext).putSP("mode", String.valueOf(mode));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDeviceAdapter != null) {
            mDeviceAdapter.clear();
            mDeviceAdapter.notifyDataSetChanged();
        }
        if (!gpsIsOpen(mContext)) {
            ly_location_warn.setVisibility(View.VISIBLE);
        } else {
            ly_location_warn.setVisibility(View.GONE);
        }

    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     *
     * @param context
     * @return true 表示开启
     */
    public boolean gpsIsOpen(final Context context) {
        LocationManager locationManager
                = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean passive = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.d(TAG, "gpsIsOpen: gps" + gps + "    passive" + passive + "     network " + network);
        if (gps || network || passive) {
            return true;
        }
        return false;
    }

    private void initView() {
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        toolbar.setSubtitle("TD-602/TD-603/TD-701/TD-801...");
//        以上3个属性必须在setSupportActionBar(toolbar)之前调用
//        setSupportActionBar(toolbar);
        //设置导航Icon，必须在setSupportActionBar(toolbar)之后设置

        ly_location_warn = findViewById(R.id.ly_location_warn);
        ly_location_warn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
            }
        });

        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_scan.setText(getString(R.string.start_scan));

        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                WebViewActivity.startCommonWeb(MainActivity.this,"xxx","https://www.baidu.com");
                if (btn_scan.getText().equals(getString(R.string.start_scan))) {
                    checkPermissions();

                } else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
                    BlePenStreamManager.getInstance().cancelScan();
                }
            }
        });
        img_loading = (ImageView) findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        progressDialog = new ProgressDialog(this);

        mDeviceAdapter = new DeviceAdapter(this);
        mDeviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onConnect(BleDevice bleDevice) {
                if (!BlePenStreamManager.getInstance().isConnected(bleDevice)) {
                    //如果当前设备未连接，取消扫描，连接选中设备。
                    BlePenStreamManager.getInstance().cancelScan();
                    connect(bleDevice);
                }
            }

            @Override
            public void onDisConnect(final BleDevice bleDevice) {
                if (BlePenStreamManager.getInstance().isConnected(bleDevice)) {
                    //如果当前设备已连接，断开连接。
                    BlePenStreamManager.getInstance().disconnect(bleDevice);
                }
            }

            @Override
            public void onDetail(BleDevice bleDevice) {
                if (BlePenStreamManager.getInstance().isConnected(bleDevice)) {
                    //跳到绘制界面
                    Intent intent = new Intent(MainActivity.this, DrawActivity.class);
                    intent.putExtra(DrawActivity.KEY_DATA, bleDevice);
                    startActivity(intent);
                }
            }
        });
        ListView listView_device = (ListView) findViewById(R.id.list_device);
        listView_device.setAdapter(mDeviceAdapter);
    }

    private void startScan() {
        //扫描回调
        BleScanCallback callback = new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                Log.d(TAG, "onScanStarted: " + success);
                mDeviceAdapter.clearScanDevice();
                mDeviceAdapter.notifyDataSetChanged();
                img_loading.startAnimation(operatingAnim);
                img_loading.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.stop_scan));
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
            }
        };
        BlePenStreamManager.getInstance().scan(callback);
    }

    private void connect(final BleDevice bleDevice) {
        //连接回调
        BleGattCallback bleGattCallback = new BleGattCallback() {
            @Override
            public void onStartConnect() {
                progressDialog.setMessage("正在连接蓝牙点阵笔:" + bleDevice.getName());

                progressDialog.show();
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
                progressDialog.dismiss();
                Log.d(TAG, "onConnectFail: " + bleDevice + "   exception:" + exception);
                Toast.makeText(MainActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                mBleDevice = bleDevice;
                progressDialog.dismiss();
                mDeviceAdapter.addDevice(0, bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
                if (BlePenStreamManager.getInstance().isConnected(bleDevice)) {
                    //跳到绘制界面
                    Intent intent = new Intent(MainActivity.this, DrawActivity.class);
                    intent.putExtra(DrawActivity.KEY_DATA, bleDevice);
                    startActivity(intent);
                }
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();
                mDeviceAdapter.removeDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
                String disConnectedMes = getString(R.string.disconnected);
                if (isActiveDisConnected) {
                    disConnectedMes = getString(R.string.active_disconnected);
                }
                Toast.makeText(MainActivity.this, disConnectedMes, Toast.LENGTH_LONG).show();
                Log.d(TAG, "onDisConnected: " + disConnectedMes);

            }
        };
        BlePenStreamManager.getInstance().connect(bleDevice.getMac(), bleGattCallback);
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                //这里进行授权被允许的处理
                startScan();
            } else {
                //这里进行权限被拒绝的处理
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(MainActivity.this, "自Android 6.0开始需要打开位置权限才可以搜索到Ble设备", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    private void checkPermissions() {
        //当Android版本大于等于6.0时才会加载布局，注册广播接收器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "检查设备是否支持蓝牙BLE", Toast.LENGTH_LONG).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_OPEN_BT_CODE);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //如果 API level 是大于等于 23(Android 6.0) 时
            //判断是否具有权限
            Log.d(TAG, "checkPermissions ACCESS_FINE_LOCATION: " + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION));
            Log.d(TAG, "checkPermissions ACCESS_COARSE_LOCATION: " + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION));
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要向用户解释为什么需要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(MainActivity.this, "自Android 6.0开始需要打开位置权限才可以搜索到Ble设备", Toast.LENGTH_LONG).show();
                }
                //请求权限
//                ActivityCompat.requestPermissions(this,
//                        new String[]{},
//                        REQUEST_CODE_PERMISSION_LOCATION);
                requestLocation();
            } else {
                startScan();
            }
        } else {
            startScan();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_BT_CODE) {
            if (resultCode == RESULT_OK) {

            } else {

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, Menu.FIRST + 1, 0, "精简绘制模式");
        menu.add(Menu.NONE, Menu.FIRST + 2, 1, "接口调试模式");
        menu.add(Menu.NONE, Menu.FIRST + 3, 2, "升级固件模式");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) //得到被点击的item的itemId
        {
            case Menu.FIRST + 1:
                //默认精简绘制模式-直接书写
                mode = 0;
                SharedPreferencesUtil.getInstance(mContext).putSP("mode", String.valueOf(mode));
                break;
            case Menu.FIRST + 2:
                //接口调试模式-调试SDK接口
                mode = 1;
                SharedPreferencesUtil.getInstance(mContext).putSP("mode", String.valueOf(mode));
                break;
            case Menu.FIRST + 3:
                //升级固件模式-笔端固件版本升级
                onDialogShow();
                break;
        }

        return true;
    }

    private void onDialogShow() {
        new AlertDialog.Builder(this).setTitle("设置固件升级模式")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("请确认笔硬件版本和将升级固件版本的兼容性再使用升级功能，必要时请联系开发人员")
                .setPositiveButton("确定使用", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mode = 2;
                        SharedPreferencesUtil.getInstance(mContext).putSP("mode", String.valueOf(mode));
                    }
                })
                .setNegativeButton("取消", null)
                .show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBleDevice != null) {
            BlePenStreamManager.getInstance().disconnect(mBleDevice);
        }
    }

    /**
     * 这里写的要特别注意，denied方法，必须是带有一个int参数的方法，下面的也一样
     *
     * @param requestCode
     */
    @PermissionCancel
    public void denied(int requestCode) {
        Log.e(TAG, "权限请求拒绝");
        Toast.makeText(mContext, "拒绝蓝牙权限", Toast.LENGTH_SHORT).show();
    }

    @PermissionDenied
    public void deniedForever(int requestCode) {
        Log.e(TAG, "权限请求拒绝，用户永久拒绝");
    }

    @Permission(permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, requestCode = 1)
    public void requestLocation() {
        Log.i("Permission", "权限请求成功");
        checkPermissions();
    }


}
