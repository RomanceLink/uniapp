package com.example.uniplugin_sunmiface;

import android.content.Context;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.dcloud.feature.uniapp.UniSDKInstance;
import io.dcloud.feature.uniapp.ui.action.AbsComponentData;
import io.dcloud.feature.uniapp.ui.component.AbsVContainer;
import io.dcloud.feature.uniapp.ui.component.UniComponent;
import io.dcloud.feature.uniapp.ui.component.UniComponentProp;

public class SunmiFaceCameraComponent extends UniComponent<SunmiFaceCameraView> implements SunmiFaceCameraView.Listener {

    public SunmiFaceCameraComponent(UniSDKInstance instance, AbsVContainer parent, AbsComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
    }

    @Override
    protected SunmiFaceCameraView initComponentHostView(Context context) {
        SunmiFaceCameraView view = new SunmiFaceCameraView(context);
        view.setListener(this);
        return view;
    }

    @UniComponentProp(name = "running")
    public void setRunning(boolean running) {
        getHostView().setRunning(running);
    }

    @UniComponentProp(name = "detecting")
    public void setDetecting(boolean detecting) {
        getHostView().setDetecting(detecting);
    }

    @UniComponentProp(name = "cameraFacing")
    public void setCameraFacing(String cameraFacing) {
        getHostView().setCameraFacing(cameraFacing);
    }

    @UniComponentProp(name = "displayOrientationDeg")
    public void setDisplayOrientationDeg(int displayOrientationDeg) {
        getHostView().setDisplayOrientationDeg(displayOrientationDeg);
    }

    @UniComponentProp(name = "analyzeIntervalMs")
    public void setAnalyzeIntervalMs(int analyzeIntervalMs) {
        getHostView().setAnalyzeIntervalMs(analyzeIntervalMs);
    }

    @UniComponentProp(name = "previewDecodeMaxSize")
    public void setPreviewDecodeMaxSize(int previewDecodeMaxSize) {
        getHostView().setPreviewDecodeMaxSize(previewDecodeMaxSize);
    }

    @UniComponentProp(name = "predictMode")
    public void setPredictMode(int predictMode) {
        getHostView().setPredictMode(predictMode);
    }

    @UniComponentProp(name = "livenessMode")
    public void setLivenessMode(int livenessMode) {
        getHostView().setLivenessMode(livenessMode);
    }

    @UniComponentProp(name = "qualityMode")
    public void setQualityMode(int qualityMode) {
        getHostView().setQualityMode(qualityMode);
    }

    @UniComponentProp(name = "maxFaceCount")
    public void setMaxFaceCount(int maxFaceCount) {
        getHostView().setMaxFaceCount(maxFaceCount);
    }

    @UniComponentProp(name = "minFaceSize")
    public void setMinFaceSize(int minFaceSize) {
        getHostView().setMinFaceSize(minFaceSize);
    }

    @UniComponentProp(name = "distanceThreshold")
    public void setDistanceThreshold(float distanceThreshold) {
        getHostView().setDistanceThreshold(distanceThreshold);
    }

    @UniComponentProp(name = "dbPath")
    public void setDbPath(String dbPath) {
        getHostView().setDbPath(dbPath);
    }

    @UniComponentProp(name = "autoStopOnRecognize")
    public void setAutoStopOnRecognize(boolean autoStopOnRecognize) {
        getHostView().setAutoStopOnRecognize(autoStopOnRecognize);
    }

    @Override
    public void onStatus(JSONObject event) {
        fireEvent("onStatus", wrapDetail(event));
    }

    @Override
    public void onRecognize(JSONObject event) {
        fireEvent("onRecognize", wrapDetail(event));
    }

    @Override
    public void onError(JSONObject event) {
        fireEvent("onError", wrapDetail(event));
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        getHostView().setRunning(false);
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
        getHostView().destroyView();
    }

    private Map<String, Object> wrapDetail(JSONObject event) {
        Map<String, Object> params = new HashMap<>();
        params.put("detail", event == null ? new HashMap<>() : event);
        return params;
    }
}
