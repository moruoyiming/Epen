    var GameSDK = {
            // CallBack
            callBacks: {},
            // Internal -- Call Native
            callNative: function (cmd, param) {
                sdk.post(cmd,param)
            },

            nativeCallback: function (cmd, param) {
//            [INFO:CONSOLE(10)] "cmd={"memoryPercent":"100","batteryPercent":"51","callbackname":"onBattery"}，param=undefined",
                console.log("cmd="+cmd+"，param="+param);
                var func = this.callBacks[cmd];
                if (func) {
                    func(JSON.parse(param));
                }
            },

            registerCallback: function (cmd, func) {
                this.callBacks[cmd] = func;
            },

            // 设置回调函数
            setOnBatteryCB: function (func) {
                this.registerCallback('onBattery', func);
            },

            setOnDrawCB: function (func) {
                this.registerCallback('onDraw', func);
            },
            // 退出游戏
            // 参数:
            //   reason: int 退出原因: 1 - 正常退出，2-异常退出
            showToast: function (msg) {
            this.callNative('showToast',{message:msg});
            },
            showDialog: function (msg) {
            this.callNative('showDialog',{message:msg});
            },
        };
