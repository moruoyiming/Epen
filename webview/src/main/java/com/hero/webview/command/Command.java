package com.hero.webview.command;

import android.content.Context;


import com.hero.webview.CommandCallBack;

import java.util.Map;

public interface Command {

    String name();

    void exec(Context context, Map params, CommandCallBack callBack);
}
