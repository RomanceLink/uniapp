package uts.sdk.modules.phironsunmiFace;

import android.app.Activity;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.alibaba.fastjson.JSONObject;

public class SunmiFaceDetectOverlay implements SunmiFaceCameraView.Listener {
    private final Activity activity;
    private final JSONObject options;
    private final FaceDetectResultCallback callback;
    private final boolean fullScreen;
    private boolean stopped = false;
    private FrameLayout container;
    private SunmiFaceCameraView cameraView;
    private Button detectButton;
    private Button closeButton;

    public SunmiFaceDetectOverlay(Activity activity, JSONObject options, FaceDetectResultCallback callback, boolean fullScreen) {
        this.activity = activity;
        this.options = options == null ? new JSONObject() : options;
        this.callback = callback;
        this.fullScreen = fullScreen;
    }

    public void start() {
        activity.runOnUiThread(() -> {
            stopped = false;
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            container = new FrameLayout(activity);
            boolean floatingWindowMode = isFloatingWindowMode(options, fullScreen);
            container.setBackgroundColor(resolveContainerBackgroundColor(floatingWindowMode));

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
                FrameLayout.LayoutParams closeParams = buildCloseButtonLayoutParams(cameraParams, floatingWindowMode);
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
                FrameLayout.LayoutParams detectParams = buildDetectButtonLayoutParams(cameraParams, floatingWindowMode);
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
            if (stopped) {
                return;
            }
            stopped = true;
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
                result.put("terminal", fullScreen);
                callback.onResult(result.toJSONString(), fullScreen);
            }
        });
    }

    public void startDetect() {
        activity.runOnUiThread(() -> {
            if (stopped) {
                return;
            }
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
        view.setGuideLayout(
                opts.containsKey("showGuideMask") && opts.getBooleanValue("showGuideMask"),
                opts.containsKey("guideBoxWidthRatio") ? opts.getFloatValue("guideBoxWidthRatio") : 0.62f,
                opts.containsKey("guideBoxHeightRatio") ? opts.getFloatValue("guideBoxHeightRatio") : 0.62f,
                opts.containsKey("guideOffsetXRatio") ? opts.getFloatValue("guideOffsetXRatio") : 0f,
                opts.containsKey("guideOffsetYRatio") ? opts.getFloatValue("guideOffsetYRatio") : 0f
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
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        boolean floatingWindowMode = isFloatingWindowMode(opts, false);
        int width;
        int height;
        if (opts.containsKey("width")) {
            width = opts.getIntValue("width");
        } else if (opts.containsKey("windowWidthRatio")) {
            width = Math.round(dm.widthPixels * clampRatio(opts.getFloatValue("windowWidthRatio"), 0.15f, 1f, 0.62f));
        } else {
            width = floatingWindowMode ? Math.round(dm.widthPixels * 0.62f) : ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if (opts.containsKey("height")) {
            height = opts.getIntValue("height");
        } else if (opts.containsKey("windowHeightRatio")) {
            height = Math.round(dm.heightPixels * clampRatio(opts.getFloatValue("windowHeightRatio"), 0.15f, 1f, 0.52f));
        } else {
            height = floatingWindowMode ? Math.round(dm.heightPixels * 0.52f) : ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if (width <= 0) width = floatingWindowMode ? Math.round(dm.widthPixels * 0.62f) : ViewGroup.LayoutParams.MATCH_PARENT;
        if (height <= 0) height = floatingWindowMode ? Math.round(dm.heightPixels * 0.52f) : ViewGroup.LayoutParams.MATCH_PARENT;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        if (floatingWindowMode) {
            params.gravity = Gravity.CENTER;
            int offsetX = opts.containsKey("windowOffsetX")
                    ? opts.getIntValue("windowOffsetX")
                    : Math.round(dm.widthPixels * (opts.containsKey("windowOffsetXRatio") ? opts.getFloatValue("windowOffsetXRatio") : 0f));
            int offsetY = opts.containsKey("windowOffsetY")
                    ? opts.getIntValue("windowOffsetY")
                    : Math.round(dm.heightPixels * (opts.containsKey("windowOffsetYRatio") ? opts.getFloatValue("windowOffsetYRatio") : 0f));
            params.leftMargin = offsetX;
            params.topMargin = offsetY;
        } else {
            params.gravity = Gravity.TOP | Gravity.START;
            params.leftMargin = opts.containsKey("offsetX") ? opts.getIntValue("offsetX") : 0;
            params.topMargin = opts.containsKey("offsetY") ? opts.getIntValue("offsetY") : 0;
        }
        return params;
    }

    private boolean isFloatingWindowMode(JSONObject opts, boolean full) {
        if (full) {
            return false;
        }
        if (opts == null) {
            return true;
        }
        if (opts.containsKey("floatingWindowMode")) {
            return opts.getBooleanValue("floatingWindowMode");
        }
        return true;
    }

    private int resolveContainerBackgroundColor(boolean floatingWindowMode) {
        if (options.containsKey("containerBackgroundColor")) {
            return parseColor(options.getString("containerBackgroundColor"), floatingWindowMode ? Color.TRANSPARENT : 0x22000000);
        }
        if (floatingWindowMode) {
            return Color.TRANSPARENT;
        }
        return fullScreen ? Color.BLACK : 0x22000000;
    }

    private FrameLayout.LayoutParams buildCloseButtonLayoutParams(FrameLayout.LayoutParams cameraParams, boolean floatingWindowMode) {
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        if (!floatingWindowMode) {
            closeParams.gravity = Gravity.TOP | Gravity.END;
            closeParams.topMargin = 80;
            closeParams.rightMargin = 24;
            return closeParams;
        }
        closeParams.gravity = Gravity.TOP | Gravity.START;
        closeParams.leftMargin = cameraParams.leftMargin + Math.max(0, cameraParams.width - dp(76));
        closeParams.topMargin = Math.max(0, cameraParams.topMargin - dp(18));
        return closeParams;
    }

    private FrameLayout.LayoutParams buildDetectButtonLayoutParams(FrameLayout.LayoutParams cameraParams, boolean floatingWindowMode) {
        FrameLayout.LayoutParams detectParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        if (!floatingWindowMode) {
            detectParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            detectParams.bottomMargin = 48;
            return detectParams;
        }
        detectParams.gravity = Gravity.TOP | Gravity.START;
        detectParams.leftMargin = cameraParams.leftMargin + Math.max(0, (cameraParams.width - dp(120)) / 2);
        detectParams.topMargin = cameraParams.topMargin + cameraParams.height + dp(16);
        return detectParams;
    }

    private int dp(int value) {
        return Math.round(activity.getResources().getDisplayMetrics().density * value);
    }

    private float clampRatio(float value, float min, float max, float fallback) {
        if (Float.isNaN(value) || value < min || value > max) {
            return fallback;
        }
        return value;
    }

    private int parseColor(String colorValue, int fallback) {
        if (colorValue == null || colorValue.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Color.parseColor(colorValue.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Override
    public void onStatus(JSONObject event) {
        emit(event, 2, false);
    }

    @Override
    public void onRecognize(JSONObject event) {
        if (stopped) {
            return;
        }
        emit(event, 0, true);
        if (fullScreen) {
            activity.runOnUiThread(() -> {
                if (stopped) {
                    return;
                }
                stopped = true;
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
        if (stopped) {
            return;
        }
        emit(event, 1, false);
    }

    private void emit(JSONObject event, int code, boolean success) {
        if (callback == null || stopped) {
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
        boolean terminal = fullScreen && (success || (event != null && "recognize_max_failures".equals(event.getString("eventType"))));
        result.put("terminal", terminal);
        callback.onResult(result.toJSONString(), terminal);
    }
}
