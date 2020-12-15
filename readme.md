## 拓思德数码笔TD Android集成文档(ble) ##


## 一、Android SDK概述 ##
 
数码点阵笔sdk实现了android手机与数码笔的连接通讯，手机通过蓝牙连接可以获取点阵笔的基本参数信息（电量、内存使用）及书写后的点阵信息（坐标，落笔，抬笔），同时可以发送事件指令到点阵笔。

## 二、运行环境 ##
 
sdk支持数码点阵笔型号为TD-602/TD-603/TD-701/TD-801...，支持android 4.4及上版本的手机系统，手机需支持BLE蓝牙功能

##  三、集成准备  ##
 
### 导入数码笔的SDK LIB ###
 - 将sdk文件包libs文件夹中的jar文件考入到项目目录的libs文件夹下

### 在AndroidManifest.xml 里添加相关声明 ###
 - 添加最低支持的操作系统版本控制
    ```xml

    <uses-sdkandroid:minSdkVersion="19"></uses-sdk>

    ```
    <-- SDK必须是19以上的，因为从 Android4.3开始，才正式支持BLE 蓝牙， 4.4以上测试比较稳定。 -->
 - 添加权限声明(蓝牙、位置、网络)
    ```xml

    <uses-permission android:name="android.permission.BLUETOOTH" />

    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

	<-- 笔端OTA升级需要网络权限 -->

	<uses-permission android:name="android.permission.INTERNET"/>

    ```

## 四、集成步骤 ##

### 1、初始化并验证授权 ###
 - 授权文件
 
 平台注册成功后需要添加应用，添加应用成功后需申请授权，授权审批通过后，下载授权文件MyLicense.java，此授权文件只能在添加应用时的包名项目中使用，需新建一个注册应用包名对应的Android的项目，或将demo的包名改成你注册应用的包名，将Mylicense.java文件拷贝到相应目录下就可以了。初始化成功才能使用SDK以下功能。

 - 在您应用程序主Activity里导入BleManager如下所示：
 
```java

	import com.tstudy.blepenlib.BlePenStreamManager;

	BlePenStreamManager.getInstance().init(Context context,byte[] bytes) 

    context:上下文对象   bytes: MyLicense.getBytes()授权文件的签名

 ```

### 2、扫描外围设备 ###

 - 初始化SDK成功后，打开蓝牙开关（自Android 6.0开始需要打开位置权限才可以搜索到Ble设备），开始扫描蓝牙：

```java

	  BlePenStreamManager.getInstance().scan(BleScanCallback callback）;
	  ·callback：蓝牙扫描回调

      ·void onScanStarted(boolean success);
      ·会回到主线程，参数表示本次扫描动作是否开启成功。由于蓝牙没有打开，上一次扫描没有结束等原因，会造成扫描开启失败。
 
      
      ·void onLeScan(BleDevice bleDevice);
 	  ·扫描过程中所有被扫描到的结果回调。由于扫描及过滤的过程是在工作线程中的，此方法也处于工作线程中。
	   同一个设备会在不同的时间，携带自身不同的状态（比如信号强度等），出现在这个回调方法中，出现次数取决于周围的设备量及外围设备的广播间隔。
      
      ·void onScanning(BleDevice bleDevice);    
 	  ·扫描过程中的所有过滤后的结果回调。与onLeScan区别之处在于：它会回到主线程；同一个设备只会出现一次；
	   出现的设备是经过扫描过滤规则过滤后的设备。
      
      ·void onScanFinished(List<BleDevice> scanResultList);    
	  ·本次扫描时段内所有被扫描且过滤后的设备集合。它会回到主线程，相当于onScanning设备之和。
 ```
### 3、取消扫描 ###

```java

	BlePenStreamManager.getInstance().cancelScan()；

```

### 4、连接蓝牙设备 ###

- 选中扫描列表中的目标设备连接

