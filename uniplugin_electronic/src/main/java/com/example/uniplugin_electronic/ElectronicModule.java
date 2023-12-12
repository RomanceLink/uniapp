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

    //连接服务
    @UniJSMethod (uiThread = true)
    public void connectService(UniJSCallback callback){
        electronic = new Electronic(mUniSDKInstance.getContext(),callback);
    }


    //获取称重信息
    @UniJSMethod (uiThread = true)
    public void getResult(UniJSCallback callback) throws RemoteException {
        Log.e(TAG, "getResult");
        electronic.getResult(callback);
    }
    //获取称重状态
    @UniJSMethod (uiThread = true)
    public void getStatus(UniJSCallback callback) throws RemoteException {
        Log.e(TAG, "getStatus");
        electronic.getStatus(callback);
    }

    //获取称重状态
    @UniJSMethod (uiThread = true)
    public void getPrice(UniJSCallback callback) throws RemoteException {
        Log.e(TAG, "getPrice");
        electronic.getPrice(callback);
    }

}
