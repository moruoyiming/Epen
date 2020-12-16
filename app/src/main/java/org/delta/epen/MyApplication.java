package org.delta.epen;

import android.app.Application;

import com.hero.basiclib.loadsir.CustomCallback;
import com.hero.basiclib.loadsir.EmptyCallback;
import com.hero.basiclib.loadsir.ErrorCallback;
import com.hero.basiclib.loadsir.LoadingCallback;
import com.hero.basiclib.loadsir.TimeoutCallback;
import com.hero.webview.command.CommandsManager;
import com.hero.webview.command.ShowDialogCommand;
import com.hero.webview.command.ToastCommand;
import com.kingja.loadsir.core.LoadSir;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LoadSir.beginBuilder()
                .addCallback(new ErrorCallback())//添加各种状态页
                .addCallback(new EmptyCallback())
                .addCallback(new LoadingCallback())
                .addCallback(new TimeoutCallback())
                .addCallback(new CustomCallback())
                .setDefaultCallback(LoadingCallback.class)//设置默认状态页
                .commit();
    }
}
