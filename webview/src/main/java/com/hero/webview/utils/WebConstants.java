package com.hero.webview.utils;

public class WebConstants {

    public static final int LEVEL_REMOTE_PROCESS = 0; // local command, that is to say, this command execution does not require app.
    public static final int LEVEL_MAIN_PROCESS = 2; // 涉及到账号相关的level

    public static final int CONTINUE = 2; // 继续分发command
    public static final int SUCCESS = 1; // 成功
    public static final int FAILED = 0; // 失败
    public static final String EMPTY = ""; // 无返回结果

    public static final String WEB2NATIVE_CALLBACk = "callback";
    public static final String NATIVE2WEB_CALLBACK = "callbackname";

    public static final String ACTION_EVENT_BUS = "eventBus";

    public static final String INTENT_TAG_TITLE = "title";
    public static final String INTENT_TAG_URL = "url";
    public static final String INTENT_TAG_HEADERS = "headers";
    public static final String INTENT_TAG_IS_SHOW_ACTION_BAR = "is_show_action_bar";
    public static final String INTENT_TAG_CAN_NATIVE_REFRESH = "can_native_refresh";

    public static final String INIT_BLE ="initBle";
    public static final String BATTERY_INFO ="getBattery";
    public static final String ON_INIT_BLE="onInitBle";
    public static final String ON_BATTERY="onBattery";
    public static final String ON_DRAW="onDraw";

//    获取WIFI信号强度
//    监听WIFI状态改变（离线，上线）
    public static final String WIFI_RSSI="getWifiRssi";
    public static final String ON_WIFI_RSSI="onWifiRssi";

//    获取移动数据信号强度
//    监听移动数据状态改变（离线，上线）
    public static final String CELLULAR_RSSI="getCellularRssi";
    public static final String ON_CELLULAR_RSSI="onCellularRssi";

    public static final String NETWORK_STATE="getNetworkState";
    public static final String ON_NETWORK_STATE="onNetworkState";

    public static final String NETWORK_RSSI="getNetworkRssi";
    public static final String ON_NETWORK_RSSI="onNetworkRssi";
//    获取手机电量
//    监听电量变化
    public static final String MOBILE_BATTERY="getMobileBattery";
    public static final String ON_MOBILE_BATTERY="onMobileBattery";
//    获取蓝牙状态
//    监听蓝牙状态
    public static final String BLE_STATE="getBleState";
    public static final String ON_BLE_STATE="onBleState";

    public static final String BLE_OPERATION  = "bleOperation"; // 0 = 打开， 1= 关闭 2 = 开始搜索蓝牙  3 = 取消搜索蓝牙  4 = 连接蓝牙  5 = 断开蓝牙
    public static final String ON_BLE_OPERATION  = "onBleOperation";//6 = 打开成功  7 = 关闭成功  8. 搜索蓝牙结束  9 = 连接蓝牙成功 10 = 连接连接失败  11= 客户端蓝牙断开


    public static final int BLE_STATUS_NORMAL=0 ;
    public static final int BLE_STATUS_CONNECTED=1 ;
    public static final int BLE_STATUS_DISCONNECTED=2 ;
    public static final int BLE_STATUS_OPEN_STREAM=3 ;

    public static final String COMMAND_UPDATE_TITLE = "xiangxue_webview_update_title";
    public static final String COMMAND_UPDATE_TITLE_PARAMS_TITLE = "xiangxue_webview_update_title_params_title";

    public static class ERRORCODE {
        public static final int NO_METHOD = -1000;
        public static final int NO_AUTH = -1001;
        public static final int NO_LOGIN = -1002;
        public static final int ERROR_PARAM = -1003;
        public static final int ERROR_EXCEPTION = -1004;
    }

    public static class ERRORMESSAGE {
        public static final String NO_METHOD = "方法找不到";
        public static final String NO_AUTH = "方法权限不够";
        public static final String NO_LOGIN = "尚未登录";
        public static final String ERROR_PARAM = "参数错误";
        public static final String ERROR_EXCEPTION = "未知异常";
    }
}
