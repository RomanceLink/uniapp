package com.example.uniplugin_electronic;

import android.content.Context;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.sunmi.scalelibrary.ScaleManager;
import com.sunmi.scalelibrary.ScaleResult;

import java.util.concurrent.atomic.AtomicBoolean;

public class Electronic {

    Context mContext;
    private ScaleManager scaleManager;
    private ScaleManager.ScaleServiceConnection scaleServiceConnection;

    public Electronic(@NonNull Context context) {
        mContext = context;
        scaleManager = ScaleManager.getInstance(context);
        scaleServiceConnection = new ScaleManager.ScaleServiceConnection() {
            @Override
            public void onServiceConnected() {
            }

            @Override
            public void onServiceDisconnect() {
            }
        };
    }



    //获取称重信息

    public JSONObject getResult() throws RemoteException {
        JSONObject data = new JSONObject();

        scaleManager.getData(new ScaleResult() {

            @Override
            public void getResult(int net, int tare, boolean isStable) {
                //这里返回称重结果
                data.put("net", net);
                data.put("tare", tare);
                data.put("isStable", isStable);
            }

            @Override
            public void getStatus(boolean isLightWeight, boolean overload, boolean clearZeroErr, boolean calibrationErr) {

            }

            @Override
            public void getPrice(int net, int tare, int unit, String unitPrice, String totalPrice, boolean isStable, boolean isLightWeight) {

            }

        });
        return data;
    }
    //获取称重状态

    public JSONObject getStatus() throws RemoteException {
        JSONObject data = new JSONObject();

        scaleManager.getData(new ScaleResult() {

            @Override
            public void getResult(int net, int tare, boolean isStable) {

            }

            @Override
            public void getStatus(boolean isLightWeight, boolean overload, boolean clearZeroErr, boolean calibrationErr) {
                //这里返回称重状态
                data.put("isLightWeight", isLightWeight);
                data.put("overload", overload);
                data.put("clearZeroErr", clearZeroErr);
                data.put("calibrationErr", calibrationErr);

            }

            @Override
            public void getPrice(int net, int tare, int unit, String unitPrice, String totalPrice, boolean isStable, boolean isLightWeight) {

            }

        });
        return data;

    }

    //获取称重状态
    public JSONObject getPrice() throws RemoteException {
        JSONObject data = new JSONObject();

        scaleManager.getData(new ScaleResult() {

            @Override
            public void getResult(int net, int tare, boolean isStable) {

            }

            @Override
            public void getStatus(boolean isLightWeight, boolean overload, boolean clearZeroErr, boolean calibrationErr) {

            }

            @Override
            public void getPrice(int net, int tare, int unit, String unitPrice, String totalPrice, boolean isStable, boolean isLightWeight) {
                //这里返回计价结果
                data.put("net", net);
                data.put("tare", tare);
                data.put("unit", unit);
                data.put("unitPrice", unitPrice);
                data.put("totalPrice", totalPrice);
                data.put("isStable", isStable);
                data.put("isLightWeight", isLightWeight);
            }


        });

        return data;

    }

}
