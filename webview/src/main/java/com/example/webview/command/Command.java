package com.example.webview.command;

import android.content.Context;


import com.example.webview.CommandCallBack;

import java.util.Map;

public interface Command {

    String name();

    void exec(Context context, Map params, CommandCallBack callBack);
}
