package org.delta.epen.view;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.tstudy.blepenlib.BlePenStreamManager;
import com.tstudy.blepenlib.callback.BleGattCallback;
import com.tstudy.blepenlib.callback.BleScanCallback;
import com.tstudy.blepenlib.data.BleDevice;
import com.tstudy.blepenlib.exception.BleException;

import org.delta.epen.adapter.DeviceAdapter;
import org.delta.epen.R;

import java.util.List;

public class BleDialog extends DialogFragment {
    private static final String TAG = "RecordDialog";
    private ImageView close;
    private DeviceAdapter mDeviceAdapter;
    private onConnectedListener onConnectedListener;
    private TextView tvScan;
    private TextView tvNull;
    private ListView mListView;
    private boolean enabled = false;
    private BluetoothAdapter mBluetoothAdapter;

    public onConnectedListener getOnConnectedListener() {
        return onConnectedListener;
    }

    public void setOnConnectedListener(onConnectedListener onConnectedListener) {
        this.onConnectedListener = onConnectedListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ble_list, container);
        mDeviceAdapter = new DeviceAdapter(getActivity());
        tvScan = view.findViewById(R.id.btn_scan);
        tvNull = view.findViewById(R.id.ble_null);
        tvScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(getActivity(), "正在开启蓝牙", Toast.LENGTH_LONG).show();
                    boolean ble = mBluetoothAdapter.enable();
                    if (ble) {
                        Toast.makeText(getActivity(), "蓝牙开启成功", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getActivity(), "蓝牙开启失败", Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (!enabled) {
                        startScan();
                    } else {
                        Toast.makeText(getActivity(), "正在扫描蓝牙设备", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        close = view.findViewById(R.id.iv_close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
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
                Log.d(TAG, "initData: onDetail: ");
                if (BlePenStreamManager.getInstance().isConnected(bleDevice)) {
                }
            }
        });
        mListView = (ListView) view.findViewById(R.id.list_device);
        mListView.setAdapter(mDeviceAdapter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getActivity(), "检查设备是否支持蓝牙BLE", Toast.LENGTH_LONG).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getActivity(), "正在开启蓝牙", Toast.LENGTH_LONG).show();
            boolean ble = mBluetoothAdapter.enable();
            if (ble) {
                Toast.makeText(getActivity(), "蓝牙开启成功", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "蓝牙开启失败", Toast.LENGTH_LONG).show();
            }
        } else {
            startScan();
        }
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.gravity = Gravity.BOTTOM | Gravity.CENTER;
        return view;
    }

    private void connect(final BleDevice bleDevice) {
        //连接回调
        BleGattCallback bleGattCallback = new BleGattCallback() {
            @Override
            public void onStartConnect() {
                Log.d(TAG, "onStartConnect: ");
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                Log.d(TAG, "onConnectFail: ");
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                if(mDeviceAdapter!=null){
                    mDeviceAdapter.addDevice(0, bleDevice);
                    mDeviceAdapter.notifyDataSetChanged();
                }
                Log.d(TAG, "onConnectSuccess: " + BlePenStreamManager.getInstance().isConnected(bleDevice) + "   " + bleDevice);
                if (BlePenStreamManager.getInstance().isConnected(bleDevice)) {
                    if (onConnectedListener != null) {
                        onConnectedListener.onConnected(bleDevice);
                    }
                }
                dismiss();
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                if(mDeviceAdapter!=null){
                    mDeviceAdapter.removeDevice(bleDevice);
                    mDeviceAdapter.notifyDataSetChanged();
                }
                if (onConnectedListener != null) {
                    onConnectedListener.onDisConnected();
                }
            }
        };
        BlePenStreamManager.getInstance().connect(bleDevice.getMac(), bleGattCallback);
    }

    private void startScan() {
        //扫描回调
        BleScanCallback callback = new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                if (isAdded()) {
                    Log.d(TAG, "onScanStarted: " + success);
                    enabled = true;
                    tvNull.setVisibility(View.GONE);
                    mListView.setVisibility(View.VISIBLE);
                    tvScan.setText(R.string.scan_scan);
                    mDeviceAdapter.clearScanDevice();
                    mDeviceAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                if (isAdded()) {
                    Log.d(TAG, "onScanning: " + bleDevice);
                    mDeviceAdapter.addDevice(bleDevice);
                    mDeviceAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                if (isAdded()) {
                    enabled = false;
                    tvScan.setText(R.string.start_scan);
                    String message = getString(R.string.bel_null);
                    Log.d(TAG, "onScanFinished: " + scanResultList.toString());
                    if (scanResultList.size() == 0) {
                        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                        tvNull.setVisibility(View.VISIBLE);
                        mListView.setVisibility(View.GONE);
                    }
                }
            }
        };
        BlePenStreamManager.getInstance().scan(callback);
    }


    public interface onConnectedListener {
        void onConnected(BleDevice bleDevice);

        void onDisConnected();
    }
}
