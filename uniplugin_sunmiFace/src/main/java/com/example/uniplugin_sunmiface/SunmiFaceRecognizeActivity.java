package com.example.uniplugin_sunmiface;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.sunmi.facelib.SunmiFaceLivenessMode;
import com.sunmi.facelib.SunmiFaceMode;
import com.sunmi.facelib.SunmiFaceQualityMode;
import com.sunmi.facelib.SunmiFaceSDK;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
public class SunmiFaceRecognizeActivity extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback, Camera.FaceDetectionListener {

    private FrameLayout rootView;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private TextView statusView;
    private boolean isCapturing = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean preferFrontCamera = true;
    private int autoCaptureDelayMs = 800;
    private boolean showCancelButton = true;
    private boolean showSwitchCameraButton = true;
    private boolean showStatusText = true;
    private boolean enableSystemFaceDetection = false;
    private boolean autoStartAnalyze = true;
    private boolean detectionEnabled = true;
    // 360 表示自动计算
    private int displayOrientationDeg = 360;
    // 360 表示自动计算
    private int captureImageRotationDeg = 360;
    private boolean captureMirrorX = false;
    private boolean pendingAutoCapture = false;
    private boolean lastFacePresent = false;
    private volatile boolean destroyed = false;
    private volatile boolean finishing = false;
    private String lastStatusMessage = "";
    private int recognizePredictMode = SunmiFaceMode.PredictMode_Feature;
    private int recognizeLivenessMode = SunmiFaceLivenessMode.LivenessMode_None;
    private int recognizeQualityMode = SunmiFaceQualityMode.QualityMode_None;
    private int recognizeMaxFaceCount = 1;
    private int analyzeIntervalMs = 700;
    private int previewDecodeMaxSize = 640;
    private int minFaceSize = 0;
    private float distanceThreshold = 0f;
    private int openedCameraId = 0;
    private int appliedDisplayOrientationDeg = 90;
    private int appliedJpegRotationDeg = 0;
    private final Runnable autoCaptureRunnable = new Runnable() {
        @Override
        public void run() {
            pendingAutoCapture = false;
            capture();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SunmiFaceKeepAliveService.start(this);
        preferFrontCamera = getIntent().getBooleanExtra("preferFrontCamera", true);
        autoCaptureDelayMs = Math.max(300, getIntent().getIntExtra("autoCaptureDelayMs", 800));
        showCancelButton = getIntent().getBooleanExtra("showCancelButton", true);
        showSwitchCameraButton = getIntent().getBooleanExtra("showSwitchCameraButton", true);
        showStatusText = getIntent().getBooleanExtra("showStatusText", true);
        enableSystemFaceDetection = getIntent().getBooleanExtra("enableSystemFaceDetection", false);
        autoStartAnalyze = !getIntent().hasExtra("autoStartAnalyze") || getIntent().getBooleanExtra("autoStartAnalyze", true);
        detectionEnabled = autoStartAnalyze;
        displayOrientationDeg = getIntent().getIntExtra("displayOrientationDeg", 360);
        captureImageRotationDeg = getIntent().getIntExtra("captureImageRotationDeg", 360);
        captureMirrorX = getIntent().getBooleanExtra("captureMirrorX", false);
        recognizePredictMode = getIntent().getIntExtra("predictMode", SunmiFaceMode.PredictMode_Feature);
        recognizeLivenessMode = getIntent().getIntExtra("livenessMode", SunmiFaceLivenessMode.LivenessMode_None);
        recognizeQualityMode = getIntent().getIntExtra("qualityMode", SunmiFaceQualityMode.QualityMode_None);
        recognizeMaxFaceCount = Math.max(1, getIntent().getIntExtra("maxFaceCount", 1));
        analyzeIntervalMs = Math.max(300, getIntent().getIntExtra("analyzeIntervalMs", 700));
        previewDecodeMaxSize = Math.max(240, getIntent().getIntExtra("previewDecodeMaxSize", 640));
        minFaceSize = Math.max(0, getIntent().getIntExtra("minFaceSize", 0));
        distanceThreshold = getIntent().getFloatExtra("distanceThreshold", 0f);
        setContentView(buildContentView());
    }

    private View buildContentView() {
        rootView = new FrameLayout(this);
        rootView.setBackgroundColor(Color.BLACK);

        surfaceView = new SurfaceView(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        rootView.addView(surfaceView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        if (showCancelButton || showSwitchCameraButton || showStatusText) {
            LinearLayout bottomBar = new LinearLayout(this);
            bottomBar.setOrientation(LinearLayout.VERTICAL);
            bottomBar.setPadding(32, 32, 32, 48);
            bottomBar.setBackgroundColor(0x66000000);

            if (showStatusText) {
                statusView = new TextView(this);
                statusView.setTextColor(Color.WHITE);
                statusView.setTextSize(16);
                statusView.setText("请将人脸对准镜头，检测到人脸后会自动识别");
                bottomBar.addView(statusView);
            } else {
                statusView = new TextView(this);
                statusView.setVisibility(View.GONE);
                bottomBar.addView(statusView);
            }

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER_HORIZONTAL);
            actions.setPadding(0, 24, 0, 0);

            if (showCancelButton) {
                Button cancelButton = new Button(this);
                cancelButton.setText("取消");
                cancelButton.setOnClickListener(v -> {
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                });
                actions.addView(cancelButton);
            }

            if (showSwitchCameraButton) {
                Button switchButton = new Button(this);
                switchButton.setText("切换摄像头");
                switchButton.setOnClickListener(v -> switchCamera());
                LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                switchParams.leftMargin = 32;
                actions.addView(switchButton, switchParams);
            }

            if (!autoStartAnalyze || !enableSystemFaceDetection) {
                Button detectButton = new Button(this);
                detectButton.setText(enableSystemFaceDetection ? "开始检测" : "拍照识别");
                detectButton.setOnClickListener(v -> {
                    detectionEnabled = true;
                    if (statusView != null) {
                        statusView.setText(enableSystemFaceDetection ? "正在检测，请将人脸对准镜头" : "正在拍照识别...");
                    }
                    if (enableSystemFaceDetection) {
                        startFaceDetectionIfSupported();
                    } else {
                        capture();
                    }
                });
                LinearLayout.LayoutParams detectParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                detectParams.leftMargin = 32;
                actions.addView(detectButton, detectParams);
            }

            bottomBar.addView(actions);

            FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            bottomParams.gravity = Gravity.BOTTOM;
            rootView.addView(bottomBar, bottomParams);
        } else {
            statusView = null;
        }
        return rootView;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // 如果运行时没有 CAMERA 权限，避免 Camera.open 直接触发系统级崩溃
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (statusView != null) statusView.setText("未获得相机权限");
                emitEvent("final", "camera_permission_missing");
                releaseCamera();
                finish();
                return;
            }
            openedCameraId = resolveCameraId();
            camera = Camera.open(openedCameraId);
            camera.setPreviewDisplay(holder);
            appliedDisplayOrientationDeg = resolveAppliedDisplayOrientation(openedCameraId);
            camera.setDisplayOrientation(appliedDisplayOrientationDeg);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPictureFormat(android.graphics.PixelFormat.JPEG);
            applyBestCameraSize(parameters);
            appliedJpegRotationDeg = resolveCaptureImageRotation(openedCameraId);
            parameters.setRotation(appliedJpegRotationDeg);
            camera.setParameters(parameters);
            updatePreviewLayout(parameters);
            applyFaceConfig();
            // 只有启用系统人脸检测时才注册 listener，避免某些机型底层崩溃
            if (enableSystemFaceDetection) {
                camera.setFaceDetectionListener(this);
            } else {
                try {
                    camera.setFaceDetectionListener(null);
                } catch (Exception ignored) {
                }
            }
            camera.startPreview();
            startFaceDetectionIfSupported();
            emitStatus("ready", autoStartAnalyze && enableSystemFaceDetection ? "预览已开启，请将人脸对准镜头" : "预览已开启");
        } catch (Exception e) {
            if (statusView != null) statusView.setText("打开相机失败: " + e.getMessage());
            releaseCamera();
            emitEvent("final", "camera_open_failed:" + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        cancelAutoCapture();
        finishing = true;
        releaseCamera();
    }

    private void capture() {
        if (camera == null || isCapturing || finishing || destroyed) {
            return;
        }
        isCapturing = true;
        if (statusView != null) statusView.setText("正在拍照识别...");
        emitEvent("capturing", "正在拍照识别...");
        try {
            // 拍照前先停掉人脸检测，避免 takePicture/stopFaceDetection 并发导致底层状态异常
            try {
                camera.setFaceDetectionListener(null);
            } catch (Exception ignored) {
            }
            try {
                camera.stopFaceDetection();
            } catch (Exception ignored) {
            }
            camera.takePicture(null, null, this);
        } catch (Exception e) {
            isCapturing = false;
            if (statusView != null) statusView.setText("拍照失败: " + e.getMessage());
            emitEvent("final", "capture_failed:" + e.getMessage());
        }
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        if (!enableSystemFaceDetection || !detectionEnabled || finishing || destroyed || isCapturing) {
            return;
        }
        boolean hasFace = faces != null && faces.length > 0;
        lastFacePresent = hasFace;
        if (!hasFace) {
            cancelAutoCapture();
            if (statusView != null) {
                statusView.setText("未检测到人脸");
            }
            return;
        }
        if (statusView != null) {
            statusView.setText("检测到人脸，准备识别...");
        }
        if (!pendingAutoCapture) {
            pendingAutoCapture = true;
            mainHandler.postDelayed(autoCaptureRunnable, autoCaptureDelayMs);
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        try {
            finishing = true;
            cancelAutoCapture();
            File imageFile = new File(getCacheDir(), "sunmi-face-capture-" + System.currentTimeMillis() + ".jpg");
            // 如果需要对齐前端坐标系/预览方向，就在保存阶段把图片做一次旋转/镜像
            int finalCaptureRotationDeg = captureImageRotationDeg == 360 ? 0 : normalizeRotation(captureImageRotationDeg);
            boolean shouldMirror = captureMirrorX;
            if (finalCaptureRotationDeg != 0 || shouldMirror) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (bitmap != null) {
                    Matrix matrix = new Matrix();
                    if (finalCaptureRotationDeg != 0) {
                        matrix.postRotate(finalCaptureRotationDeg);
                    }
                    if (shouldMirror) {
                        // 围绕位图中心做水平翻转，避免平移导致裁剪偏差
                        matrix.postScale(-1, 1, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
                    }
                    Bitmap out = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                    try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                        out.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                        outputStream.flush();
                    }
                    out.recycle();
                } else {
                    // 解码失败则回退：直接保存原始 jpeg bytes
                    try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                        outputStream.write(data);
                        outputStream.flush();
                    }
                }
            } else {
                try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                    outputStream.write(data);
                    outputStream.flush();
                }
            }
            Intent intent = new Intent();
            intent.putExtra("imagePath", imageFile.getAbsolutePath());
            setResult(Activity.RESULT_OK, intent);
            // 有些机型在 finish 之后异步释放相机会触发 native 崩溃，成功路径先主动释放
            releaseCamera();
            finish();
        } catch (IOException e) {
            isCapturing = false;
            pendingAutoCapture = false;
            if (statusView != null) statusView.setText("保存照片失败: " + e.getMessage());
            try {
                camera.startPreview();
                startFaceDetectionIfSupported();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
        finishing = true;
        cancelAutoCapture();
        releaseCamera();
        SunmiFaceKeepAliveService.stop(this);
    }

    private void startFaceDetectionIfSupported() {
        if (!enableSystemFaceDetection || !detectionEnabled || camera == null || finishing || destroyed) {
            return;
        }
        try {
            camera.startFaceDetection();
        } catch (Exception ignored) {
        }
    }

    private void applyBestCameraSize(Camera.Parameters parameters) {
        Camera.Size previewSize = chooseBestSize(parameters.getSupportedPreviewSizes(), 16f / 9f, 1280);
        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }
        Camera.Size pictureSize = chooseBestSize(parameters.getSupportedPictureSizes(), 16f / 9f, 1920);
        if (pictureSize != null) {
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
        }
    }

    private Camera.Size chooseBestSize(java.util.List<Camera.Size> sizes, float targetRatio, int maxWidth) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        Camera.Size best = null;
        float bestScore = Float.MAX_VALUE;
        for (Camera.Size size : sizes) {
            if (size.width > maxWidth) {
                continue;
            }
            float ratio = (float) size.width / (float) size.height;
            float score = Math.abs(ratio - targetRatio) + (Math.abs(maxWidth - size.width) / 1000f);
            if (best == null || score < bestScore) {
                best = size;
                bestScore = score;
            }
        }
        return best == null ? sizes.get(0) : best;
    }

