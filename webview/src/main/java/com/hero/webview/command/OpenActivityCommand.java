package com.hero.webview.command;

import android.content.Context;


import com.google.auto.service.AutoService;
import com.hero.webview.CommandCallBack;

import java.util.Map;
@AutoService({Command.class})
public class OpenActivityCommand implements Command {

    @Override
    public String name() {
        return "newPage";
    }

    @Override
    public void exec(Context context, Map params, CommandCallBack callBack) {
//        String newUrl = params.get("url").toString();
//        String title = (String) params.get("title");
//        Intent intent = new Intent(context, DemoActivity.class);
//        intent.putExtra("title",title);
//        context.startActivity(intent);
    }
}
