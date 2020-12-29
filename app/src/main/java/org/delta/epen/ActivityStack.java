package org.delta.epen;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;


import androidx.annotation.IntDef;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * @Desc
 * @Author songxg
 * @Date 2018-10-10 23:07
 */
public class ActivityStack {

    private static final String TAG = "ActivityStack";
    // 页面退出的tag
    public static final int TAG_ACCOUNT_LOGOUT = 0x0001; // 该tag的页面会在账户退出时finish掉
    public static final int TAG_USERS = 0x0010;

    @IntDef({TAG_ACCOUNT_LOGOUT, TAG_USERS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActivityTag {

    }

    private static boolean sIsBackground = false;

    private final static List<Activity> sActivityList = new ArrayList<>();

    private final static List<Activity> sResumeActivityList = new ArrayList<>();

    public static String curResumeActivityName,curPauseActivityName,curStopActivityName;

    private ActivityStack() {

    }


    private final static Application.ActivityLifecycleCallbacks sLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (activity instanceof AppCompatActivity) {
                pushInstance(activity);
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (activity instanceof AppCompatActivity) {
                saveResume(activity);
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
            curResumeActivityName = activity.getClass().getName();
            if(stopRunable != null){
                Log.i("cocos_platform_sdk_ac","remove onResume stopRunable = " +stopRunable + ",currentPid = "+ Process.myPid());
                handler.removeCallbacks(stopRunable);
                stopRunable = null;
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
            curPauseActivityName = activity.getClass().getName();
        }

        @Override
        public void onActivityStopped(final Activity activity) {
            if (activity instanceof AppCompatActivity) {
                removeResume(activity);
            }
            curStopActivityName = activity.getClass().getName();
        }

        private Handler handler = new Handler(Looper.getMainLooper());

        private Runnable stopRunable;


        private void handStopMainActivity(final Activity activity){
            Log.i("cocos_platform_sdk_ac","collect curResumeActivityName = " +curResumeActivityName + "....curStopActivityName = "+curStopActivityName + ",currentPid = "+ Process.myPid());
            if(stopRunable != null){
                handler.removeCallbacks(stopRunable);
                Log.i("cocos_platform_sdk_ac","remove onstop stopRunable = " +stopRunable +  ",currentPid = "+ Process.myPid());
                stopRunable = null;
            }
            if(curResumeActivityName.equals(curStopActivityName) && isBeKillInBackGround()) {
                stopRunable = new Runnable() {
                    @Override
                    public void run() {
                        if (curResumeActivityName.equals(curStopActivityName)
                                && isBeKillInBackGround()) {
                            Log.i("cocos_platform_sdk_ac","begin to finish curResumeActivityName = " +curResumeActivityName + "....curStopActivityName = "+curStopActivityName);
                            for(Activity stackActivity : sActivityList){
                                        if(stackActivity.getClass().getName().startsWith("com.cocos.vs")){
                                            if (stackActivity != null) {
                                                Log.i("cocos_platform_sdk_ac","finish activity = "+ stackActivity.getClass().getName());
                                                stackActivity.finish();
                                            }
                                        }
                                    }
                        }
                    }
                };
                Log.i("cocos_platform_sdk_ac","send curResumeActivityName = " +curResumeActivityName + "   curStopActivityName = "+curStopActivityName+ "  currentPid = "+ Process.myPid() + "  runable = "+stopRunable);

                handler.postDelayed(stopRunable,8*60*1000);
            }
        }

        private boolean isBeKillInBackGround(){
            if(curStopActivityName.startsWith("com.cocos.vs") || curStopActivityName.startsWith("com.bytedance.sdk")){
                return true;
            }
            return false;
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (activity instanceof AppCompatActivity) {
                popInstance(activity);
            }
        }
    };

    private static void pushInstance(Activity activity) {
        synchronized (sActivityList) {
            sActivityList.add(activity);
            Log.i(TAG,"pushInstance:" + activity.getClass().getSimpleName());
        }
    }

    private static void popInstance(Activity activity) {
        synchronized (sActivityList) {
            sActivityList.remove(activity);
            Log.i(TAG,"popInstance:" + activity.getClass().getSimpleName());
        }
    }

    /**
     * 保存唤醒的 activity
     */
    private static void saveResume(Activity activity) {
        synchronized (sResumeActivityList) {
            boolean isEmpty = sResumeActivityList.isEmpty();
            sResumeActivityList.add(activity);
            if (isEmpty) {
                Log.i(TAG,"App resume");
                sIsBackground = false;
            }
        }
    }

    /**
     * 删除唤醒的 activity
     */
    private static void removeResume(Activity activity) {
        synchronized (sResumeActivityList) {
            sResumeActivityList.remove(activity);
            if (sResumeActivityList.isEmpty()) {
                Log.i(TAG,"App pause");
                sIsBackground = true;
            }
        }
    }

    public static boolean isBackground() {
        return sIsBackground;
    }

    public static void init(Application application) {
        if (application != null) {
            application.registerActivityLifecycleCallbacks(sLifecycleCallbacks);
        }

    }


    /**
     * 取栈内实例
     *
     * @param index 栈内下标
     * @return 实例对象
     */
    public static Activity takeInstance(int index) {
        if (index >= 0 && index < sActivityList.size()) {
            Activity activity = sActivityList.get(index);
            return activity;
        }
        return null;
    }

    /**
     * 取栈顶实例
     *
     * @return 实例对象
     */
    public static Activity takeInstance() {
        return takeInstance(sActivityList.size() - 1);
    }

    /**
     * 退出整个程序
     *
     * @param isDesc 是否倒序
     */
    public static void exitApplication(boolean isDesc) {
        synchronized (sActivityList) {
            if (isDesc) {
                for (int i = sActivityList.size() - 1; i >= 0; i--) {
                    Activity activity = sActivityList.get(i);
                    activity.finish();
                }
            } else {
                for (int i = 0; i < sActivityList.size(); i++) {
                    Activity activity = sActivityList.get(i);
                    activity.finish();
                }
            }
            sActivityList.clear();
        }
    }

    /**
     * 栈的大小
     *
     * @return Activity实例数
     */
    public static int size() {
        return sActivityList.size();
    }

    /**
     * 退出整个程序,倒序
     */
    public static void exitApplication() {
        exitApplication(true);
    }

    public interface IAutoExit {
        int getActivityTag();
    }

}
