package org.delta.epen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("BOOTonReceive","Android 设备重启"+intent.getAction()+"android.intent.action.BOOT_COMPLETED".equals(intent.getAction()));
        //android.intent.action.BOOT_COMPLETED
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent toIntent = new Intent(context, MainActivity.class);
            toIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(toIntent);
        }
//        PackageManager p=context.getPackageManager();
//        Intent in=p.getLaunchIntentForPackage("org.delta.epen"); // 启动其他应用程序
//        if(in!=null){
//            context.startActivity(in);
//        }else{
//            Toast.makeText(context, "哟，赶紧下载安装这个APP吧", Toast.LENGTH_LONG).show();
//        }
    }
}

