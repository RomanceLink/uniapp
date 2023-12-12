package com.example.uniplugin_electronic;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.sunmi.scalelibrary.ScaleManager;
import com.sunmi.scalelibrary.ScaleResult;

import io.dcloud.feature.uniapp.bridge.UniJSCallback;

public class Electronic {

    private static final String TAG = "Electronic";
    Context mContext;
    private ScaleManager scaleManager;
    private ScaleManager.ScaleServiceConnection scaleServiceConnection;

    // 添加一个标志来指示连接状态
    private boolean isServiceConnected = false;

    public Electronic(@NonNull Context context, final UniJSCallback jsCallback) {
        mContext = context;
        scaleManager = ScaleManager.getInstance(context);

        Log.e(TAG, "scaleManager");

        scaleServiceConnection = new ScaleManager.ScaleServiceConnection() {
            @Override
            public void onServiceConnected() {
                Log.e(TAG, "onServiceConnected");
                // 设置连接状态为true
                isServiceConnected = true;
                if (jsCallback != null) {
                    JSONObject data = new JSONObject();
                    data.put("code", "初始化连接成功");
                    jsCallback.invoke(data);
                }
            }

            @Override
            public void onServiceDisconnect() {
                Log.e(TAG, "onServiceDisconnect");
                // 设置连接状态为false
                isServiceConnected = false;
                if (jsCallback != null) {
                    JSONObject data = new JSONObject();
                    data.put("code", "断开连接");
                    jsCallback.invoke(data);
                }
            }
        };

        // 明确调用 connectService 方法
        scaleManager.connectService(scaleServiceConnection);

        Log.e(TAG, "电子称未连接");

        // 添加一个延迟以确保 onServiceConnected 方法执行
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 在这里检查连接状态
                checkConnectionStatus(jsCallback);
            }
        }, 6000); // 2000毫秒延迟示例

    }

    // 在服务连接成功后检查连接状态
    private void checkConnectionStatus(UniJSCallback jsCallback) {
        if (!isServiceConnected) {
            Log.e(TAG, "电子称未连接");
            if (jsCallback != null) {
                JSONObject data = new JSONObject();
                data.put("code", "初始化连接失败");
                jsCallback.invoke(data);
            }
        }
    }


    // 处理未连接错误
    private void handleNotConnectedError(UniJSCallback jsCallback) {
        Log.e(TAG, "电子称未连接");
        if (jsCallback != null) {
            JSONObject data = new JSONObject();
            data.put("code", "电子称未连接");
            jsCallback.invoke(data);
        }
    }


    //获取称重信息

    public void getResult(final UniJSCallback jsCallback) {

        // 在这里检查连接状态
        if (!isServiceConnected) {
            handleNotConnectedError(jsCallback);
            return; // 不再执行后面的代码
        }

        try {
            scaleManager.getData(new ScaleResult() {
                @Override
                public void getResult(int net, int tare, boolean isStable) {
                    JSONObject data = new JSONObject();
                    data.put("net", net);
                    data.put("tare", tare);
                    data.put("isStable", isStable);
                    if (jsCallback != null) {
                        //这里返回称重结果
                        jsCallback.invoke(data);
                    }
                }

                @Override
                public void getStatus(boolean isLightWeight, boolean overload, boolean clearZeroErr, boolean calibrationErr) {

                }

                @Override
                public void getPrice(int net, int tare, int unit, String unitPrice, String totalPrice, boolean isStable, boolean isLightWeight) {

                }

            });

        } catch (RemoteException e) {
            handleNotConnectedError(jsCallback);
        }
    }
    //获取称重状态

    public void getStatus(final UniJSCallback jsCallback) {
        // 在这里检查连接状态
        if (!isServiceConnected) {
            handleNotConnectedError(jsCallback);
            return; // 不再执行后面的代码
        }

        try {
            scaleManager.getData(new ScaleResult() {

                @Override
                public void getResult(int net, int tare, boolean isStable) {

                }

                @Override
                public void getStatus(boolean isLightWeight, boolean overload, boolean clearZeroErr, boolean calibrationErr) {
                    JSONObject data = new JSONObject();
                    //这里返回称重状态
                    data.put("isLightWeight", isLightWeight);
                    data.put("overload", overload);
                    data.put("clearZeroErr", clearZeroErr);
                    data.put("calibrationErr", calibrationErr);
                    if (jsCallback != null) {
                        //这里返回称重结果
                        jsCallback.invoke(data);
                    }
                }

                @Override
                public void getPrice(int net, int tare, int unit, String unitPrice, String totalPrice, boolean isStable, boolean isLightWeight) {

                }

            });

        } catch (RemoteException e) {
            handleNotConnectedError(jsCallback);
        }
    }

    //获取称重状态
    public void getPrice(final UniJSCallback jsCallback) {

        // 在这里检查连接状态
        if (!isServiceConnected) {
            handleNotConnectedError(jsCallback);
            return; // 不再执行后面的代码
        }

        try {
            scaleManager.getData(new ScaleResult() {

                @Override
                public void getResult(int net, int tare, boolean isStable) {

                }

                @Override
                public void getStatus(boolean isLightWeight, boolean overload, boolean clearZeroErr, boolean calibrationErr) {

                }

                @Override
                public void getPrice(int net, int tare, int unit, String unitPrice, String totalPrice, boolean isStable, boolean isLightWeight) {
                    JSONObject data = new JSONObject();
                    //这里返回计价结果
                    data.put("net", net);
                    data.put("tare", tare);
                    data.put("unit", unit);
                    data.put("unitPrice", unitPrice);
                    data.put("totalPrice", totalPrice);
                    data.put("isStable", isStable);
                    data.put("isLightWeight", isLightWeight);
                    if (jsCallback != null) {
                        //这里返回称重结果
                        jsCallback.invoke(data);
                    }
                }

            });

        } catch (RemoteException e) {
            handleNotConnectedError(jsCallback);
        }

    }

}
