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
            public void getData(int net, int pnet, int statu) {
                JSONObject data = new JSONObject();
                //这里返回称重状态
                data.put("net", net);
                data.put("pnet", pnet);
                data.put("statu", statu);
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

    //获取称重状态
    @UniJSMethod(uiThread = true)
    public void getPrice(UniJSCallback callback) {
        Log.e(TAG, "getPrice");
        electronic.getPrice(callback);
    }

    //获取称重状态
    @UniJSMethod(uiThread = true)
    public void zero(UniJSCallback callback) {
        Log.e(TAG, "getPrice");
        electronic.zero(callback);
    }
}
