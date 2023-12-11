package com.example.uniplugin_electronic;

import android.os.RemoteException;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class ElectronicModule extends UniModule {

    private static final String TAG = "ElectronicModule";

    private Electronic electronic;

    @UniJSMethod(uiThread = true)
    public void add(JSONObject json, UniJSCallback callback){
        final int a = json.getIntValue("a");
        final int b = json.getIntValue("b");
        callback.invoke(new JSONObject(){{
            put("code",200);
            put("result",a+b);
        }});
    }

    //连接服务
    @UniJSMethod (uiThread = true)
    public void init(UniJSCallback callback){
        electronic = new Electronic(mUniSDKInstance.getContext());
        if(callback != null) {
            JSONObject data = new JSONObject();
            data.put("code","初始化成功");
            callback.invoke(data);
        }
    }


    //获取称重信息
    @UniJSMethod (uiThread = true)
    public void getResult(UniJSCallback callback) throws RemoteException {
        Log.e(TAG, "getResult");

        JSONObject result = electronic.getResult();
        if(callback != null) {
            callback.invoke(result);
        }

    }
    //获取称重状态
    @UniJSMethod (uiThread = true)
    public void getStatus(UniJSCallback callback) throws RemoteException {
        Log.e(TAG, "getStatus");
        JSONObject result = electronic.getStatus();
        if(callback != null) {
            callback.invoke(result);
        }
    }

    //获取称重状态
    @UniJSMethod (uiThread = true)
    public void getPrice(UniJSCallback callback) throws RemoteException {
        Log.e(TAG, "getPrice");
        JSONObject result = electronic.getPrice();
        if (callback != null) {
            callback.invoke(result);
        }
    }

}
