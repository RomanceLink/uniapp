package com.example.uniplugin_electronic;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.sunmi.electronicscaleservice.ScaleCallback;
import com.sunmi.scalelibrary.ScaleManager;
import com.sunmi.scalelibrary.ScaleResult;

import io.dcloud.feature.uniapp.bridge.UniJSCallback;

public class Electronic {

    private static final String TAG = "Electronic";
    Context mContext;
    private ScaleManager scaleManager;

    // 添加一个标志来指示连接状态
    private boolean isServiceConnected = false;

    public static int net;
    public int pnet;//皮重

    private int status = -1;


    public ScalePresenterCallback callback;

    public void getData(final ScalePresenterCallback scalePresenterCallback) {
        if (isServiceConnected) {
            try {
                scaleManager.getData(new ScaleCallback.Stub() {
                    @Override
                    public void getData(final int i, int i1, final int i2) throws RemoteException {
                        net = i;
                        pnet = i1;
                        status = i2;
                        // 在这里调用传入的回调方法
                        scalePresenterCallback.getData(i, pnet, i2);
                        scalePresenterCallback.isScaleCanUse(true);
                    }

                    @Override
                    public void error(int errorCode) throws RemoteException {
                        // 处理错误
                    }

                    @Override
                    public void getPrice(int net, int tare, int unit, String unitPrice, String totalPrice, int status) throws RemoteException {
                        // 处理计价逻辑
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            // 处理未连接的情况
            scalePresenterCallback.isScaleCanUse(false);
        }
    }

    public interface ScalePresenterCallback {
        void getData(int net, int pnet, int statu);

        void isScaleCanUse(boolean isCan);
    }

    //连接电子秤服务
    private void connectScaleService(final UniJSCallback jsCallback) {
        scaleManager = ScaleManager.getInstance(mContext);
        scaleManager.connectService(new ScaleManager.ScaleServiceConnection() {
            @Override
            public void onServiceConnected() {
                Log.e(TAG, "onServiceConnected");

                // 设置连接状态为true
                isServiceConnected = true;
                if (jsCallback != null) {
                    JSONObject data = new JSONObject();
                    data.put("code", true);
                    data.put("msg", "初始化连接成功");
                    data.put("data", null);
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
                    data.put("code", false);
                    data.put("msg", "断开连接");
                    data.put("data", null);
                    jsCallback.invoke(data);
                }
            }
        });
    }

    public Electronic(@NonNull Context context, final UniJSCallback jsCallback) {
        mContext = context;
        connectScaleService(jsCallback);
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
                data.put("code", false);
                data.put("msg", "初始化连接失败");
                data.put("data", null);
                jsCallback.invoke(data);
            }
        }
    }


    // 处理未连接错误
    private void handleNotConnectedError(UniJSCallback jsCallback) {
        Log.e(TAG, "电子称未连接");
        if (jsCallback != null) {
            JSONObject data = new JSONObject();
            data.put("code", false);
            data.put("msg", "电子称未连接");
            data.put("data", null);
            jsCallback.invoke(data);
        }
    }

    private void getScaleData() {
        try {
            scaleManager.getData(new ScaleCallback.Stub() {
                @Override
                public void getData(final int i, int i1, final int i2) throws RemoteException {
                    // i = 净重量 单位 克 ，i1 = 皮重量 单位 克 ，i2 = 稳定状态  1 为稳定。具体其他状态请参考商米开发者文档
                    net = i;
                    pnet = i1;
                    status = i2;
                    callback.getData(i, pnet, i2);
                    callback.isScaleCanUse(true);
                }

                @Override
                public void error(int errorCode) throws RemoteException {

                }

                @Override
                public void getPrice(int net, int tare, int unit, String unitPrice, String totalPrice, int status) throws RemoteException {

                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    //获取称重信息

    public void getResult(final UniJSCallback jsCallback) {

        // 在这里检查连接状态
        if (!isServiceConnected) {
            handleNotConnectedError(jsCallback);
            connectScaleService(jsCallback);
            return; // 不再执行后面的代码
        }

        try {
            scaleManager.getData(new ScaleResult() {
                @Override
                public void getResult(int net, int tare, boolean isStable) {
                    JSONObject data = new JSONObject();

                    data.put("code", true);

                    JSONObject data1 = new JSONObject();
                    data1.put("net", net);
                    data1.put("tare", tare);
                    data1.put("isStable", isStable);

                    data.put("msg", "获取称重信息成功");
                    data.put("data", data1);

                    if (jsCallback != null) {
                        //这里返回称重结果
                        jsCallback.invoke(data);
                    }

                    cancelGetData(jsCallback);
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
            connectScaleService(jsCallback);
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

                    data.put("code", true);

                    JSONObject data1 = new JSONObject();
                    //这里返回称重状态
                    data1.put("isLightWeight", isLightWeight);
                    data1.put("overload", overload);
                    data1.put("clearZeroErr", clearZeroErr);
                    data1.put("calibrationErr", calibrationErr);

                    data.put("msg", "获取称重状态成功");
                    data.put("data", data1);

                    if (jsCallback != null) {
                        //这里返回称重结果
                        jsCallback.invoke(data);
                    }
                    cancelGetData(jsCallback);
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
            connectScaleService(jsCallback);
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
                    data.put("code", true);

                    JSONObject data1 = new JSONObject();
                    //这里返回计价结果
                    data1.put("net", net);
                    data1.put("tare", tare);
                    data1.put("unit", unit);
                    data1.put("unitPrice", unitPrice);
                    data1.put("totalPrice", totalPrice);
                    data1.put("isStable", isStable);
                    data1.put("isLightWeight", isLightWeight);


                    data.put("msg", "获取价格成功");
                    data.put("data", data1);

                    if (jsCallback != null) {
                        //这里返回称重结果
                        jsCallback.invoke(data);
                    }
                    cancelGetData(jsCallback);
                }

            });

        } catch (RemoteException e) {
            handleNotConnectedError(jsCallback);
        }

    }

    public void zero(UniJSCallback jsCallback) {
        // 在这里检查连接状态
        if (!isServiceConnected) {
            handleNotConnectedError(jsCallback);
            connectScaleService(jsCallback);
            return; // 不再执行后面的代码
        }
        try {
            scaleManager.zero();
            JSONObject data = new JSONObject();
            data.put("code", true);
            data.put("msg", "归零成功");
            data.put("data", null);
            jsCallback.invoke(data);
        } catch (RemoteException e) {
            handleNotConnectedError(jsCallback);
        }
    }

    public void cancelGetData(UniJSCallback jsCallback) {
        // 在这里检查连接状态
        if (!isServiceConnected) {
            handleNotConnectedError(jsCallback);
            connectScaleService(jsCallback);
            return; // 不再执行后面的代码
        }
        try {
            scaleManager.cancelGetData();
        } catch (RemoteException e) {
            handleNotConnectedError(jsCallback);
        }
    }

    public void tare(UniJSCallback jsCallback) {
        // 在这里检查连接状态
        if (!isServiceConnected) {
            handleNotConnectedError(jsCallback);
            connectScaleService(jsCallback);
            return; // 不再执行后面的代码
        }
        try {
            scaleManager.tare();
            JSONObject data = new JSONObject();
            data.put("code", true);
            data.put("msg", "去皮成功");
            data.put("data", null);
            jsCallback.invoke(data);
        } catch (RemoteException e) {
            handleNotConnectedError(jsCallback);
        }
    }
}