    private void updatePreviewLayout(Camera.Parameters parameters) {
        Camera.Size size = parameters.getPreviewSize();
        if (size == null || surfaceView == null) {
            return;
        }
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        float previewRatio = (float) size.height / (float) size.width;
        int targetWidth = screenWidth;
        int targetHeight = (int) (targetWidth / previewRatio);
        if (targetHeight < screenHeight) {
            targetHeight = screenHeight;
            targetWidth = (int) (targetHeight * previewRatio);
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(targetWidth, targetHeight);
        params.gravity = Gravity.CENTER;
        surfaceView.setLayoutParams(params);
    }

    private int resolveCameraId() {
        int cameraCount = Camera.getNumberOfCameras();
        int fallbackId = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (preferFrontCamera && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
            if (!preferFrontCamera && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return fallbackId;
    }

    private int resolveAppliedDisplayOrientation(int cameraId) {
        if (displayOrientationDeg != 360) {
            return normalizeRotation(displayOrientationDeg);
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = getWindowRotationDegrees();
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            int result = (info.orientation + degrees) % 360;
            return (360 - result) % 360;
        }
        return (info.orientation - degrees + 360) % 360;
    }

    private int resolveCaptureImageRotation(int cameraId) {
        if (captureImageRotationDeg != 360) {
            return normalizeRotation(captureImageRotationDeg);
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = getWindowRotationDegrees();
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (info.orientation + degrees) % 360;
        }
        return (info.orientation - degrees + 360) % 360;
    }

    private int getWindowRotationDegrees() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case android.view.Surface.ROTATION_90:
                return 90;
            case android.view.Surface.ROTATION_180:
                return 180;
            case android.view.Surface.ROTATION_270:
                return 270;
            case android.view.Surface.ROTATION_0:
            default:
                return 0;
        }
    }

    private int normalizeRotation(int deg) {
        int normalized = deg % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    private void cancelAutoCapture() {
        pendingAutoCapture = false;
        mainHandler.removeCallbacks(autoCaptureRunnable);
    }

    private void switchCamera() {
        // switchCameraButton 被隐藏时，仍然不允许切换（避免误触）
        if (!showSwitchCameraButton || finishing || destroyed) return;
        preferFrontCamera = !preferFrontCamera;
        isCapturing = false;
        cancelAutoCapture();
        releaseCamera();
        try {
            if (surfaceHolder != null) {
                surfaceCreated(surfaceHolder);
            }
        } catch (Exception e) {
            if (statusView != null) statusView.setText("切换摄像头失败: " + e.getMessage());
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
            } catch (Exception ignored) {
            }
            try {
                camera.setFaceDetectionListener(null);
            } catch (Exception ignored) {
            }
            try {
                if (enableSystemFaceDetection) {
                    camera.stopFaceDetection();
                }
            } catch (Exception ignored) {
            }
            try {
                camera.stopPreview();
            } catch (Exception ignored) {
            }
            camera.release();
            camera = null;
        }
    }

    private void applyFaceConfig() {
        try {
            com.sunmi.facelib.SunmiFaceConfigParam param = new com.sunmi.facelib.SunmiFaceConfigParam();
            SunmiFaceSDK.getConfig(param);
            if (minFaceSize > 0) {
                param.setMinFaceSize(minFaceSize);
            }
            if (distanceThreshold > 0) {
                param.setDistanceThreshold(distanceThreshold);
            }
            SunmiFaceSDK.setConfig(param);
        } catch (Exception ignored) {
        }
    }

    private void emitStatus(String eventType, String message) {
        emitStatus(eventType, message, null);
    }

    private void emitStatus(String eventType, String message, JSONObject featureStatus) {
        mainHandler.post(() -> {
            if (destroyed) return;
            if (statusView != null && message != null) {
                statusView.setText(message);
            }
            if (message != null && message.equals(lastStatusMessage)) {
                return;
            }
            lastStatusMessage = message == null ? "" : message;
            emitEvent(eventType, message, featureStatus);
        });
    }

    private void emitEvent(String eventType, String message) {
        emitEvent(eventType, message, null);
    }

    private void emitEvent(String eventType, String message, JSONObject featureStatus) {
        // 原生相机页在前台时，持续向 uni 的 JS 引擎推送全局事件会导致部分设备/基座报
        // “original owner has die” 并直接闪退。实时提示改为只显示在原生页面内，
        // 最终结果仍通过 onActivityResult 返回给前端。
    }

    private void mergeFeatureStatus(JSONObject event, JSONObject featureStatus) {
        if (event == null || featureStatus == null) {
            return;
        }
        try {
            event.putAll(featureStatus);
        } catch (Throwable ignored) {
        }
    }
}
