package com.linkzill.bletest;

import android.text.InputFilter;
import android.util.Log;

import com.linkzill.bletest.base.ApplaicationBase;
import com.vise.baseble.ViseBle;
import com.vise.log.ViseLog;
import com.vise.log.inner.LogcatTree;

public class MyApplication extends ApplaicationBase {

    //过滤器
    public InputFilter[] cmdAsciiFilter = null;
    public InputFilter[] cmdHexFilter = null;

    @Override
    public void onCreate() {
        super.onCreate();

        //初始化过滤器
        initEditCmdFilter();

        //初始化日志打印
        ViseLog.getLogConfig()
                .configAllowLog(true)//是否输出日志
                .configShowBorders(true)//是否排版显示
                .configTagPrefix("ViseLog")//设置标签前缀
                .configFormatTag("%d{HH:mm:ss:SSS} %t %c{-5}")//个性化设置标签，默认显示包名
                .configLevel(Log.VERBOSE);//设置日志最小输出级别，默认Log.VERBOSE
        ViseLog.plant(new LogcatTree());//添加打印日志信息到Logcat的树

        //初始化BLE蓝牙实例
        //蓝牙相关配置修改
        ViseBle.config()
                .setScanTimeout(6000)//扫描超时时间，这里设置为永久扫描
                .setConnectTimeout(10 * 1000)//连接超时时间
                .setOperateTimeout(5 * 1000)//设置数据操作超时时间
                .setConnectRetryCount(10)//设置连接失败重试次数
                .setConnectRetryInterval(1000)//设置连接失败重试间隔时间
                .setOperateRetryCount(3)//设置数据操作失败重试次数
                .setOperateRetryInterval(1000)//设置数据操作失败重试间隔时间
                .setMaxConnectCount(3);//设置最大连接设备数量
        ViseBle.getInstance().init(this);
    }

    private void initEditCmdFilter(){
        cmdAsciiFilter = new InputFilter[]{};
        cmdHexFilter = new InputFilter[]{
                new LzoneInputFilter(getResources().getString(R.string.NAL_rule_hexval))
        };
    }
}