```java

	- 通过设备对象连接蓝牙
    BlePenStreamManager.getInstance().connect(BleDevice bleDevice, BleGattCallback bleGattCallback);

	- 通过设备mac连接蓝牙
    BlePenStreamManager.getInstance().connect(String mac, BleGattCallback bleGattCallback);
	·bleGattCallback：蓝牙连接回调
    
	·void onStartConnect() 
	·开始进行连接。
 
    ·void onConnectFail(BleException exception) 
    ·连接失败。   
 
    ·void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) 
    ·连接成功。
 
    ·void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) 
    ·连接断开，特指连接后再断开的情况。在这里可以监控设备的连接状态，
    ·isActiveDisConnected：是否主动断开；
	主动断开：调用disconnect、笔端主动关机；
	被动断开：关闭系统蓝牙、笔自动关机、超出蓝牙有效范围

	一旦连接断开，可以根据自身情况考虑对BleDevice对象进行重连操作。
	需要注意的是，断开和重连之间最好间隔一段时间，否则可能会出现长时间连接不上的情况。
   


```
### 5、断开连接蓝牙设备 ###

- 结束使用蓝牙笔时候断开设备

```java

	- 通过设备对象断开蓝牙
	BlePenStreamManager.getInstance().disconnect(BleDevice bleDevice);
	·bleDevice：蓝牙设备实例
	
	- 通过设备mac断开蓝牙
	BlePenStreamManager.getInstance().disconnect(String mac);
	·mac：蓝牙设备mac地址

```

### 6、打开笔输出流 ###

- 打开笔输出流，自动获得电量、内存、已使用字节数等信息，笔内若有缓存可以选择删除缓存或者输出缓存数据，输出缓存（onOutLineCoordDraw）可实时查看离线传输的开始、结尾、及中间的实时进度。

```java
	
	- 通过设备对象建立通讯
	BlePenStreamManager.getInstance().openPenStream(BleDevice bleDevice, BlePenStreamCallback blePenStreamCallback)

	- 通过设备mac建立通讯
	BlePenStreamManager.getInstance().openPenStream(String mac, BlePenStreamCallback blePenStreamCallback)

    onOpenPenStreamStatus(boolean openSuccess,String message);
	·openSuccess 打开笔输出流状态 true：成功  false：失败
	·message：描述信息

    onRemainBatteryAndMemory(int batteryPercent,int memoryPercent,int byteNum);
	·剩余电量内存信息
	·batteryPercent：剩余电量百分比
	·percent：剩余内存百分比
	·byteNum: 已使用字节数，单位字节。
	
    void onCoordDraw(CoordinateInfo coordinateInfo);
     
    ·coordinateInfo：坐标信息
         |                  state| 笔状态(down,up,move)|
         |           pageAddress | 点阵地址(页码信息)  |move状态下页码无效|
         |                coordX | 坐标X               |move状态下坐标无效|
         |                coordY | 坐标Y               |move状态下坐标无效|
         |            coordForce | 压力值              |move状态下压力无效|
         |             strokeNum | 笔画数              |
         |              timeLong | 时间戳              |
         |              isOFFLine| 是否是离线笔迹      |
         |    offLineDataAllSize | 离线总数据          |
     	 |offLineDateCurrentSize | 当前已传输离线数据  |
     ·其中笔的状态为move时坐标携带的坐标信息（点阵地址、坐标、压力）有效，
       down、up时坐标信息（点阵地址、坐标、压力）无效，绘制一般只取落笔抬笔事件用于重置绘制状态
       
    void onWarnActiveReport(int statusNum)
	·警告主动上报事件
    ·statusNum  警告状态码 
	·0x05  电池电量低警告  0x08 存储空间警告

    void onVersionAndserialNumber(String hardVersion, String softVersion,String serialNumber);
	·软硬件版本号
	·hardVersion 笔硬件版本号
	·softVersion 固件版本
	·serialNumber笔端序列号

    void onCurrentTime(long time);
	·获取当前的笔端时间
	·time 笔端当前时间(单位ms)

```

### 7、检查更新 ###

- 检查是否有新版本,如不需要SDK检查更新，直接传下载固件地址url给updata更新接口请忽略该接口。

```java

  	BlePenStreamManager.getInstance().checkVersion(boolean testCheckUpdate,  BleCheckVersionCallback bleCheckVersionCallback);
	·testCheckUpdate：boolean类型，是否测试检查更新
	·bleCheckVersionCallback 检查更新回调

    void onCheckVersion(int state,String urlData,String responseMessage);
	·state      0 成功 已是最新版本；
     			1 成功 有最新版本
     			400 手机网络异常

	·urlData  （state:1）有最新版本的更新下载地址，不为null时原样传给updata更新接口

	·responseMessage  状态描述
```


### 8、更新版本 ###

