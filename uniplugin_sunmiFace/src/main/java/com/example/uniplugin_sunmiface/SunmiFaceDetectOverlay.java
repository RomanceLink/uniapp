package com.example.uniplugin_sunmiface;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.alibaba.fastjson.JSONObject;

import io.dcloud.feature.uniapp.bridge.UniJSCallback;

public class SunmiFaceDetectOverlay implements SunmiFaceCameraView.Listener {
    private final Activity activity;
    private final JSONObject options;
    private final UniJSCallback callback;
    private final boolean fullScreen;
    private FrameLayout container;
    private SunmiFaceCameraView cameraView;
    private Button detectButton;
    private Button closeButton;

    public SunmiFaceDetectOverlay(Activity activity, JSONObject options, UniJSCallback callback, boolean fullScreen) {
        this.activity = activity;
        this.options = options == null ? new JSONObject() : options;
        this.callback = callback;
        this.fullScreen = fullScreen;
    }

    public void start() {
        activity.runOnUiThread(() -> {
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            container = new FrameLayout(activity);
            container.setBackgroundColor(fullScreen ? Color.BLACK : 0x22000000);

            cameraView = new SunmiFaceCameraView(activity);
            cameraView.setListener(this);
            applyOptions(cameraView, options);

            FrameLayout.LayoutParams cameraParams = buildCameraLayoutParams(options, fullScreen);
            decor.addView(container, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            container.addView(cameraView, cameraParams);

            if (!options.containsKey("showCloseButton") || options.getBooleanValue("showCloseButton")) {
                closeButton = new Button(activity);
                closeButton.setText("关闭");
                closeButton.setOnClickListener(v -> stop("closed", "已关闭人脸识别"));
                FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                closeParams.gravity = Gravity.TOP | Gravity.END;
                closeParams.topMargin = 80;
                closeParams.rightMargin = 24;
                container.addView(closeButton, closeParams);
            } else {
                closeButton = null;
            }

            boolean showStartButton = options.containsKey("showStartButton")
                    ? options.getBooleanValue("showStartButton")
                    : (options.containsKey("autoStartDetect") && !options.getBooleanValue("autoStartDetect"));
            if (showStartButton) {
                detectButton = new Button(activity);
                detectButton.setText("开始检测");
                detectButton.setOnClickListener(v -> startDetect());
                FrameLayout.LayoutParams detectParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                detectParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                detectParams.bottomMargin = 48;
                container.addView(detectButton, detectParams);
            } else {
                detectButton = null;
            }

            cameraView.setRunning(true);
            cameraView.setDetecting(!options.containsKey("autoStartDetect") || options.getBooleanValue("autoStartDetect"));
        });
    }

    public void stop(String eventType, String message) {
        activity.runOnUiThread(() -> {
            if (cameraView != null) {
                cameraView.destroyView();
                cameraView = null;
            }
            if (container != null) {
                ViewGroup parent = (ViewGroup) container.getParent();
                if (parent != null) {
                    parent.removeView(container);
                }
                container = null;
            }
            if (callback != null) {
                JSONObject result = new JSONObject();
                result.put("code", 9);
                result.put("success", false);
                result.put("eventType", eventType);
                result.put("message", message);
                if (fullScreen) {
                    callback.invoke(result);
                } else {
                    callback.invokeAndKeepAlive(result);
                }
            }
        });
    }

    public void startDetect() {
        activity.runOnUiThread(() -> {
            if (cameraView != null) {
                cameraView.setDetecting(true);
            }
            if (detectButton != null) {
                detectButton.setVisibility(View.GONE);
            }
        });
    }

    private void applyOptions(SunmiFaceCameraView view, JSONObject opts) {
        view.setCameraFacing(resolveCameraFacing(opts));
        view.setDisplayOrientationDeg(opts.containsKey("displayOrientationDeg") ? opts.getIntValue("displayOrientationDeg") : 360);
        view.setAnalyzeIntervalMs(resolveAnalyzeIntervalMs(opts));
        view.setPreviewDecodeMaxSize(opts.containsKey("previewDecodeMaxSize") ? opts.getIntValue("previewDecodeMaxSize") : 640);
        view.setPredictMode(opts.containsKey("predictMode") ? opts.getIntValue("predictMode") : 3);
        view.setLivenessMode(opts.containsKey("livenessMode") ? opts.getIntValue("livenessMode") : 0);
        view.setQualityMode(opts.containsKey("qualityMode") ? opts.getIntValue("qualityMode") : 0);
        view.setMaxFaceCount(opts.containsKey("maxFaceCount") ? opts.getIntValue("maxFaceCount") : 1);
        view.setMinFaceSize(opts.containsKey("minFaceSize") ? opts.getIntValue("minFaceSize") : 0);
        view.setDistanceThreshold(opts.containsKey("distanceThreshold") ? opts.getFloatValue("distanceThreshold") : 0f);
        view.setFaceScoreThreshold(opts.containsKey("faceScoreThreshold") ? opts.getFloatValue("faceScoreThreshold") : 0f);
        view.setThreadNum(opts.containsKey("threadNum") ? opts.getIntValue("threadNum") : 0);
        view.setDbPath(opts.getString("dbPath"));
        view.setLicensePath(opts.getString("licensePath"));
        view.setAppId(opts.getString("appId"));
        view.setForceRefresh(opts.containsKey("forceRefresh") && opts.getBooleanValue("forceRefresh"));
        view.setAutoStopOnRecognize(opts.containsKey("autoStopOnRecognize") ? opts.getBooleanValue("autoStopOnRecognize") : fullScreen);
        view.setShowStatusText(!opts.containsKey("showStatusText") || opts.getBooleanValue("showStatusText"));
        view.setMaxRecognizeFailures(opts.containsKey("maxRecognizeFailures") ? opts.getIntValue("maxRecognizeFailures") : 0);
        view.setGuideStyle(
                opts.containsKey("showCircleGuide") && opts.getBooleanValue("showCircleGuide"),
                !opts.containsKey("showSquareGuide") || opts.getBooleanValue("showSquareGuide"),
                opts.containsKey("showRedLineGuide") && opts.getBooleanValue("showRedLineGuide")
        );
    }

    private String resolveCameraFacing(JSONObject opts) {
        if (opts.containsKey("cameraFacing")) {
            return opts.getString("cameraFacing");
        }
        if (opts.containsKey("cameraId")) {
            return opts.getIntValue("cameraId") == 1 ? "front" : "back";
        }
        return "front";
    }

    private int resolveAnalyzeIntervalMs(JSONObject opts) {
        if (opts.containsKey("analyzeIntervalMs")) {
            return Math.max(300, opts.getIntValue("analyzeIntervalMs"));
        }
        if (opts.containsKey("interval")) {
            return Math.max(300, opts.getIntValue("interval") * 1000);
        }
        return 1000;
    }

    private FrameLayout.LayoutParams buildCameraLayoutParams(JSONObject opts, boolean full) {
        if (full) {
            return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        int width = opts.containsKey("width") ? opts.getIntValue("width") : ViewGroup.LayoutParams.MATCH_PARENT;
        int height = opts.containsKey("height") ? opts.getIntValue("height") : ViewGroup.LayoutParams.MATCH_PARENT;
        if (width <= 0) width = ViewGroup.LayoutParams.MATCH_PARENT;
        if (height <= 0) height = ViewGroup.LayoutParams.MATCH_PARENT;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = opts.containsKey("offsetX") ? opts.getIntValue("offsetX") : 0;
        params.topMargin = opts.containsKey("offsetY") ? opts.getIntValue("offsetY") : 0;
        return params;
    }

    @Override
    public void onStatus(JSONObject event) {
        emit(event, 2, false);
    }

    @Override
    public void onRecognize(JSONObject event) {
        emit(event, 0, true);
        if (fullScreen) {
            activity.runOnUiThread(() -> {
                if (cameraView != null) {
                    cameraView.destroyView();
                    cameraView = null;
                }
                if (container != null) {
                    ViewGroup parent = (ViewGroup) container.getParent();
                    if (parent != null) {
                        parent.removeView(container);
                    }
                    container = null;
                }
            });
        }
    }

    @Override
    public void onError(JSONObject event) {
        emit(event, 1, false);
    }

    private void emit(JSONObject event, int code, boolean success) {
        if (callback == null) {
            return;
        }
        JSONObject result = new JSONObject();
        result.put("code", code);
        result.put("success", success);
        if (event != null) {
            result.putAll(event);
            result.put("data", event);
        } else {
            result.put("data", null);
        }
        if (fullScreen && (success || (event != null && "recognize_max_failures".equals(event.getString("eventType"))))) {
            callback.invoke(result);
        } else {
            callback.invokeAndKeepAlive(result);
        }
    }
}
