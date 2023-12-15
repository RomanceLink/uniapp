package com.example.uniplugin_electronic;

import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class ElectronicModule extends UniModule {

    private static final String TAG = "ElectronicModule";
    private Electronic electronic;

    //连接服务
    @UniJSMethod(uiThread = true)
    public void connectService(UniJSCallback callback) {
        electronic = new Electronic(mUniSDKInstance.getContext(), callback);
    }

    //获取称重信息
    @UniJSMethod(uiThread = true)
    public void getRData(UniJSCallback callback) {
        Log.e(TAG, "getResult");
        electronic.getData(new Electronic.ScalePresenterCallback() {
            @Override
            public void getData(int net, int pnet, int status) {
                JSONObject data = new JSONObject();
                data.put("code", true);

                JSONObject data1 = new JSONObject();
                //这里返回称重状态
                data1.put("net", net);
                data1.put("pnet", pnet);
                data1.put("status", status);

                data.put("msg", "获取称重信息成功");
                data.put("data", data1);

                if (callback != null) {
                    //这里返回称重结果
                    callback.invoke(data);
                }

                electronic.cancelGetData(callback);
            }

            @Override
            public void isScaleCanUse(boolean isCan) {
                // 处理连接状态
            }
        });
    }


    //获取称重信息
    @UniJSMethod(uiThread = true)
    public void getResult(UniJSCallback callback) {
        Log.e(TAG, "getResult");
        electronic.getResult(callback);
    }

    //获取称重状态
    @UniJSMethod(uiThread = true)
    public void getStatus(UniJSCallback callback) {
        Log.e(TAG, "getStatus");
        electronic.getStatus(callback);
    }

    //获取价格
    @UniJSMethod(uiThread = true)
    public void getPrice(UniJSCallback callback) {
        Log.e(TAG, "getPrice");
        electronic.getPrice(callback);
    }

    //清零
    @UniJSMethod(uiThread = true)
    public void zero(UniJSCallback callback) {
        Log.e(TAG, "zero");
        electronic.zero(callback);
    }

    //去皮
    @UniJSMethod(uiThread = true)
    public void tare(UniJSCallback callback) {
        Log.e(TAG, "tare");
        electronic.tare(callback);
    }

    //数字去皮
    @UniJSMethod(uiThread = true)
    public void digitalTare(JSONObject json,UniJSCallback callback) {
        Log.e(TAG, "digitalTare");
        final int num = json.getIntValue("num");
        electronic.digitalTare(num,callback);
    }

    //取消获取数据
    @UniJSMethod(uiThread = true)
    public void cancelGetData(UniJSCallback callback) {
        Log.e(TAG, "cancelGetData");
        electronic.cancelGetDataOne(callback);
    }

    //读取加速度数据
    @UniJSMethod(uiThread = true)
    public void readAcceleData(UniJSCallback callback) {
        Log.e(TAG, "readAcceleData");
        electronic.readAcceleData(callback);
    }

    //读取标定按钮开关状态
    @UniJSMethod(uiThread = true)
    public void getCalStatus(UniJSCallback callback) {
        Log.e(TAG, "getCalStatus");
        electronic.getCalStatus(callback);
    }

    //获取铅封状态
    @UniJSMethod(uiThread = true)
    public void readSealState(UniJSCallback callback) {
        Log.e(TAG, "readSealState");
        electronic.readSealState(callback);
    }

    //设置单价
    @UniJSMethod(uiThread = true)
    public void setUnitPrice(JSONObject json,UniJSCallback callback) {
        Log.e(TAG, "setUnitPrice");
        final String unitPrice = json.getString("unitPrice");
        electronic.setUnitPrice(unitPrice,callback);
    }

    //获取当前已经设置的单价
    @UniJSMethod(uiThread = true)
    public void getUnitPrice(UniJSCallback callback) {
        Log.e(TAG, "getUnitPrice");
        electronic.getUnitPrice(callback);
    }

    //设置价格计算时的重量单位
    @UniJSMethod(uiThread = true)
    public void setUnit(JSONObject json,UniJSCallback callback) {
        Log.e(TAG, "setUnit");
        final int num = json.getIntValue("num");
        electronic.setUnit(num,callback);
    }

    //获取当前价格计算的重量单位
    @UniJSMethod(uiThread = true)
    public void getUnit(UniJSCallback callback) {
        Log.e(TAG, "getUnit");
        electronic.getUnit(callback);
    }

    //重启
    @UniJSMethod(uiThread = true)
    public void restart(UniJSCallback callback) {
        Log.e(TAG, "restart");
        electronic.restart(callback);
    }

}