- 下载新版本固件导入笔端，升级过程尽量保持手机亮屏状态；笔电量充足（30%以上）
- 因中间有重启笔，重连的过程，但是不排除遇到连接失败的情况，升级失败时，请开关笔端重新走一遍完整流程，依次检查更新、更新版本。
- 整个升级过程灯光灯光变化：绿灯（升级前待连接）->蓝灯（升级前连接）->红灯（升级准备进入boot模式）->蓝灯（升级中连接）->蓝灯亮灭转变（升级中传输数据）->绿灯（升级完成待连接）

```java

  	BlePenStreamManager.getInstance().updata(BleUpdateCallback bleUpdateCallback);
	·bleUpdateCallback 更新版本回调

    void onUpdateStart();
	·开始更新

    void onUpdating(int state, String updateMesage,int percentage);
    ·正在更新
    ·state 更新过程进度
                   1、下载解压缩
                   2、进入boot并重启重连
                   3、写入固件信息
                   4、重启应用
    ·updateMesage  更新信息描述
    ·percentage    更新进度百分比

    void onUpdateFinished(int state,String updateMesage);
    ·更新完成
    ·state：更新完成状态码
                   0、成功
                   1、下载解压缩失败
                   2、进入boot并重启重连失败
                   3、写入固件失败
                   4、重启应用失败
	·updateMesage：更新信息描述

```
### 9、判断蓝牙设备连接状态 ###

- 结束使用蓝牙笔时候断开设备

```java

	BlePenStreamManager.getInstance().isConnected(String mac);
	mac:判断设备mac是否连接

	BlePenStreamManager.getInstance().isConnected(bleDevice);
	bleDevice：判断是否连接的设备对象

	返回值 true 当前正在连接设备，false 未连接设备

```
### 11、额外功能 ###
	获取应用坐标(setStandMode): 正常模式下的书写和离线书写会存入缓存，点击获取应用坐标可以输出缓存中数据；
	获取悬浮坐标（setHoverMode）：悬浮模式:距离纸面1-3mm移动类似于鼠标效果，数据不可靠，呈现到界面上书写有丢笔迹现象。点击此按钮会中止从缓存输出数据，会实时同步上传悬浮数据；
	·注意·
	如果打开悬浮模式，断开连接前需发送获取应用坐标使笔复位，否则断开之后，笔处于悬浮模式下，不存储笔迹。

```java

 	BlePenStreamManager.getInstance().getPenInfo();

 	获取笔端信息电量、存储、版本号，通过笔数据流onRemainBatteryAndMemory/onVersionAndserialNumber返回

```java

 	BlePenStreamManager.getInstance().clearMemoryCache();

	获取笔端信息（内存百分比）后，如果不想要离线数据，调用此接口清空缓存


```java

 	BlePenStreamManager.getInstance().setHoverMode();

	打开悬浮模式(获取悬浮坐标)，发送此指令后，中止从缓存输出数据，会实时同步上传悬浮数据进入悬浮模式，此模式下书写，数据不可靠，呈现到界面上书写可能有丢笔迹现象
	使用场景：距离纸面1-3mm移动类似于鼠标效果。


```java

 	BlePenStreamManager.getInstance().setStandMode();

    关闭悬浮模式(获取应用坐标)：进入正常模式正常模式下的书写和离线书写会存入缓存，发送此指令后可以输出缓存中数据；
	·注意·
    1、刚进入程序，需获取应用坐标之后，坐标数据才会上传；
	2、如果打开悬浮模式，断开连接前需发送获取应用坐标使笔复位，否则断开之后，笔处于悬浮模式下，不存储笔迹。

```java

	BlePenStreamManager.getInstance().getPenRTC()

	获取笔端时间

```java


	BlePenStreamManager.getInstance().setPenRTC(long currentTime)

	设置笔端时间
	currentTime:要设置的时间毫秒数


```java

 	public BleManager enableLog(boolean enable)

 	调试日志是否打开？enable：true 打开，false 关闭

	log标签(TstudyBle_tag)


```java

	BlePenStreamManager.getInstance().setPenOFFTime(int minuteTime)

	设置笔端关机时间（只限于TDP001版本固件的笔使用，其他版本固件调用无效）
	minuteTime:要设置的定时关机时间，以分钟为单位，取值范围【0-360】；
        


