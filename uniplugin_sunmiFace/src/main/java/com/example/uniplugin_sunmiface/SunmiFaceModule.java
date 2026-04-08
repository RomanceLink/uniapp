package com.example.uniplugin_sunmiface;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sunmi.authorizelibrary.AuthorizeCallBack;
import com.sunmi.authorizelibrary.SunmiAuthorizeSDK;
import com.sunmi.authorizelibrary.bean.AuthorizeResult;
import com.sunmi.authorizelibrary.constants.ErrorCode;
import com.sunmi.facelib.SunmiFaceAge;
import com.sunmi.facelib.SunmiFaceBoxSortMode;
import com.sunmi.facelib.SunmiFaceCompareResult;
import com.sunmi.facelib.SunmiFaceConfigParam;
import com.sunmi.facelib.SunmiFaceDBIdInfo;
import com.sunmi.facelib.SunmiFaceDBRecord;
import com.sunmi.facelib.SunmiFaceFeature;
import com.sunmi.facelib.SunmiFaceGender;
import com.sunmi.facelib.SunmiFaceGenderType;
import com.sunmi.facelib.SunmiFaceImage;
import com.sunmi.facelib.SunmiFaceImageFeatures;
import com.sunmi.facelib.SunmiFaceLib;
import com.sunmi.facelib.SunmiFaceLibConstants;
import com.sunmi.facelib.SunmiFaceLivenessMode;
import com.sunmi.facelib.SunmiFaceMode;
import com.sunmi.facelib.SunmiFacePose;
import com.sunmi.facelib.SunmiFaceQualityMode;
import com.sunmi.facelib.SunmiFaceRect;
import com.sunmi.facelib.SunmiFaceSDK;
import com.sunmi.facelib.SunmiFaceStatusCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;
import io.dcloud.feature.uniapp.AbsSDKInstance;

