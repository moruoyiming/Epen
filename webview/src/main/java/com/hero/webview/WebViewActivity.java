package com.hero.webview;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.auto.service.AutoService;
import com.hero.webview.command.Command;
import com.hero.webview.command.CommandsManager;
import com.hero.webview.databinding.ActivityCommonWebBinding;
import com.hero.webview.utils.WebConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 *     author: jian
 *     Date  : 2020/5/25 2:13 PM
 *     Description:
 * </pre>
 */
public class WebViewActivity extends AppCompatActivity {
    private String title;
    private String url;
    private boolean showBar;
    private ActivityCommonWebBinding binding;
    private BaseWebFragment webviewFragment;


    public static void startCommonWeb(Context context, String title, String url) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra(WebConstants.INTENT_TAG_TITLE, title);
        intent.putExtra(WebConstants.INTENT_TAG_URL, url);
        if (context instanceof Service) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CommandsManager.getInstance().registerCommand(titleUpdateCommand);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_common_web);
        title = getIntent().getStringExtra(WebConstants.INTENT_TAG_TITLE);
        url = getIntent().getStringExtra(WebConstants.INTENT_TAG_URL);
        showBar = getIntent().getBooleanExtra(WebConstants.INTENT_TAG_IS_SHOW_ACTION_BAR, false);
        binding.actionBars.setVisibility(showBar?View.VISIBLE:View.GONE);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        webviewFragment = null;
        webviewFragment = WebViewFragment.newInstance(url, (HashMap<String, String>) getIntent().getExtras().getSerializable(WebConstants.INTENT_TAG_HEADERS), true);
        transaction.replace(R.id.web_view_fragment, webviewFragment).commit();
        setTitle(title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    public void CallJsMethod(String cmd,String params){
        webviewFragment.CallJsMethod(cmd,params);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webviewFragment != null && webviewFragment instanceof BaseWebFragment) {
            boolean flag = webviewFragment.onKeyDown(keyCode, event);
            if (flag) {
                return flag;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 页面路由
     */
    private final Command titleUpdateCommand = new Command() {
        @Override
        public String name() {
            return WebConstants.COMMAND_UPDATE_TITLE;
        }

        @Override
        public void exec(Context context, Map params, CommandCallBack resultBack) {
            if (params.containsKey(WebConstants.COMMAND_UPDATE_TITLE_PARAMS_TITLE)) {
                setTitle((String) params.get(WebConstants.COMMAND_UPDATE_TITLE_PARAMS_TITLE));
            }
        }
    };

}