```
##  五、更新日志  ##
### 1.0.0版本 ###

- 1、集成扫描连接点阵笔、应用与点阵笔互传数据及绘制笔迹功能。

### 1.0.1版本 ###

- 1、笔型号修改定为TD。
- 2、添加获取笔端信息接口获取电量和内存信息，并可以选择删除缓存数据。
- 3、添加悬浮模式(类似于鼠标效果，离纸面2-3mm移动到特定位置)，获取序列号。
- 4、笔数据接口增加笔开关机按钮断开指令，并加入重连功能。

### 1.0.2版本 ###

- 1、增加返回离线数据坐标接口，可以获取开始、结尾以及中间实时进度。
- 2、增加每一笔里面每一点的时间，毫秒级。
- 3、优化内部逻辑（写入队列）。

### 1.0.3版本 ###

- 1、优化内部逻辑（增加笔端应答机制）
- 2、解决因应答重复数据导致进度条不准确的问题

### 1.0.4版本 ###

- 1、优化内部逻辑
- 2、增加获取笔端时间和设置笔端时间的接口

### 1.0.5版本 ###

- 1、处理笔端异常情况，导致的数组越界的错误
- 2、返回信息接口，增加返回笔端序列号

### 1.0.6版本 ###

- 1、优化内部逻辑不严谨，导致的空指针
- 2、增加笔端OTA升级接口（检查更新和更新版本）
- 3、增加申请网络权限

### 1.0.7版本 ###

- 1、demo蓝牙搜索列表适配器逻辑及界面简单优化
- 2、取消设置MTU
- 3、OTA升级内部逻辑优化

### 1.0.8版本 ###

- 1、OTA升级检查更新接口（checkVersion）如有新版本返回固件下载地址，如已维护检查更新下载地址，可以忽略该接口，直接传下载地址url到更新接口（updata）。
- 2、更新接口（updata）增加参数传入下载地址url。

### 1.0.9版本 ###

- 1、优化蓝牙过滤机制。

### 1.1.0版本 ###

- 1、修改V2坐标偏移解析；
- 2、完善V2补包机制；
- 3、V1和V2兼容适配

### 1.1.1版本 ###

- 1、修改V2时间戳不准确的问题；
- 2、V1和V2兼容适配更新判断依据1.6.0

### 1.1.2版本 ###
- 1、demo笔迹粗细与设备适配；
- 2、demo添加“精简模式”，“编辑模式”，demo默认“调试模式”；
- 3、demo实现翻页保存功能；
- 4、demo升级固件增加安全提示，并且内部重连忽略过滤条件（解决笔AP和boot名字不一的问题）；
- 5、"YDP"版本固件SDK增加设置定时关机。


### 1.1.3版本 ###
- 1、优化内部逻辑，防止lib空指针导致主程序崩溃
- 2、优化内部逻辑，断开蓝牙连接清理gatt缓存，防止下次连接出现错误码133的gatt错误；

### 1.1.4版本 ###
- 1、优化回调笔端信息（软硬件版本号、内存使用、电量），防止根据内存使用输出离线时版本号取默认值

### 1.1.5版本 ###
- 1、优化P101扫描及获取设备名称设置设备名称，解决P101笔OTA升级红灯扫不到的问题。

### 1.2.0版本 ###
- 1、连接回调取消数据流接口的断开回调统一到连接回调并内部排重有且只有一次
（主动调用断开和按压笔端关机为主动断开，关闭系统蓝牙、笔自动关机、笔超出范围为被动断开）
- 2、数据流接口BlePenStreamCallback精简
- 3、解决由笔端数据异常引起的死循环问题。

### 1.2.1版本 ###
- 1、修复156版本数据解析压力值上报问题。
- 2、优化内部逻辑。

### 1.2.2版本 ###
- 1、修复OTA内部重连次数失效bug。

### 1.2.3版本 ###
- 1、优化补pendown机制（正常模式补pendown，悬浮模式不补pendown）；
- 2、优化内部逻辑（java1.8）；
- 3、OTA升级过程精细化。

### 1.2.4版本 ###
- 1、TD笔SDK适配TD-701名称扫描过滤修改。

### 1.2.5版本 ###
- 1、TD笔SDK适配TD-602/TD602B/TD-701固件更新地址；
- 2、demo更新绘制及模式选择。

### 1.2.6版本 ###
- 1、优化离线传输显示最大离线字节数不改变的问题；

### 1.2.7版本 ###
- 1、优化demo绘制效果；
- 2、修复读写队列可能为空的问题；
- 3、优化坐标输出回调onCoordDraw描述问题，防止页码null判断时过滤掉抬笔落笔事件。

