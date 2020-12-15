package com.example.webview.command;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import com.example.webview.CommandCallBack;
import com.example.webview.utils.MainLooper;
import com.example.webview.utils.WebConstants;
import com.example.webview.widget.BaseWebView;
import com.google.gson.Gson;

import java.util.Map;

public class CommandDispatcher {
    private static CommandDispatcher instance;
    private Gson gson = new Gson();

    public static CommandDispatcher getInstance() {
        if (instance == null) {
            synchronized (CommandDispatcher.class) {
                if (instance == null) {
                    instance = new CommandDispatcher();
                }
            }
        }
        return instance;
    }

    public void exec(Context context, String cmd, String params, final WebView webView) {
        Log.i("CommandDispatcher", "command: " + cmd + " params: " + params);
        try {
            if (CommandsManager.getInstance().isWebviewProcessCommand(cmd)) {
                Map mapParams = gson.fromJson(params, Map.class);
                CommandsManager.getInstance().execWebViewProcessCommand(context, cmd, mapParams, new CommandCallBack() {
                    @Override
                    public void onResult(int status, String action, Object result) {
                        handleCallback(status, action, gson.toJson(result), webView);
                    }
                });
            }
        } catch (Exception e) {
            Log.e("CommandDispatcher", "Command exec error!!!!", e);
        }
    }

    private void handleCallback(final int responseCode, final String actionName, final String response,
                                final WebView webView) {
        Log.d("CommandDispatcher", String.format("Callback result: action= %s, result= %s", actionName, response));
        MainLooper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Map params = new Gson().fromJson(response, Map.class);
                if (params.get(WebConstants.NATIVE2WEB_CALLBACK) != null && !TextUtils.isEmpty(params.get(WebConstants.NATIVE2WEB_CALLBACK).toString())) {
                    if (webView instanceof BaseWebView) {
                        ((BaseWebView) webView).handleCallback(response);
                    }
                }
            }
        });
    }
}