public class SunmiFaceModule extends UniModule {
    private static final String METADATA_FILE_NAME = "face_records_meta.json";
    private static final int REQUEST_CODE_PERMISSIONS = 40961;
    private static final int REQUEST_CODE_FACE_RECOGNIZE = 40962;
    // 用全局事件推送实时状态（比多次回调 UniJSCallback 更稳定）
    private static final String FACE_EVENT_NAME = "SunmiFaceEvent";
    private static volatile AbsSDKInstance faceEventSDKInstance = null;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static SunmiFaceDetectOverlay activeDetectOverlay = null;

    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA"
    };
    // INTERNET / ACCESS_NETWORK_STATE 属于普通权限：通常无需运行时申请，但我们仍然在 checkPermissions 里展示状态
    private static final String[] NETWORK_PERMISSIONS = new String[]{
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE"
    };

    private static final String[] CONFIG_FILES = new String[]{
            "attribute.model",
            "config.json",
            "depth_detector.yml",
            "detect.model",
            "detect_new.model",
            "face.model",
            "face_occlusion.model",
            "head_pose.model",
            "nir_liveness.model",
            "rgb_liveness.model"
    };

    private final Map<String, SunmiFaceImageFeatures> featuresCache = new HashMap<>();
    private boolean handleCreated = false;
    private boolean authorizeSdkInitialized = false;
    private UniJSCallback permissionCallback;
    private UniJSCallback recognizeCallback;
    private JSONObject recognizeOptions;

    @UniJSMethod(uiThread = false)
    public JSONObject getVersion() {
        JSONObject data = new JSONObject();
        data.put("code", SunmiFaceStatusCode.FACE_CODE_OK);
        data.put("success", true);
        data.put("message", "ok");
        data.put("data", SunmiFaceSDK.getVersion());
        return data;
    }

    @SuppressLint({"HardwareIds", "MissingPermission"})
    @UniJSMethod(uiThread = false)
    public JSONObject getDeviceInfo() {
        Context context = getSafeContext();
        JSONObject data = new JSONObject();
        data.put("brand", Build.BRAND);
        data.put("manufacturer", Build.MANUFACTURER);
        data.put("model", Build.MODEL);
        data.put("device", Build.DEVICE);
        data.put("product", Build.PRODUCT);
        data.put("hardware", Build.HARDWARE);
        data.put("board", Build.BOARD);
        data.put("fingerprint", Build.FINGERPRINT);
        data.put("androidSdkInt", Build.VERSION.SDK_INT);
        data.put("androidRelease", Build.VERSION.RELEASE);
        JSONArray supportedAbis = new JSONArray();
        supportedAbis.addAll(Arrays.asList(Build.SUPPORTED_ABIS));
        data.put("supportedAbis", supportedAbis);
        data.put("isSunmiBrand",
                containsIgnoreCase(Build.BRAND, "sunmi")
                        || containsIgnoreCase(Build.MANUFACTURER, "sunmi"));

        File defaultLicenseFile = new File("/storage/emulated/0/SunmiRemoteFiles/license_face.txt");
        data.put("defaultLicensePath", defaultLicenseFile.getAbsolutePath());
        data.put("defaultLicenseExists", defaultLicenseFile.exists());
        data.put("defaultLicenseReadable", defaultLicenseFile.canRead());

        if (context != null) {
            data.put("appFilesDir", context.getFilesDir().getAbsolutePath());
            data.put("appPackageName", context.getPackageName());
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                data.put("serial", Build.getSerial());
            } else {
                data.put("serial", Build.SERIAL);
            }
        } catch (Exception e) {
            data.put("serial", null);
            data.put("serialError", e.toString());
        }

        return success(data, "device info success");
    }

    // 授权 SDK 初始化可能耗时，但一些 SDK 对线程/上下文有约束
    // 这里尽量保持与原始封装一致：交由主线程执行
    @UniJSMethod(uiThread = true)
    public void initAuthorizeSDK(JSONObject options, UniJSCallback callback) {
        try {
            Context context = getSafeContext();
            if (context == null) {
                callback.invoke(error(ErrorCode.SDK_NOT_INIT, "context is null"));
                return;
            }
            boolean debuggable = options != null && options.getBooleanValue("debuggable");
            SunmiAuthorizeSDK.setDebuggable(debuggable);
            SunmiAuthorizeSDK.init(context);
            authorizeSdkInitialized = true;

            JSONObject data = new JSONObject();
            data.put("debuggable", debuggable);
            data.put("sdkVersion", SunmiAuthorizeSDK.getSunmiAuthorizeSDKVersion());
            callback.invoke(success(data, "init authorize sdk success"));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = false)
    public JSONObject getAuthorizeSDKVersion() {
        JSONObject data = new JSONObject();
        data.put("code", ErrorCode.IS_SUCCESS);
        data.put("success", true);
        data.put("message", "ok");
        data.put("data", SunmiAuthorizeSDK.getSunmiAuthorizeSDKVersion());
        return data;
    }

    @UniJSMethod(uiThread = false)
    public void syncGetAuthorizeCode(JSONObject options, UniJSCallback callback) {
        try {
            ensureAuthorizeSdk();
            AuthorizeResult result = SunmiAuthorizeSDK.syncGetAuthorizeCode(buildAuthorizeParams(options));
            callback.invoke(authorizeResult(result));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = true)
    public void asyncGetAuthorizeToken(JSONObject options, UniJSCallback callback) {
        try {
            ensureAuthorizeSdk();
            SunmiAuthorizeSDK.asyncGetAuthorizeToken(buildAuthorizeParams(options), new AuthorizeCallBack() {
                @Override
                public void onCall(AuthorizeResult result) {
                    if (callback != null) {
                        callback.invoke(authorizeResult(result));
                    }
                }
            });
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = true)
    public void clearLocalToken(UniJSCallback callback) {
        try {
            ensureAuthorizeSdk();
            SunmiAuthorizeSDK.clearLocalToken();
            callback.invoke(success(null, "clear local token success"));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = true)
    public void startFaceRecognize(JSONObject options, UniJSCallback callback) {
        Context context = getSafeContext();
        if (!(context instanceof Activity)) {
            callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR, "context is not activity"));
            return;
        }
        recognizeCallback = callback;
        recognizeOptions = options == null ? new JSONObject() : options;
        // 记录 SDKInstance 给 Activity 推事件用
        if (mUniSDKInstance instanceof AbsSDKInstance) {
            faceEventSDKInstance = (AbsSDKInstance) mUniSDKInstance;
        }
        Intent intent = new Intent(context, SunmiFaceRecognizeActivity.class);
        boolean preferFrontCamera = !recognizeOptions.containsKey("preferFrontCamera") || recognizeOptions.getBooleanValue("preferFrontCamera");
        if (recognizeOptions.containsKey("cameraFacing")) {
            String facing = recognizeOptions.getString("cameraFacing");
            // cameraFacing: "front" / "back"
            preferFrontCamera = !"back".equalsIgnoreCase(facing);
        }
        intent.putExtra("preferFrontCamera", preferFrontCamera);
        intent.putExtra("autoCaptureDelayMs", recognizeOptions.containsKey("autoCaptureDelayMs") ? recognizeOptions.getIntValue("autoCaptureDelayMs") : 800);
        intent.putExtra("showCancelButton", !recognizeOptions.containsKey("showCancelButton") || recognizeOptions.getBooleanValue("showCancelButton"));
        intent.putExtra("showSwitchCameraButton", !recognizeOptions.containsKey("showSwitchCameraButton") || recognizeOptions.getBooleanValue("showSwitchCameraButton"));
        // 让“检测文字”交给前端：可把状态栏隐藏
        intent.putExtra("showStatusText", !recognizeOptions.containsKey("showStatusText") || recognizeOptions.getBooleanValue("showStatusText"));
        intent.putExtra("showStartButton", recognizeOptions.containsKey("showStartButton") && recognizeOptions.getBooleanValue("showStartButton"));
        intent.putExtra("autoStartAnalyze", !recognizeOptions.containsKey("autoStartAnalyze") || recognizeOptions.getBooleanValue("autoStartAnalyze"));
        // 某些机型启用系统人脸检测会导致 native 崩溃（scudo/double free），默认关闭更稳
        intent.putExtra("enableSystemFaceDetection", recognizeOptions.containsKey("enableSystemFaceDetection") && recognizeOptions.getBooleanValue("enableSystemFaceDetection"));
        // 允许前端调整预览方向/抓拍图片方向，避免不同机型“倒的”
        intent.putExtra("displayOrientationDeg", recognizeOptions.containsKey("displayOrientationDeg") ? recognizeOptions.getIntValue("displayOrientationDeg") : 360);
        intent.putExtra("captureImageRotationDeg", recognizeOptions.containsKey("captureImageRotationDeg") ? recognizeOptions.getIntValue("captureImageRotationDeg") : 360);
        intent.putExtra("captureMirrorX", recognizeOptions.containsKey("captureMirrorX") && recognizeOptions.getBooleanValue("captureMirrorX"));
        intent.putExtra("showCircleGuide", recognizeOptions.containsKey("showCircleGuide") && recognizeOptions.getBooleanValue("showCircleGuide"));
        intent.putExtra("showSquareGuide", !recognizeOptions.containsKey("showSquareGuide") || recognizeOptions.getBooleanValue("showSquareGuide"));
        intent.putExtra("showRedLineGuide", recognizeOptions.containsKey("showRedLineGuide") && recognizeOptions.getBooleanValue("showRedLineGuide"));
        intent.putExtra("showGuideMask", recognizeOptions.containsKey("showGuideMask") && recognizeOptions.getBooleanValue("showGuideMask"));
        intent.putExtra("guideBoxWidthRatio", recognizeOptions.containsKey("guideBoxWidthRatio") ? recognizeOptions.getFloatValue("guideBoxWidthRatio") : 0.62f);
        intent.putExtra("guideBoxHeightRatio", recognizeOptions.containsKey("guideBoxHeightRatio") ? recognizeOptions.getFloatValue("guideBoxHeightRatio") : 0.62f);
        intent.putExtra("guideOffsetXRatio", recognizeOptions.containsKey("guideOffsetXRatio") ? recognizeOptions.getFloatValue("guideOffsetXRatio") : 0f);
        intent.putExtra("guideOffsetYRatio", recognizeOptions.containsKey("guideOffsetYRatio") ? recognizeOptions.getFloatValue("guideOffsetYRatio") : 0f);
        intent.putExtra("predictMode", recognizeOptions.containsKey("predictMode")
                ? recognizeOptions.getIntValue("predictMode")
                : SunmiFaceMode.PredictMode_Feature);
        intent.putExtra("livenessMode", recognizeOptions.containsKey("livenessMode")
                ? recognizeOptions.getIntValue("livenessMode")
                : SunmiFaceLivenessMode.LivenessMode_None);
        intent.putExtra("qualityMode", recognizeOptions.containsKey("qualityMode")
                ? recognizeOptions.getIntValue("qualityMode")
                : SunmiFaceQualityMode.QualityMode_None);
        intent.putExtra("maxFaceCount", recognizeOptions.containsKey("maxFaceCount")
                ? Math.max(1, recognizeOptions.getIntValue("maxFaceCount"))
                : 1);
        intent.putExtra("analyzeIntervalMs", recognizeOptions.containsKey("analyzeIntervalMs")
                ? Math.max(300, recognizeOptions.getIntValue("analyzeIntervalMs"))
                : 700);
        intent.putExtra("previewDecodeMaxSize", recognizeOptions.containsKey("previewDecodeMaxSize")
                ? Math.max(240, recognizeOptions.getIntValue("previewDecodeMaxSize"))
                : (recognizeOptions.containsKey("decodeMaxSize")
                ? Math.max(240, recognizeOptions.getIntValue("decodeMaxSize"))
                : 640));
        intent.putExtra("minFaceSize", recognizeOptions.containsKey("minFaceSize")
                ? Math.max(0, recognizeOptions.getIntValue("minFaceSize"))
                : 0);
        intent.putExtra("distanceThreshold", recognizeOptions.containsKey("distanceThreshold")
                ? recognizeOptions.getFloatValue("distanceThreshold")
                : 0f);
        ((Activity) context).startActivityForResult(intent, REQUEST_CODE_FACE_RECOGNIZE);
    }

    @UniJSMethod(uiThread = true)
    public void startFaceDetect(JSONObject options, UniJSCallback callback) {
        Context context = getSafeContext();
        if (!(context instanceof Activity)) {
            callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR, "context is not activity"));
            return;
        }
        stopActiveDetectOverlay("replaced", "已替换上一层人脸识别");
        JSONObject detectOptions = options == null ? new JSONObject() : options;
        activeDetectOverlay = new SunmiFaceDetectOverlay((Activity) context, detectOptions, callback, false);
        activeDetectOverlay.start();
    }

    @UniJSMethod(uiThread = true)
    public JSONObject stopFaceDetect() {
        stopActiveDetectOverlay("stopped", "已关闭人脸识别");
        return success(null, "stop face detect success");
    }

    @UniJSMethod(uiThread = true)
    public void openFaceDetect(JSONObject options, UniJSCallback callback) {
        Context context = getSafeContext();
        if (!(context instanceof Activity)) {
            callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR, "context is not activity"));
            return;
        }
        stopActiveDetectOverlay("replaced", "已替换上一层人脸识别");
        JSONObject detectOptions = options == null ? new JSONObject() : options;
        if (!detectOptions.containsKey("autoStopOnRecognize")) {
        detectOptions.put("autoStopOnRecognize", true);
        }
        activeDetectOverlay = new SunmiFaceDetectOverlay((Activity) context, detectOptions, callback, true);
        activeDetectOverlay.start();
    }

    // 给 Activity 调用：通过全局事件推送实时状态
    public static void emitFaceEvent(JSONObject event) {
        AbsSDKInstance inst = faceEventSDKInstance;
        if (inst == null || event == null) return;
        MAIN.post(() -> {
            try {
                inst.fireGlobalEventCallback(FACE_EVENT_NAME, event);
            } catch (Throwable ignored) {
            }
        });
    }

    private void stopActiveDetectOverlay(String eventType, String message) {
        SunmiFaceDetectOverlay overlay = activeDetectOverlay;
        activeDetectOverlay = null;
        if (overlay != null) {
            overlay.stop(eventType, message);
        }
    }

    @UniJSMethod(uiThread = false)
    public void activateByAppId(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        try {
            ensureAuthorizeSdk();
            AuthorizeResult authResult = SunmiAuthorizeSDK.syncGetAuthorizeCode(buildAuthorizeParams(options));
            JSONObject data = new JSONObject();
            data.put("authorize", authorizePayload(authResult));
            if (!isAuthorizeSuccess(authResult)) {
                callback.invoke(authorizeResult(authResult));
                return;
            }
            int verifyCode = SunmiFaceSDK.verifyLicense(getSafeContext(), authResult.token);
            data.put("verify", buildVerifyPayload(verifyCode, null, authResult.token));
            callback.invoke(status(verifyCode, data, verifyCode == SunmiFaceStatusCode.FACE_CODE_OK ? "activate by appId success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = false)
    public void activateByLicensePath(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        try {
            String licensePath = getString(options, "licensePath");
            if (TextUtils.isEmpty(licensePath)) {
                licensePath = "/storage/emulated/0/SunmiRemoteFiles/license_face.txt";
            }
            String licenseContent = readTextFile(new File(normalizePath(licensePath)));
            int code = SunmiFaceSDK.verifyLicense(getSafeContext(), licenseContent);
            callback.invoke(status(code, buildVerifyPayload(code, licensePath, licenseContent), code == SunmiFaceStatusCode.FACE_CODE_OK ? "activate by license path success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = true)
    public void checkPermissions(UniJSCallback callback) {
        Context context = getSafeContext();
        if (!(context instanceof Activity)) {
            callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR, "context is not activity"));
            return;
        }

        JSONArray missing = getMissingPermissions(context);
        if (missing.isEmpty()) {
            callback.invoke(success(buildPermissionPayload(context, false), "all permissions granted"));
            return;
        }

        permissionCallback = callback;
        ActivityCompat.requestPermissions((Activity) context, toStringArray(missing), REQUEST_CODE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != REQUEST_CODE_PERMISSIONS) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        Context context = getSafeContext();
        JSONObject payload = buildPermissionPayload(context, true);
        UniJSCallback callback = permissionCallback;
        permissionCallback = null;

        if (callback == null) {
            return;
        }

        JSONArray missing = payload.getJSONArray("missingPermissions");
        if (missing != null && !missing.isEmpty()) {
            callback.invoke(errorWithData(SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR, "permissions denied", payload));
            return;
        }
        callback.invoke(success(payload, "permissions granted"));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FACE_RECOGNIZE) {
            handleRecognizeResult(resultCode, data);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @UniJSMethod(uiThread = false)
    public void createHandle(UniJSCallback callback) {
        if (!handleCreated) {
            int code = SunmiFaceSDK.createHandle();
            handleCreated = code == SunmiFaceStatusCode.FACE_CODE_OK;
            callback.invoke(status(code, null, handleCreated ? "create handle success" : null));
            return;
        }
        callback.invoke(success(null, "handle already created"));
    }

    @UniJSMethod(uiThread = false)
    public void init(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        Context context = getSafeContext();
        try {
            String configPath = getString(options, "configPath");
            boolean useAssetConfig = options == null || !options.containsKey("useAssetConfig") || options.getBooleanValue("useAssetConfig");
            if (TextUtils.isEmpty(configPath) && useAssetConfig) {
                configPath = new File(ensureConfigDirectory(context, options), "config.json").getAbsolutePath();
            } else if (!TextUtils.isEmpty(configPath)) {
                configPath = resolveConfigPath(configPath);
            }
            int code = SunmiFaceSDK.init(configPath);
            JSONObject data = new JSONObject();
            data.put("configPath", configPath);
            callback.invoke(status(code, data, code == SunmiFaceStatusCode.FACE_CODE_OK ? "init success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = false)
    public void verifyLicense(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        try {
            String license = getString(options, "license");
            String appId = getString(options, "appId");
            if (TextUtils.isEmpty(license)) {
                license = getString(options, "licensePath");
            }
            if (!TextUtils.isEmpty(license) && looksLikeFilePath(license)) {
                String licensePath = normalizePath(license);
                license = readTextFile(new File(licensePath));
            }
            if (TextUtils.isEmpty(license) && !TextUtils.isEmpty(appId)) {
                ensureAuthorizeSdk();
                AuthorizeResult authResult = SunmiAuthorizeSDK.syncGetAuthorizeCode(buildAuthorizeParams(options));
                JSONObject data = new JSONObject();
                data.put("authorize", authorizePayload(authResult));
                if (!isAuthorizeSuccess(authResult)) {
                    callback.invoke(authorizeResult(authResult));
                    return;
                }
                int code = SunmiFaceSDK.verifyLicense(getSafeContext(), authResult.token);
                data.put("verify", buildVerifyPayload(code, null, authResult.token));
                callback.invoke(status(code, data, code == SunmiFaceStatusCode.FACE_CODE_OK ? "verify license success" : null));
                return;
            }
            if (TextUtils.isEmpty(license)) {
                callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_LICENSE_ERROR, "license, licensePath or appId is required"));
                return;
            }
            int code = SunmiFaceSDK.verifyLicense(getSafeContext(), license);
            JSONObject data = buildVerifyPayload(code, null, license);
            callback.invoke(status(code, data, code == SunmiFaceStatusCode.FACE_CODE_OK ? "verify license success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = false)
    public JSONObject getErrorString(JSONObject options) {
        int code = options == null ? SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR : options.getIntValue("code");
        JSONObject data = new JSONObject();
        data.put("code", code);
        data.put("success", true);
        data.put("message", "ok");
        data.put("data", SunmiFaceSDK.getErrorString(code));
        return data;
    }

    @UniJSMethod(uiThread = false)
    public void setConfig(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        try {
            SunmiFaceConfigParam param = buildConfigParam(options);
            int code = SunmiFaceSDK.setConfig(param);
            callback.invoke(status(code, configToJson(param), code == SunmiFaceStatusCode.FACE_CODE_OK ? "set config success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = false)
    public void getConfig(UniJSCallback callback) {
        ensureHandle();
        try {
            SunmiFaceConfigParam param = new SunmiFaceConfigParam();
            int code = SunmiFaceSDK.getConfig(param);
            callback.invoke(status(code, configToJson(param), code == SunmiFaceStatusCode.FACE_CODE_OK ? "get config success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = false)
    public void initDB(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        try {
            String dbPath = resolveDbFilePath(options);
            ensureParentDirectory(new File(dbPath));
            int code = SunmiFaceSDK.initDB(dbPath);
            JSONObject data = new JSONObject();
            data.put("dbPath", dbPath);
            callback.invoke(status(code, data, code == SunmiFaceStatusCode.FACE_CODE_OK ? "init db success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = false)
    public void getImageFeatures(JSONObject options, UniJSCallback callback) {
        ExtractionResult result = null;
        try {
            result = extractFeatures(options);
            JSONObject data = result.toJson();
            callback.invoke(status(result.code, data, result.code == SunmiFaceStatusCode.FACE_CODE_OK ? "get image features success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        } finally {
            if (result != null && result.owned && result.imageFeatures != null) {
                try {
                    SunmiFaceSDK.releaseImageFeatures(result.imageFeatures);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @UniJSMethod(uiThread = false)
    public void releaseImageFeatures(JSONObject options, UniJSCallback callback) {
        String token = getString(options, "token");
        SunmiFaceImageFeatures features = TextUtils.isEmpty(token) ? null : featuresCache.remove(token);
        if (features == null) {
            callback.invoke(success(null, "image features already released"));
            return;
        }
        int code = SunmiFaceSDK.releaseImageFeatures(features);
        callback.invoke(status(code, null, code == SunmiFaceStatusCode.FACE_CODE_OK ? "release image features success" : null));
    }

    @UniJSMethod(uiThread = false)
    public void addDBRecord(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        FeatureCarrier carrier = null;
        SunmiFaceDBRecord record = null;
        try {
            String id = getString(options, "id");
            if (TextUtils.isEmpty(id)) {
                callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_IMAGE_ID_ERROR, "id is required"));
                return;
            }
            carrier = resolveFeatureCarrier(options);
            if (carrier.feature == null || carrier.feature.getFeature() == null) {
                callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_EMPTY_IMAGE, "feature is required"));
                return;
            }
            record = SunmiFaceSDK.faceFeature2FaceDBRecord(carrier.feature);
            record.setId(id);
            record.setName(getString(options, "name"));
            record.setImgId(getString(options, "imgId"));
            int code = SunmiFaceSDK.addDBRecord(record);
            JSONObject data = dbRecordToJson(record);
            if (code == SunmiFaceStatusCode.FACE_CODE_OK) {
                saveMetadataRecord(options, record, carrier.feature);
            }
            callback.invoke(status(code, data, code == SunmiFaceStatusCode.FACE_CODE_OK ? "add db record success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        } finally {
            releaseCarrier(carrier);
        }
    }

    @UniJSMethod(uiThread = false)
    public void searchDB(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        FeatureCarrier carrier = null;
        SunmiFaceDBRecord record = null;
        try {
            carrier = resolveFeatureCarrier(options);
            if (carrier.feature == null || carrier.feature.getFeature() == null) {
                callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_EMPTY_IMAGE, "feature is required"));
                return;
            }
            record = SunmiFaceSDK.faceFeature2FaceDBRecord(carrier.feature);
            SunmiFaceDBIdInfo info = new SunmiFaceDBIdInfo();
            int code = SunmiFaceSDK.searchDB(record, info);
            JSONObject data = dbIdInfoToJson(info);
            if (code == SunmiFaceStatusCode.FACE_CODE_OK) {
                JSONObject metadata = findFirstMetadataRecord(options, info.getId());
                if (metadata != null) {
                    data.put("metadata", metadata);
                }
            }
            callback.invoke(status(code, data, code == SunmiFaceStatusCode.FACE_CODE_OK ? "search db success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        } finally {
            releaseCarrier(carrier);
        }
    }

    @UniJSMethod(uiThread = false)
    public void compare1v1(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        FeatureCarrier first = null;
        FeatureCarrier second = null;
        try {
            JSONObject firstOptions = options == null ? null : options.getJSONObject("first");
            JSONObject secondOptions = options == null ? null : options.getJSONObject("second");
            if (firstOptions == null || secondOptions == null) {
                callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_EMPTY_IMAGE, "first and second are required"));
                return;
            }
            first = resolveFeatureCarrier(firstOptions);
            second = resolveFeatureCarrier(secondOptions);
            if (first.feature == null || second.feature == null) {
                callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_EMPTY_IMAGE, "failed to resolve compare features"));
                return;
            }
            SunmiFaceCompareResult result = new SunmiFaceCompareResult();
            int code = SunmiFaceSDK.compare1v1(first.feature, second.feature, result);
            callback.invoke(status(code, compareResultToJson(result), code == SunmiFaceStatusCode.FACE_CODE_OK ? "compare success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        } finally {
            releaseCarrier(first);
            releaseCarrier(second);
        }
    }

    @UniJSMethod(uiThread = false)
    public void deleteDBRecord(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        String id = getString(options, "id");
        String imgId = getString(options, "imgId");
        if (TextUtils.isEmpty(id) && TextUtils.isEmpty(imgId)) {
            callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_IMAGE_ID_ERROR, "imgId or id is required"));
            return;
        }
        try {
            File metadataFile = resolveMetadataFile(options);
            JSONArray deletedImgIds = new JSONArray();
            int code;
            if (!TextUtils.isEmpty(imgId)) {
                code = SunmiFaceSDK.deleteDBRecord(imgId);
                if (code == SunmiFaceStatusCode.FACE_CODE_OK) {
                    deletedImgIds.add(imgId);
                    removeMetadataRecords(metadataFile, deletedImgIds);
                }
            } else {
                JSONArray imgIds = findImgIdsById(metadataFile, id);
                if (imgIds.isEmpty()) {
                    code = SunmiFaceSDK.deleteDBRecord(id);
                    if (code == SunmiFaceStatusCode.FACE_CODE_OK) {
                        deletedImgIds.add(id);
                    }
                } else {
                    code = SunmiFaceStatusCode.FACE_CODE_OK;
                    for (int i = 0; i < imgIds.size(); i++) {
                        String currentImgId = imgIds.getString(i);
                        int deleteCode = SunmiFaceSDK.deleteDBRecord(currentImgId);
                        if (deleteCode == SunmiFaceStatusCode.FACE_CODE_OK) {
                            deletedImgIds.add(currentImgId);
                        } else {
                            code = deleteCode;
                            break;
                        }
                    }
                    if (!deletedImgIds.isEmpty()) {
                        removeMetadataRecords(metadataFile, deletedImgIds);
                    }
                }
            }
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("imgId", imgId);
            data.put("deletedImgIds", deletedImgIds);
            data.put("deletedCount", deletedImgIds.size());
            callback.invoke(status(code, data, code == SunmiFaceStatusCode.FACE_CODE_OK ? "delete db record success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = false)
    public void getAllDBRecords(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        try {
            File metadataFile = resolveMetadataFile(options);
            JSONArray records = readMetadataArray(metadataFile);
            JSONObject data = new JSONObject();
            data.put("records", records);
            data.put("count", records.size());
            data.put("metadataPath", metadataFile.getAbsolutePath());
            callback.invoke(success(data, "get all db records success"));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = false)
    public void clearFaceDatabase(JSONObject options, UniJSCallback callback) {
        ensureHandle();
        try {
            String dbPath = resolveDbFilePath(options);
            File dbFile = new File(dbPath);
            File metadataFile = resolveMetadataFile(options);
            boolean deletedDb = !dbFile.exists() || dbFile.delete();
            boolean deletedMetadata = !metadataFile.exists() || metadataFile.delete();
            if (!deletedDb) {
                callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_FACE_DB_ERROR, "failed to delete db file"));
                return;
            }
            if (!deletedMetadata) {
                callback.invoke(error(SunmiFaceStatusCode.FACE_CODE_FACE_DB_ERROR, "failed to delete metadata file"));
                return;
            }
            ensureParentDirectory(dbFile);
            int code = SunmiFaceSDK.initDB(dbPath);
            JSONObject data = new JSONObject();
            data.put("dbPath", dbPath);
            data.put("metadataPath", metadataFile.getAbsolutePath());
            data.put("deletedDb", deletedDb);
            data.put("deletedMetadata", deletedMetadata);
            callback.invoke(status(code, data, code == SunmiFaceStatusCode.FACE_CODE_OK ? "clear face database success" : null));
        } catch (Exception e) {
            callback.invoke(exception(e));
        }
    }

    @UniJSMethod(uiThread = false)
    public void releaseHandle(UniJSCallback callback) {
        releaseAllCachedFeatures();
        boolean hadModuleHandle = handleCreated;
        handleCreated = false;
        SunmiFaceCameraView.releaseSharedSdk();
        if (!hadModuleHandle) {
            callback.invoke(success(null, "handle already released"));
            return;
        }
        callback.invoke(success(null, "release handle success"));
    }

    private void ensureHandle() {
        if (!handleCreated) {
            int code = SunmiFaceSDK.createHandle();
            handleCreated = code == SunmiFaceStatusCode.FACE_CODE_OK;
            if (!handleCreated) {
                throw new IllegalStateException("createHandle failed: " + SunmiFaceSDK.getErrorString(code));
            }
        }
    }

    private ExtractionResult extractFeatures(JSONObject options) throws IOException {
        ensureHandle();
        // 如前端传了识别参数（distanceThreshold/minFaceSize 等），先下发给 SDK
        if (options != null
                && (options.containsKey("threadNum")
                || options.containsKey("distanceThreshold")
                || options.containsKey("faceScoreThreshold")
                || options.containsKey("minFaceSize")
                || options.containsKey("depthXOffset")
                || options.containsKey("depthYOffset")
                || options.containsKey("boxSortMode"))) {
            int code = SunmiFaceSDK.setConfig(buildConfigParam(options));
            if (code != SunmiFaceStatusCode.FACE_CODE_OK) {
                throw new IllegalStateException("setConfig failed: " + SunmiFaceSDK.getErrorString(code));
            }
        }

        ImageBuildResult buildResult = buildImage(options);
        SunmiFaceImage image = buildResult.image;
        int imageWidth = buildResult.width;
        int imageHeight = buildResult.height;
        SunmiFaceImageFeatures imageFeatures = new SunmiFaceImageFeatures();
        try {
            int code = SunmiFaceSDK.getImageFeatures(image, imageFeatures);
            ExtractionResult result = new ExtractionResult();
            result.code = code;
            result.imageFeatures = imageFeatures;
            result.feature = getPrimaryFeature(imageFeatures);
            result.data = minimalFeatureContainerToJson(imageFeatures, imageWidth, imageHeight);
            if (code == SunmiFaceStatusCode.FACE_CODE_OK && options != null && options.getBooleanValue("keepAlive")) {
                result.token = UUID.randomUUID().toString();
                featuresCache.put(result.token, imageFeatures);
                result.data.put("token", result.token);
                result.owned = false;
            } else {
                result.owned = true;
            }
            if (result.owned && code != SunmiFaceStatusCode.FACE_CODE_OK) {
                try {
                    SunmiFaceSDK.releaseImageFeatures(imageFeatures);
                } catch (Exception ignored) {
                }
            }
            return result;
        } finally {
            try {
                image.delete();
            } catch (Exception ignored) {
            }
        }
    }

    private FeatureCarrier resolveFeatureCarrier(JSONObject options) throws IOException {
        if (options == null) {
            return new FeatureCarrier(null, null, false);
        }

        String token = getString(options, "token");
        if (!TextUtils.isEmpty(token) && featuresCache.containsKey(token)) {
            boolean retainToken = options.getBooleanValue("retainToken");
            SunmiFaceImageFeatures imageFeatures = retainToken ? featuresCache.get(token) : featuresCache.remove(token);
            return new FeatureCarrier(getPrimaryFeature(imageFeatures), imageFeatures, !retainToken);
        }

        float[] featureArray = toFloatArray(options.getJSONArray("feature"));
        if (featureArray != null && featureArray.length > 0) {
            SunmiFaceFeature feature = new SunmiFaceFeature();
            feature.setFeature(featureArray);
            return new FeatureCarrier(feature, null, false);
        }

        ExtractionResult result = extractFeatures(options);
        if (result.code != SunmiFaceStatusCode.FACE_CODE_OK) {
            throw new IllegalStateException("getImageFeatures failed: " + SunmiFaceSDK.getErrorString(result.code));
        }
        return new FeatureCarrier(result.feature, result.imageFeatures, result.owned);
    }

    private void releaseCarrier(FeatureCarrier carrier) {
        if (carrier != null && carrier.feature != null) {
            try {
                carrier.feature.delete();
            } catch (Exception ignored) {
            }
        }
        if (carrier != null && carrier.owned && carrier.imageFeatures != null) {
            try {
                SunmiFaceSDK.releaseImageFeatures(carrier.imageFeatures);
            } catch (Exception ignored) {
            }
        }
    }

    private ImageBuildResult buildImage(JSONObject options) throws IOException {
        byte[] imageBytes = resolveImageBytes(options);
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imagePath or base64 is required");
        }

        int decodeMaxSize = options == null || !options.containsKey("decodeMaxSize") ? 1280 : options.getIntValue("decodeMaxSize");
        decodeMaxSize = Math.max(64, decodeMaxSize);

        Bitmap bitmap = decodeBitmapForFace(imageBytes, decodeMaxSize);
        if (bitmap == null) {
            throw new IllegalArgumentException("failed to decode image");
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        byte[] rgb = new byte[width * height * 3];
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            rgb[i * 3] = (byte) (pixel & 0xFF);
            rgb[i * 3 + 1] = (byte) ((pixel >> 8) & 0xFF);
            rgb[i * 3 + 2] = (byte) ((pixel >> 16) & 0xFF);
        }
        bitmap.recycle();

        int maxFaceCount = options == null || !options.containsKey("maxFaceCount") ? 1 : options.getIntValue("maxFaceCount");
        SunmiFaceImage image = new SunmiFaceImage(rgb, height, width, maxFaceCount);
        image.setPredictMode(options == null || !options.containsKey("predictMode") ? SunmiFaceMode.PredictMode_Feature : options.getIntValue("predictMode"));
        image.setLivenessMode(options == null ? SunmiFaceLivenessMode.LivenessMode_None : options.getIntValue("livenessMode"));
        image.setQualityMode(options == null ? SunmiFaceQualityMode.QualityMode_None : options.getIntValue("qualityMode"));
        return new ImageBuildResult(image, width, height);
    }

    private Bitmap decodeBitmapForFace(byte[] imageBytes, int decodeMaxSize) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bounds);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = calculateInSampleSize(bounds, decodeMaxSize, decodeMaxSize);
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while (height / inSampleSize > reqHeight || width / inSampleSize > reqWidth) {
            inSampleSize *= 2;
        }
        return Math.max(1, inSampleSize);
    }

    private byte[] resolveImageBytes(JSONObject options) throws IOException {
        String imagePath = getString(options, "imagePath");
        if (!TextUtils.isEmpty(imagePath)) {
            imagePath = normalizePath(imagePath);
            return readFileBytes(new File(imagePath));
        }

        String base64 = getString(options, "base64");
        if (TextUtils.isEmpty(base64)) {
            return null;
        }
        int comma = base64.indexOf(',');
        if (comma >= 0) {
            base64 = base64.substring(comma + 1);
        }
        return Base64.decode(base64.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
    }

    private SunmiFaceConfigParam buildConfigParam(JSONObject options) {
        SunmiFaceConfigParam param = new SunmiFaceConfigParam();
        if (options == null) {
            return param;
        }
        if (options.containsKey("threadNum")) {
            param.setThreadNum(options.getIntValue("threadNum"));
        }
        if (options.containsKey("distanceThreshold")) {
            param.setDistanceThreshold(options.getFloatValue("distanceThreshold"));
        }
        if (options.containsKey("faceScoreThreshold")) {
            param.setFaceScoreThreshold(options.getFloatValue("faceScoreThreshold"));
        }
        if (options.containsKey("minFaceSize")) {
            param.setMinFaceSize(options.getIntValue("minFaceSize"));
        }
        if (options.containsKey("depthXOffset")) {
            param.setDepthXOffset(options.getIntValue("depthXOffset"));
        }
        if (options.containsKey("depthYOffset")) {
            param.setDepthYOffset(options.getIntValue("depthYOffset"));
        }
        if (options.containsKey("boxSortMode")) {
            param.setBoxSortMode(options.getIntValue("boxSortMode"));
        } else {
            param.setBoxSortMode(SunmiFaceBoxSortMode.BoxSortMode_Score);
        }
        return param;
    }

    private JSONObject configToJson(SunmiFaceConfigParam param) {
        JSONObject data = new JSONObject();
        data.put("threadNum", param.getThreadNum());
        data.put("distanceThreshold", param.getDistanceThreshold());
        data.put("faceScoreThreshold", param.getFaceScoreThreshold());
        data.put("minFaceSize", param.getMinFaceSize());
        data.put("depthXOffset", param.getDepthXOffset());
        data.put("depthYOffset", param.getDepthYOffset());
        data.put("boxSortMode", param.getBoxSortMode());
        return data;
    }

    private JSONObject featureContainerToJson(SunmiFaceImageFeatures imageFeatures, JSONObject options, int imageWidth, int imageHeight) {
        JSONObject data = new JSONObject();
        int rectRotationDeg = options != null && options.containsKey("rectRotation") ? options.getIntValue("rectRotation") : 0;
        boolean rectMirrorX = options != null && options.containsKey("rectMirrorX") && options.getBooleanValue("rectMirrorX");
        int normRot = ((rectRotationDeg % 360) + 360) % 360;
        int outputWidth = (normRot == 90 || normRot == 270) ? imageHeight : imageWidth;
        int outputHeight = (normRot == 90 || normRot == 270) ? imageWidth : imageHeight;

        data.put("imageWidth", imageWidth);
        data.put("imageHeight", imageHeight);
        data.put("outputImageWidth", outputWidth);
        data.put("outputImageHeight", outputHeight);
        data.put("featuresCount", imageFeatures.getFeaturesCount());
        data.put("feature", featureToJson(getPrimaryFeature(imageFeatures), imageWidth, imageHeight, normRot, rectMirrorX));
        return data;
    }

    private JSONObject minimalFeatureContainerToJson(SunmiFaceImageFeatures imageFeatures, int imageWidth, int imageHeight) {
        JSONObject data = new JSONObject();
        data.put("imageWidth", imageWidth);
        data.put("imageHeight", imageHeight);
        data.put("outputImageWidth", imageWidth);
        data.put("outputImageHeight", imageHeight);
        data.put("featuresCount", imageFeatures == null ? 0 : imageFeatures.getFeaturesCount());
        data.put("safeMode", true);
        data.put("ultraSafeMode", true);
        return data;
    }

    private SunmiFaceFeature getPrimaryFeature(SunmiFaceImageFeatures imageFeatures) {
        if (imageFeatures == null || imageFeatures.getFeaturesCount() <= 0 || imageFeatures.getFeatures() == null) {
            return null;
        }
        return SunmiFaceLib.SunmiFaceFeatureArrayGetItem(imageFeatures.getFeatures(), 0);
    }

    private JSONObject featureToJson(SunmiFaceFeature feature, int imageWidth, int imageHeight, int rectRotationDeg, boolean rectMirrorX) {
        if (feature == null) {
            return null;
        }
        JSONObject data = new JSONObject();
        data.put("faceRect", rectToJson(feature.getFaceRect(), imageWidth, imageHeight, rectRotationDeg, rectMirrorX));
        data.put("rgbLivenessScore", feature.getRgbLivenessScore());
        data.put("nirLivenessScore", feature.getNirLivenessScore());
        data.put("depthLivenessScore", feature.getDepthLivenessScore());
        data.put("feature", feature.getFeature());
        data.put("landmark", landmarkToJson(feature));
        data.put("pose", poseToJson(feature.getPose()));
        data.put("age", ageToJson(feature.getAge()));
        data.put("gender", genderToJson(feature.getGender()));
        data.put("varLaplacian", feature.getVarLaplacian());
        data.put("luminance", feature.getLuminance());
        data.put("occlusionScore", feature.getOcclusionScore());
        return data;
    }

    private JSONObject landmarkToJson(SunmiFaceFeature feature) {
        if (feature == null || feature.getLandmark() == null) {
            return null;
        }
        JSONArray points = new JSONArray();
        for (int i = 0; i < SunmiFaceLibConstants.SUNMI_FACE_LANDMARK_LEN; i++) {
            JSONObject point = new JSONObject();
            point.put("index", i);
            point.put("x", SunmiFaceLib.SunmiFaceLmkArrayGetItem(feature.getLandmark(), i).getX());
            point.put("y", SunmiFaceLib.SunmiFaceLmkArrayGetItem(feature.getLandmark(), i).getY());
            points.add(point);
        }
        JSONObject result = new JSONObject();
        result.put("points", points);
        return result;
    }

    private JSONObject rectToJson(SunmiFaceRect rect, int imageWidth, int imageHeight, int rectRotationDeg, boolean rectMirrorX) {
        if (rect == null) {
            return null;
        }
        JSONObject data = new JSONObject();

        float x1 = rect.getX1();
        float y1 = rect.getY1();
        float x2 = rect.getX2();
        float y2 = rect.getY2();

        float left = Math.min(x1, x2);
        float right = Math.max(x1, x2);
        float top = Math.min(y1, y2);
        float bottom = Math.max(y1, y2);

        // 先镜像后旋转（与前端对图像的变换顺序保持一致的前提下，通常最接近预期）
        float[] p1 = transformPoint(left, top, imageWidth, imageHeight, rectRotationDeg, rectMirrorX);     // LT
        float[] p2 = transformPoint(right, top, imageWidth, imageHeight, rectRotationDeg, rectMirrorX);    // RT
        float[] p3 = transformPoint(left, bottom, imageWidth, imageHeight, rectRotationDeg, rectMirrorX); // LB
        float[] p4 = transformPoint(right, bottom, imageWidth, imageHeight, rectRotationDeg, rectMirrorX); // RB

        float nx1 = Math.min(Math.min(p1[0], p2[0]), Math.min(p3[0], p4[0]));
        float nx2 = Math.max(Math.max(p1[0], p2[0]), Math.max(p3[0], p4[0]));
        float ny1 = Math.min(Math.min(p1[1], p2[1]), Math.min(p3[1], p4[1]));
        float ny2 = Math.max(Math.max(p1[1], p2[1]), Math.max(p3[1], p4[1]));

        data.put("x1", nx1);
        data.put("y1", ny1);
        data.put("x2", nx2);
        data.put("y2", ny2);
        data.put("score", rect.getScore());
        return data;
    }

    /**
     * 将点 (x,y) 从“原始图片坐标系”映射到“应用了 rectRotationDeg + rectMirrorX 后的坐标系”。
     * 坐标原点假设为左上角，rectRotationDeg 为顺时针旋转角度。
     */
    private float[] transformPoint(float x, float y, int imageWidth, int imageHeight, int rectRotationDeg, boolean rectMirrorX) {
        float px = x;
        float py = y;
        if (rectMirrorX) {
            // 水平翻转：x 从左侧变到右侧
            px = imageWidth - px;
        }

        switch (rectRotationDeg) {
            case 0:
                return new float[]{px, py};
            case 90:
                // (x,y) -> (H - y, x)
                return new float[]{imageHeight - py, px};
            case 180:
                // (x,y) -> (W - x, H - y)
                return new float[]{imageWidth - px, imageHeight - py};
            case 270:
                // (x,y) -> (y, W - x)
                return new float[]{py, imageWidth - px};
            default:
                return new float[]{px, py};
        }
    }

    private JSONObject poseToJson(SunmiFacePose pose) {
        if (pose == null) {
            return null;
        }
        JSONObject data = new JSONObject();
        data.put("pitch", pose.getPitch());
        data.put("yaw", pose.getYaw());
        data.put("roll", pose.getRoll());
        return data;
    }

    private JSONObject ageToJson(SunmiFaceAge age) {
        if (age == null) {
            return null;
        }
        JSONObject data = new JSONObject();
        data.put("classification", age.getClassification());
        data.put("score", age.getScore());
        return data;
    }

    private JSONObject genderToJson(SunmiFaceGender gender) {
        if (gender == null) {
            return null;
        }
        JSONObject data = new JSONObject();
        data.put("classification", gender.getClassification());
        data.put("score", gender.getScore());
        data.put("type", gender.getClassification() == SunmiFaceGenderType.FACE_ATTR_MALE ? "male" : "female");
        return data;
    }

    private JSONObject compareResultToJson(SunmiFaceCompareResult result) {
        JSONObject data = new JSONObject();
        data.put("isMatched", result.getIsMatched());
        data.put("distance", result.getDistance());
        return data;
    }

    private JSONObject dbRecordToJson(SunmiFaceDBRecord record) {
        JSONObject data = new JSONObject();
        data.put("id", record.getId());
        data.put("name", record.getName());
        data.put("imgId", record.getImgId());
        data.put("feature", record.getFeature());
        return data;
    }

    private JSONObject dbIdInfoToJson(SunmiFaceDBIdInfo info) {
        JSONObject data = new JSONObject();
        data.put("id", info.getId());
        data.put("name", info.getName());
        data.put("isMatched", info.getIsMatched());
        data.put("distance", info.getDistance());
        return data;
    }

    private JSONObject success(Object data, String message) {
        JSONObject result = new JSONObject();
        result.put("code", SunmiFaceStatusCode.FACE_CODE_OK);
        result.put("success", true);
        result.put("message", message == null ? "ok" : message);
        result.put("data", data);
        return result;
    }

    private JSONObject error(int code, String message) {
        JSONObject result = new JSONObject();
        result.put("code", code);
        result.put("success", false);
        result.put("message", message);
        result.put("errorString", SunmiFaceSDK.getErrorString(code));
        result.put("data", null);
        return result;
    }

    private JSONObject errorWithData(int code, String message, Object data) {
        JSONObject result = new JSONObject();
        result.put("code", code);
        result.put("success", false);
        result.put("message", message);
        result.put("errorString", SunmiFaceSDK.getErrorString(code));
        result.put("data", data);
        return result;
    }

    private JSONObject exception(Exception e) {
        JSONObject result = new JSONObject();
        result.put("code", SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR);
        result.put("success", false);
        result.put("message", e.getMessage());
        result.put("errorString", e.toString());
        result.put("data", null);
        return result;
    }

    private JSONObject status(int code, Object data, String successMessage) {
        if (code == SunmiFaceStatusCode.FACE_CODE_OK) {
            return success(data, successMessage);
        }
        JSONObject result = new JSONObject();
        result.put("code", code);
        result.put("success", false);
        result.put("message", SunmiFaceSDK.getErrorString(code));
        result.put("errorString", SunmiFaceSDK.getErrorString(code));
        result.put("data", data);
        return result;
    }

    private void ensureAuthorizeSdk() {
        if (!authorizeSdkInitialized) {
            Context context = getSafeContext();
            if (context == null) {
                throw new IllegalStateException("context is null");
            }
            SunmiAuthorizeSDK.init(context);
            authorizeSdkInitialized = true;
        }
    }

    private Map<String, Object> buildAuthorizeParams(JSONObject options) {
        String appId = getString(options, "appId");
        if (TextUtils.isEmpty(appId)) {
            throw new IllegalArgumentException("appId is required");
        }
        Map<String, Object> params = new HashMap<>();
        params.put(SunmiAuthorizeSDK.APP_ID, appId);
        params.put(SunmiAuthorizeSDK.CATEGORY_TYPE_KEY, SunmiAuthorizeSDK.CATEGORY_TYPE_FACE);
        params.put(SunmiAuthorizeSDK.IS_FORCE_REFRESH, options != null && options.getBooleanValue("forceRefresh"));
        return params;
    }

    private boolean isAuthorizeSuccess(AuthorizeResult result) {
        return result != null && result.code == ErrorCode.IS_SUCCESS && !TextUtils.isEmpty(result.token);
    }

    private JSONObject authorizeResult(AuthorizeResult result) {
        JSONObject data = authorizePayload(result);
        boolean success = isAuthorizeSuccess(result);
        JSONObject wrapper = new JSONObject();
        wrapper.put("code", result == null ? ErrorCode.REQUEST_EXCEPTION : result.code);
        wrapper.put("success", success);
        wrapper.put("message", result == null ? "authorize result is null" : result.msg);
        wrapper.put("data", data);
        return wrapper;
    }

    private JSONObject authorizePayload(AuthorizeResult result) {
        JSONObject data = new JSONObject();
        if (result == null) {
            data.put("code", ErrorCode.REQUEST_EXCEPTION);
            data.put("msg", "authorize result is null");
            data.put("token", null);
            return data;
        }
        data.put("code", result.code);
        data.put("msg", result.msg);
        data.put("token", result.token);
        return data;
    }

    private JSONObject buildVerifyPayload(int code, String licensePath, String license) {
        JSONObject data = new JSONObject();
        data.put("licensePath", licensePath);
        data.put("license", license);
        data.put("errorString", SunmiFaceSDK.getErrorString(code));
        return data;
    }

    private Context getSafeContext() {
        return mUniSDKInstance == null ? null : mUniSDKInstance.getContext();
    }

    private JSONObject buildPermissionPayload(Context context, boolean requested) {
        JSONObject data = new JSONObject();
        JSONObject status = new JSONObject();
        JSONArray missing = getMissingPermissions(context);
        for (String permission : REQUIRED_PERMISSIONS) {
            boolean granted = context != null
                    && ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
            status.put(permission, granted);
        }
        for (String permission : NETWORK_PERMISSIONS) {
            boolean granted = context != null
                    && ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
            status.put(permission, granted);
        }
        data.put("requested", requested);
        data.put("permissions", status);
        data.put("missingPermissions", missing);
        data.put("networkConnected", isNetworkConnected(context));
        return data;
    }

    private JSONArray getMissingPermissions(Context context) {
        JSONArray missing = new JSONArray();
        for (String permission : REQUIRED_PERMISSIONS) {
            boolean granted = context != null
                    && ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                missing.add(permission);
            }
        }
        return missing;
    }

    private boolean isNetworkConnected(Context context) {
        if (context == null) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        // 兼容老系统：没有 NET_CAPABILITY 时退化到 cm 判定
        //noinspection deprecation
        android.net.NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private String[] toStringArray(JSONArray array) {
        String[] result = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            result[i] = array.getString(i);
        }
        return result;
    }

    private String getString(JSONObject object, String key) {
        return object == null ? null : object.getString(key);
    }

    private String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("file://")) {
            return path.substring("file://".length());
        }
        return path;
    }

    private float[] toFloatArray(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return null;
        }
        float[] result = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            result[i] = array.getFloatValue(i);
        }
        return result;
    }

    private File ensureConfigDirectory(Context context, JSONObject options) throws IOException {
        if (context == null) {
            throw new IllegalStateException("context is null");
        }
        File configDir = resolveStorageDirectory(options);
        if (!configDir.exists() && !configDir.mkdirs()) {
            throw new IOException("failed to create config directory");
        }
        for (String fileName : CONFIG_FILES) {
            copyAssetIfNeeded(context, fileName, new File(configDir, fileName));
        }
        rewriteConfigJson(configDir);
        return configDir;
    }

    private void rewriteConfigJson(File configDir) throws IOException {
        File configFile = new File(configDir, "config.json");
        JSONObject jsonObject = JSONObject.parseObject(readTextFile(configFile));
        absolutizeConfigValue(jsonObject, configDir, "face_model_path");
        absolutizeConfigValue(jsonObject, configDir, "detect_model_path");
        absolutizeConfigValue(jsonObject, configDir, "rgb_liveness_model_path");
        absolutizeConfigValue(jsonObject, configDir, "nir_liveness_model_path");
        absolutizeConfigValue(jsonObject, configDir, "attr_model_path");
        absolutizeConfigValue(jsonObject, configDir, "occlusion_model_path");
        absolutizeConfigValue(jsonObject, configDir, "headpose_model_path");
        absolutizeConfigValue(jsonObject, configDir, "depth_detector");
        absolutizeConfigValue(jsonObject, configDir, "face_db_file");
        writeTextFile(configFile, jsonObject.toJSONString());
    }

    private void absolutizeConfigValue(JSONObject jsonObject, File baseDir, String key) {
        if (jsonObject == null || baseDir == null || TextUtils.isEmpty(key)) {
            return;
        }
        String value = jsonObject.getString(key);
        if (TextUtils.isEmpty(value)) {
            return;
        }
        File file = new File(normalizePath(value));
        if (!file.isAbsolute()) {
            jsonObject.put(key, new File(baseDir, value).getAbsolutePath());
        }
    }

    private File resolveStorageDirectory(JSONObject options) {
        String configPath = getString(options, "configPath");
        if (!TextUtils.isEmpty(configPath)) {
            File target = new File(normalizePath(configPath));
            return target.isDirectory() ? target : target.getParentFile();
        }
        String dbPath = getString(options, "dbPath");
        if (!TextUtils.isEmpty(dbPath)) {
            File target = new File(normalizePath(dbPath));
            return target.isDirectory() ? target : target.getParentFile();
        }
        return new File("/storage/emulated/0", "config");
    }

    private String resolveConfigPath(String configPath) {
        File configFile = new File(normalizePath(configPath));
        if (configFile.isDirectory()) {
            configFile = new File(configFile, "config.json");
        }
        return configFile.getAbsolutePath();
    }

    private String resolveDbFilePath(JSONObject options) throws IOException {
        String dbPath = getString(options, "dbPath");
        File storageDir = resolveStorageDirectory(options);
        String dbFileName = resolveConfiguredDbFileName(storageDir);
        if (TextUtils.isEmpty(dbPath)) {
            return new File(storageDir, dbFileName).getAbsolutePath();
        }
        File target = new File(normalizePath(dbPath));
        if (target.isDirectory() || !target.getName().contains(".")) {
            target = new File(target, dbFileName);
        }
        return target.getAbsolutePath();
    }

    private String resolveConfiguredDbFileName(File storageDir) throws IOException {
        File configFile = new File(storageDir, "config.json");
        if (!configFile.exists()) {
            String assetValue = readAssetConfigValue("face_db_file");
            return TextUtils.isEmpty(assetValue) ? "sunmi_face.db" : new File(normalizePath(assetValue)).getName();
        }
        JSONObject jsonObject = JSONObject.parseObject(readTextFile(configFile));
        String value = jsonObject.getString("face_db_file");
        if (TextUtils.isEmpty(value)) {
            String assetValue = readAssetConfigValue("face_db_file");
            return TextUtils.isEmpty(assetValue) ? "sunmi_face.db" : new File(normalizePath(assetValue)).getName();
        }
        return new File(normalizePath(value)).getName();
    }

    private File resolveMetadataFile(JSONObject options) throws IOException {
        String dbPath = resolveDbFilePath(options);
        File dbFile = new File(dbPath);
        File parent = dbFile.getParentFile();
        if (parent == null) {
            throw new IOException("invalid db path: " + dbPath);
        }
        return new File(parent, METADATA_FILE_NAME);
    }

    private JSONArray readMetadataArray(File metadataFile) throws IOException {
        if (metadataFile == null || !metadataFile.exists() || metadataFile.length() <= 0) {
            return new JSONArray();
        }
        JSONArray records = JSONArray.parseArray(readTextFile(metadataFile));
        return records == null ? new JSONArray() : records;
    }

    private void writeMetadataArray(File metadataFile, JSONArray records) throws IOException {
        ensureParentDirectory(metadataFile);
        writeTextFile(metadataFile, records == null ? "[]" : records.toJSONString());
    }

    private void saveMetadataRecord(JSONObject options, SunmiFaceDBRecord record, SunmiFaceFeature feature) throws IOException {
        File metadataFile = resolveMetadataFile(options);
        JSONArray records = readMetadataArray(metadataFile);
        String imgId = record.getImgId();
        if (TextUtils.isEmpty(imgId)) {
            imgId = getString(options, "imgId");
        }
        JSONObject item = new JSONObject();
        item.put("imgId", imgId);
        item.put("id", record.getId());
        item.put("name", record.getName());
        item.put("phone", getString(options, "phone"));
        item.put("photoPath", firstNonEmpty(getString(options, "photoPath"), getString(options, "imagePath")));
        item.put("feature", feature == null ? null : feature.getFeature());
        item.put("createdAt", System.currentTimeMillis());

        int existingIndex = -1;
        for (int i = 0; i < records.size(); i++) {
            JSONObject current = records.getJSONObject(i);
            if (current != null && TextUtils.equals(imgId, current.getString("imgId"))) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            records.set(existingIndex, item);
        } else {
            records.add(item);
        }
        writeMetadataArray(metadataFile, records);
    }

    private JSONArray findImgIdsById(File metadataFile, String id) throws IOException {
        JSONArray imgIds = new JSONArray();
        if (TextUtils.isEmpty(id)) {
            return imgIds;
        }
        JSONArray records = readMetadataArray(metadataFile);
        for (int i = 0; i < records.size(); i++) {
            JSONObject item = records.getJSONObject(i);
            if (item != null && TextUtils.equals(id, item.getString("id"))) {
                String imgId = item.getString("imgId");
                if (!TextUtils.isEmpty(imgId)) {
                    imgIds.add(imgId);
                }
            }
        }
        return imgIds;
    }

    private void removeMetadataRecords(File metadataFile, JSONArray deletedImgIds) throws IOException {
        if (deletedImgIds == null || deletedImgIds.isEmpty()) {
            return;
        }
        JSONArray records = readMetadataArray(metadataFile);
        JSONArray next = new JSONArray();
        for (int i = 0; i < records.size(); i++) {
            JSONObject item = records.getJSONObject(i);
            String imgId = item == null ? null : item.getString("imgId");
            if (!containsString(deletedImgIds, imgId)) {
                next.add(item);
            }
        }
        writeMetadataArray(metadataFile, next);
    }

    private JSONObject findFirstMetadataRecord(JSONObject options, String id) throws IOException {
        if (TextUtils.isEmpty(id)) {
            return null;
        }
        JSONArray records = readMetadataArray(resolveMetadataFile(options));
        for (int i = 0; i < records.size(); i++) {
            JSONObject item = records.getJSONObject(i);
            if (item != null && TextUtils.equals(id, item.getString("id"))) {
                return item;
            }
        }
        return null;
    }

    private boolean containsString(JSONArray array, String value) {
        if (array == null || TextUtils.isEmpty(value)) {
            return false;
        }
        for (int i = 0; i < array.size(); i++) {
            if (TextUtils.equals(value, array.getString(i))) {
                return true;
            }
        }
        return false;
    }

    private String firstNonEmpty(String first, String second) {
        return TextUtils.isEmpty(first) ? second : first;
    }

    private String readAssetConfigValue(String key) throws IOException {
        Context context = getSafeContext();
        if (context == null) {
            return null;
        }
        try (InputStream inputStream = context.getAssets().open("config/config.json")) {
            byte[] bytes = new byte[inputStream.available()];
            int offset = 0;
            int len;
            while (offset < bytes.length
                    && (len = inputStream.read(bytes, offset, bytes.length - offset)) != -1) {
                offset += len;
            }
            JSONObject jsonObject = JSONObject.parseObject(new String(bytes, 0, offset, StandardCharsets.UTF_8));
            return jsonObject.getString(key);
        }
    }

    private void ensureParentDirectory(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("failed to create parent directory: " + parent.getAbsolutePath());
        }
    }

    private void copyAssetIfNeeded(Context context, String assetName, File targetFile) throws IOException {
        if (targetFile.exists() && targetFile.length() > 0) {
            return;
        }
        try (InputStream inputStream = context.getAssets().open("config/" + assetName);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        }
    }

    private byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int offset = 0;
            int readLen;
            while (offset < buffer.length
                    && (readLen = inputStream.read(buffer, offset, buffer.length - offset)) != -1) {
                offset += readLen;
            }
            if (offset < buffer.length) {
                throw new IOException("failed to read file: " + file.getAbsolutePath());
            }
            return buffer;
        }
    }

    private String readTextFile(File file) throws IOException {
        return new String(readFileBytes(file), StandardCharsets.UTF_8);
    }

    private void writeTextFile(File file, String content) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    private boolean looksLikeFilePath(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        return value.startsWith("/") || value.startsWith("file://");
    }

    private void releaseAllCachedFeatures() {
        for (SunmiFaceImageFeatures imageFeatures : featuresCache.values()) {
            try {
                SunmiFaceSDK.releaseImageFeatures(imageFeatures);
            } catch (Exception ignored) {
            }
        }
        featuresCache.clear();
    }

    private void handleRecognizeResult(int resultCode, Intent data) {
        UniJSCallback callback = recognizeCallback;
        JSONObject options = recognizeOptions;
        recognizeCallback = null;
        recognizeOptions = null;
        faceEventSDKInstance = null;

        if (callback == null) {
            return;
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            JSONObject res = error(SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR, "recognize canceled");
            res.put("eventType", "final");
            callback.invoke(res);
            return;
        }

        String imagePath = data.getStringExtra("imagePath");
        if (TextUtils.isEmpty(imagePath)) {
            JSONObject res = error(SunmiFaceStatusCode.FACE_CODE_EMPTY_IMAGE, "captured image is empty");
            res.put("eventType", "final");
            callback.invoke(res);
            return;
        }

        ExtractionResult extractionResult = null;
        SunmiFaceDBRecord record = null;
        try {
            if (options != null && !TextUtils.isEmpty(getString(options, "dbPath"))) {
                SunmiFaceSDK.initDB(resolveDbFilePath(options));
            }

            JSONObject featureOptions = new JSONObject();
            featureOptions.put("imagePath", imagePath);
            featureOptions.put("maxFaceCount", 1);
            featureOptions.put("predictMode", options != null && options.containsKey("predictMode")
                    ? options.getIntValue("predictMode")
                    : SunmiFaceMode.PredictMode_Feature);
            featureOptions.put("livenessMode", options != null && options.containsKey("livenessMode")
                    ? options.getIntValue("livenessMode")
                    : SunmiFaceLivenessMode.LivenessMode_None);
            featureOptions.put("qualityMode", options != null && options.containsKey("qualityMode")
                    ? options.getIntValue("qualityMode")
                    : SunmiFaceQualityMode.QualityMode_None);

            // 配置参数与解码尺寸（用于解决“距离太远/太近不好调”与“闪退/内存问题”）
            if (options != null) {
                if (options.containsKey("threadNum")) featureOptions.put("threadNum", options.getIntValue("threadNum"));
                if (options.containsKey("distanceThreshold")) featureOptions.put("distanceThreshold", options.getFloatValue("distanceThreshold"));
                if (options.containsKey("faceScoreThreshold")) featureOptions.put("faceScoreThreshold", options.getFloatValue("faceScoreThreshold"));
                if (options.containsKey("minFaceSize")) featureOptions.put("minFaceSize", options.getIntValue("minFaceSize"));
                if (options.containsKey("depthXOffset")) featureOptions.put("depthXOffset", options.getIntValue("depthXOffset"));
                if (options.containsKey("depthYOffset")) featureOptions.put("depthYOffset", options.getIntValue("depthYOffset"));
                if (options.containsKey("boxSortMode")) featureOptions.put("boxSortMode", options.getIntValue("boxSortMode"));
                if (options.containsKey("decodeMaxSize")) featureOptions.put("decodeMaxSize", options.getIntValue("decodeMaxSize"));
            }

            // 允许前端指定 faceRect 坐标变换，解决“画框倒了/偏了”
            if (options != null && options.containsKey("rectRotation")) {
                featureOptions.put("rectRotation", options.getIntValue("rectRotation"));
            }
            if (options != null && options.containsKey("rectMirrorX")) {
                featureOptions.put("rectMirrorX", options.getBooleanValue("rectMirrorX"));
            }

            extractionResult = extractFeatures(featureOptions);
            JSONObject payload = new JSONObject();
            payload.put("imagePath", imagePath);
            payload.put("feature", extractionResult.toJson());

            if (extractionResult.code != SunmiFaceStatusCode.FACE_CODE_OK) {
                // 关闭系统人脸检测时，也需要给前端一个明确的“未检测到人脸/识别失败”状态
                JSONObject evt = new JSONObject();
                evt.put("eventType", "face_not_detected");
                evt.put("message", "未检测到人脸");
                emitFaceEvent(evt);

                JSONObject res = status(extractionResult.code, payload, null);
                res.put("eventType", "final");
                callback.invoke(res);
                return;
            }

            if (extractionResult.feature == null) {
                JSONObject evt = new JSONObject();
                evt.put("eventType", "face_not_detected");
                evt.put("message", "未检测到人脸特征");
                emitFaceEvent(evt);

                JSONObject res = status(extractionResult.code, payload, null);
                res.put("eventType", "final");
                callback.invoke(res);
                return;
            }

            record = SunmiFaceSDK.faceFeature2FaceDBRecord(extractionResult.feature);
            SunmiFaceDBIdInfo info = new SunmiFaceDBIdInfo();
            int searchCode = SunmiFaceSDK.searchDB(record, info);
            payload.put("search", dbIdInfoToJson(info));
            if (searchCode == SunmiFaceStatusCode.FACE_CODE_OK && info.getIsMatched()) {
                JSONObject metadata = findFirstMetadataRecord(options, info.getId());
                if (metadata != null) {
                    payload.put("metadata", metadata);
                }
                JSONObject evt = new JSONObject();
                evt.put("eventType", "recognize_success");
                evt.put("message", "识别成功");
                emitFaceEvent(evt);
            } else {
                JSONObject evt = new JSONObject();
                evt.put("eventType", "recognize_failed");
                evt.put("message", searchCode == SunmiFaceStatusCode.FACE_CODE_OK ? "未匹配到人脸" : "识别失败: " + SunmiFaceSDK.getErrorString(searchCode));
                emitFaceEvent(evt);
            }

            JSONObject res = status(searchCode, payload, searchCode == SunmiFaceStatusCode.FACE_CODE_OK ? "face recognize success" : null);
            res.put("eventType", "final");
            callback.invoke(res);
        } catch (Exception e) {
            JSONObject evt = new JSONObject();
            evt.put("eventType", "recognize_failed");
            evt.put("message", "识别异常: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
            emitFaceEvent(evt);

            JSONObject res = exception(e);
            res.put("eventType", "final");
            callback.invoke(res);
        } finally {
            if (extractionResult != null && extractionResult.feature != null) {
                try {
                    extractionResult.feature.delete();
                } catch (Exception ignored) {
                }
            }
            if (extractionResult != null && extractionResult.imageFeatures != null) {
                try {
                    SunmiFaceSDK.releaseImageFeatures(extractionResult.imageFeatures);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (TextUtils.isEmpty(source) || TextUtils.isEmpty(target)) {
            return false;
        }
        return source.toLowerCase().contains(target.toLowerCase());
    }

    private static class FeatureCarrier {
        final SunmiFaceFeature feature;
        final SunmiFaceImageFeatures imageFeatures;
        final boolean owned;

        FeatureCarrier(SunmiFaceFeature feature, SunmiFaceImageFeatures imageFeatures, boolean owned) {
            this.feature = feature;
            this.imageFeatures = imageFeatures;
            this.owned = owned;
        }
    }

    private static class ExtractionResult {
        int code = SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR;
        boolean owned;
        String token;
        SunmiFaceFeature feature;
        SunmiFaceImageFeatures imageFeatures;
        JSONObject data = new JSONObject();

        JSONObject toJson() {
            JSONObject result = new JSONObject();
            result.putAll(data);
            if (!TextUtils.isEmpty(token)) {
                result.put("token", token);
            }
            return result;
        }
    }

    private static class ImageBuildResult {
        final SunmiFaceImage image;
        final int width;
        final int height;

        ImageBuildResult(SunmiFaceImage image, int width, int height) {
            this.image = image;
            this.width = width;
            this.height = height;
        }
    }
}
