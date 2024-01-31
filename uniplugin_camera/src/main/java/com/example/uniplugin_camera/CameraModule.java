package com.example.uniplugin_camera;

import android.hardware.Camera;
import android.view.SurfaceHolder;

import com.alibaba.fastjson.JSONObject;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class CameraModule extends UniModule {

    private static final String TAG = "CameraModule";
    private CameraActivity cameraActivity;
    private SurfaceHolder colorSurfaceHolder;
    private SurfaceHolder infraredSurfaceHolder;

    private Camera.PictureCallback colorCallback;
    private Camera.PictureCallback infraredCallback;


    //初始化相机
    @UniJSMethod(uiThread = true)
    public void init(UniJSCallback callback) {
        cameraActivity.initCameras(colorSurfaceHolder, infraredSurfaceHolder);
        JSONObject data = new JSONObject();
        data.put("code", true);
        data.put("msg", "初始化成功");
        data.put("data", null);
        callback.invoke(data);
    }

    //拍照
    @UniJSMethod(uiThread = true)
    public void takePhoto(UniJSCallback callback) {
        cameraActivity.takePhotos(colorCallback,infraredCallback);
    }


    //关闭摄像头
    @UniJSMethod(uiThread = true)
    public void releaseCameras(UniJSCallback callback) {
        cameraActivity.takePhotos(colorCallback,infraredCallback);
    }

}
