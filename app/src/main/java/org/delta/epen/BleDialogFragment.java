package org.delta.epen;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
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

import java.util.List;

public class BleDialogFragment extends DialogFragment {
    private static final String TAG = "RecordDialog";
    private ImageView close;
    private DeviceAdapter mDeviceAdapter;
    private Context context;
    private onConnectedListener onConnectedListener;
    private TextView tvScan;

    public onConnectedListener getOnConnectedListener() {
        return onConnectedListener;
    }

    public void setOnConnectedListener(onConnectedListener onConnectedListener) {
        this.onConnectedListener = onConnectedListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        context=getActivity();
        View view = inflater.inflate(R.layout.vs_layout_history, container);
        mDeviceAdapter = new DeviceAdapter(context);
        tvScan = view.findViewById(R.id.btn_scan);
        tvScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvScan.setEnabled(false);
                startScan();
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
        ListView listView_device = (ListView) view.findViewById(R.id.list_device);
        listView_device.setAdapter(mDeviceAdapter);
        startScan();
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.gravity = Gravity.BOTTOM|Gravity.CENTER;
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
                Toast.makeText(context, R.string.connected, Toast.LENGTH_LONG).show();
                mDeviceAdapter.addDevice(0, bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
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
                mDeviceAdapter.removeDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
                String disConnectedMes = context.getString(R.string.disconnected);
                if (isActiveDisConnected) {
                    disConnectedMes = context.getString(R.string.active_disconnected);
                }
                Toast.makeText(context, disConnectedMes, Toast.LENGTH_LONG).show();
                Log.d(TAG, "onDisConnected: " + disConnectedMes);
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
                Log.d(TAG, "onScanStarted: " + success);
                mDeviceAdapter.clearScanDevice();
                mDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                Log.d(TAG, "onScanning: " + bleDevice);
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                Log.d(TAG, "onScanFinished: " + scanResultList.toString());
                tvScan.setEnabled(true);
            }
        };
        BlePenStreamManager.getInstance().scan(callback);
    }

    public interface onConnectedListener {
        void onConnected(BleDevice bleDevice);

        void onDisConnected();
    }
}
