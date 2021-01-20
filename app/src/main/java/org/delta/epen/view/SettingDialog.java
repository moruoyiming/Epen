package org.delta.epen.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;


import com.tstudy.blepenlib.data.BleDevice;

import org.delta.epen.R;

public class SettingDialog extends DialogFragment {
    private ImageView close;
    private TextView tvDetails;
    private onSetCallBack onSetCallBack;
    private int REQUEST_PERMISSION_SETTING = 131;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.ble_setting, container);
        close = view.findViewById(R.id.iv_close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        tvDetails = view.findViewById(R.id.tv_details);
        tvDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent,REQUEST_PERMISSION_SETTING);
            }
        });
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("permission"," resultcode "+resultCode + " requestCode  "+requestCode);
        if(requestCode==REQUEST_PERMISSION_SETTING){
            dismiss();
            if (onSetCallBack != null) {
                onSetCallBack.onResult();
            }
        }
    }

    public SettingDialog.onSetCallBack getOnSetCallBack() {
        return onSetCallBack;
    }

    public void setOnSetCallBack(SettingDialog.onSetCallBack onSetCallBack) {
        this.onSetCallBack = onSetCallBack;
    }

    public interface onSetCallBack {
        void onResult();
    }
}
