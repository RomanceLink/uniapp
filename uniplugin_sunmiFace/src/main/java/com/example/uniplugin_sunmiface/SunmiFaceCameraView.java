package com.example.uniplugin_sunmiface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sunmi.facelib.SunmiFaceDBIdInfo;
import com.sunmi.facelib.SunmiFaceDBRecord;
import com.sunmi.facelib.SunmiFaceFeature;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SunmiFaceCameraView extends FrameLayout implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {
    public interface Listener {
        void onStatus(JSONObject event);
        void onRecognize(JSONObject event);
        void onError(JSONObject event);
    }

    private final TextureView textureView;
    private final FaceBoxOverlayView faceBoxOverlayView;
    private final TextView overlayView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService analyzerExecutor = Executors.newSingleThreadExecutor();
    private final Object sdkLock = new Object();

    private Listener listener;
    private Camera camera;
    private int appliedDisplayOrientationDeg = 90;
    private boolean running = false;
    private boolean detecting = false;
    private boolean previewReady = false;
    private boolean destroyed = false;
    private boolean analyzingFrame = false;
    private boolean sdkHandleReady = false;
    private long lastAnalyzeAt = 0L;
    private String lastMessage = "";
    private String initializedDbPath = "";

    private String cameraFacing = "front";
    private int displayOrientationDeg = 90;
    private int analyzeIntervalMs = 700;
    private int previewDecodeMaxSize = 480;
    private int predictMode = SunmiFaceMode.PredictMode_Feature;
    private int livenessMode = SunmiFaceLivenessMode.LivenessMode_None;
    private int qualityMode = SunmiFaceQualityMode.QualityMode_None;
    private int maxFaceCount = 1;
    private int minFaceSize = 0;
    private float distanceThreshold = 0f;
    private String dbPath = "";
    private boolean autoStopOnRecognize = true;

    public SunmiFaceCameraView(Context context) {
        super(context);
        textureView = new TextureView(context);
        textureView.setSurfaceTextureListener(this);
        addView(textureView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        faceBoxOverlayView = new FaceBoxOverlayView(context);
        addView(faceBoxOverlayView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        overlayView = new TextView(context);
        overlayView.setTextColor(0xFFFFFFFF);
        overlayView.setTextSize(16);
        overlayView.setBackgroundColor(0x66000000);
        overlayView.setPadding(24, 16, 24, 16);
        overlayView.setText("未启动预览");
        LayoutParams overlayParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        overlayParams.gravity = Gravity.BOTTOM;
        addView(overlayView, overlayParams);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setRunning(boolean running) {
        this.running = running;
        if (running) {
            openCameraIfPossible();
        } else {
            clearFaceOverlay();
            stopPreviewAndRelease();
            emitStatus("preview_stopped", "预览已关闭", null);
        }
    }

    public void setDetecting(boolean detecting) {
        this.detecting = detecting;
        if (detecting) {
            emitStatus("detecting", "正在检测，请将人脸对准镜头", null);
        } else if (running) {
            clearFaceOverlay();
            emitStatus("preview_ready", "预览已开启", null);
        } else {
            clearFaceOverlay();
            emitStatus("idle", "未启动预览", null);
        }
    }

    public void setCameraFacing(String cameraFacing) {
        this.cameraFacing = TextUtils.isEmpty(cameraFacing) ? "front" : cameraFacing;
        restartIfRunning();
    }

    public void setDisplayOrientationDeg(int displayOrientationDeg) {
        this.displayOrientationDeg = displayOrientationDeg;
        restartIfRunning();
    }

    public void setAnalyzeIntervalMs(int analyzeIntervalMs) {
        this.analyzeIntervalMs = Math.max(300, analyzeIntervalMs);
    }

    public void setPreviewDecodeMaxSize(int previewDecodeMaxSize) {
        this.previewDecodeMaxSize = Math.max(240, previewDecodeMaxSize);
    }

    public void setPredictMode(int predictMode) {
        this.predictMode = predictMode;
    }

    public void setLivenessMode(int livenessMode) {
        this.livenessMode = livenessMode;
    }

    public void setQualityMode(int qualityMode) {
        this.qualityMode = qualityMode;
    }

    public void setMaxFaceCount(int maxFaceCount) {
        this.maxFaceCount = Math.max(1, maxFaceCount);
    }

    public void setMinFaceSize(int minFaceSize) {
        this.minFaceSize = Math.max(0, minFaceSize);
    }

    public void setDistanceThreshold(float distanceThreshold) {
        this.distanceThreshold = distanceThreshold;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath == null ? "" : dbPath;
    }

    public void setAutoStopOnRecognize(boolean autoStopOnRecognize) {
        this.autoStopOnRecognize = autoStopOnRecognize;
    }

    public void destroyView() {
        destroyed = true;
        running = false;
        detecting = false;
        clearFaceOverlay();
        stopPreviewAndRelease();
        analyzerExecutor.shutdownNow();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        previewReady = true;
        openCameraIfPossible();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        previewReady = false;
        stopPreviewAndRelease();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!running || !detecting || destroyed || camera == null || data == null || analyzingFrame) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAnalyzeAt < analyzeIntervalMs) {
            return;
        }
        lastAnalyzeAt = now;
        Camera.Size previewSize;
        try {
            previewSize = camera.getParameters().getPreviewSize();
        } catch (Exception e) {
            emitError("preview_size_error", "读取预览尺寸失败: " + safeMessage(e));
            return;
        }
        if (previewSize == null) {
            return;
        }
        final byte[] frameCopy = data.clone();
        final int width = previewSize.width;
        final int height = previewSize.height;
        analyzingFrame = true;
        analyzerExecutor.execute(() -> analyzeFrame(frameCopy, width, height));
    }

    private void analyzeFrame(byte[] data, int width, int height) {
        SunmiFaceImageFeatures imageFeatures = null;
        SunmiFaceFeature feature = null;
        SunmiFaceDBRecord record = null;
        try {
            Bitmap bitmap = decodePreviewBitmap(data, width, height);
            if (bitmap == null) {
                clearFaceOverlay();
                emitStatus("frame_decode_failed", "画面解析失败", null);
                return;
            }
            byte[] bgr = bitmapToBgr(bitmap);
            int imgWidth = bitmap.getWidth();
            int imgHeight = bitmap.getHeight();
            bitmap.recycle();

            ensureSdkReady();

            SunmiFaceImage image = new SunmiFaceImage(bgr, imgHeight, imgWidth, maxFaceCount);
            image.setPredictMode(predictMode);
            image.setLivenessMode(livenessMode);
            image.setQualityMode(qualityMode);
            try {
                imageFeatures = new SunmiFaceImageFeatures();
                int code = SunmiFaceSDK.getImageFeatures(image, imageFeatures);

                if (code != SunmiFaceStatusCode.FACE_CODE_OK) {
                    clearFaceOverlay();
                    emitStatus("sdk_error", "识别失败: " + SunmiFaceSDK.getErrorString(code), buildSimpleData(code));
                    return;
                }
                int featuresCount = imageFeatures.getFeaturesCount();
                if (featuresCount <= 0) {
                    clearFaceOverlay();
                    emitStatus("face_not_detected", "未检测到人脸", null);
                    return;
                }

                feature = SunmiFaceLib.SunmiFaceFeatureArrayGetItem(imageFeatures.getFeatures(), 0);
                if (feature == null) {
                    clearFaceOverlay();
                    emitStatus("face_not_detected", "未检测到人脸", null);
                    return;
                }

                JSONObject recognizeData = new JSONObject();
                recognizeData.put("featuresCount", featuresCount);
                emitStatus("face_detected", "检测到人脸，正在识别...", recognizeData);
                record = SunmiFaceSDK.faceFeature2FaceDBRecord(feature);
                SunmiFaceDBIdInfo info = new SunmiFaceDBIdInfo();
                int searchCode = SunmiFaceSDK.searchDB(record, info);
                recognizeData.put("searchCode", searchCode);
                recognizeData.put("searchMessage", SunmiFaceSDK.getErrorString(searchCode));

                if (searchCode == SunmiFaceStatusCode.FACE_CODE_OK && info.getIsMatched()) {
                    detecting = false;
                    recognizeData.put("matched", true);
                    recognizeData.put("id", info.getId());
                    recognizeData.put("name", info.getName());
                    recognizeData.put("distance", info.getDistance());
                    emitRecognize("recognize_success", "人脸识别成功", recognizeData);
                    if (autoStopOnRecognize) {
                        scheduleAutoStopAfterRecognize();
                    }
                } else {
                    recognizeData.put("matched", false);
                    emitStatus("recognize_failed", searchCode == SunmiFaceStatusCode.FACE_CODE_OK ? "未匹配到人脸" : "识别失败", recognizeData);
                }
            } finally {
                try {
                    image.delete();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            clearFaceOverlay();
            emitError("recognize_exception", "识别异常: " + safeMessage(e));
        } finally {
            if (record != null) {
                try {
                    record.delete();
                } catch (Exception ignored) {
                }
            }
            if (feature != null) {
                try {
                    feature.delete();
                } catch (Exception ignored) {
                }
            }
            if (imageFeatures != null) {
                try {
                    SunmiFaceSDK.releaseImageFeatures(imageFeatures);
                } catch (Exception ignored) {
                }
            }
            analyzingFrame = false;
        }
    }

    private void ensureSdkReady() {
        synchronized (sdkLock) {
            if (!sdkHandleReady) {
                int code = SunmiFaceSDK.createHandle();
                if (code != SunmiFaceStatusCode.FACE_CODE_OK) {
                    throw new IllegalStateException("createHandle failed: " + SunmiFaceSDK.getErrorString(code));
                }
                sdkHandleReady = true;
            }

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

            if (!TextUtils.isEmpty(dbPath)) {
                File dbFile = new File(dbPath);
                String finalDbPath = dbFile.isDirectory() ? new File(dbFile, "sunmi_face.db").getAbsolutePath() : dbFile.getAbsolutePath();
                if (!finalDbPath.equals(initializedDbPath)) {
                    int dbCode = SunmiFaceSDK.initDB(finalDbPath);
                    if (dbCode != SunmiFaceStatusCode.FACE_CODE_OK) {
                        throw new IllegalStateException("initDB failed: " + SunmiFaceSDK.getErrorString(dbCode));
                    }
                    initializedDbPath = finalDbPath;
                }
            }
        }
    }

    private Bitmap decodePreviewBitmap(byte[] data, int width, int height) throws Exception {
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 60, outputStream);
        byte[] jpegBytes = outputStream.toByteArray();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = calculateSampleSize(width, height, previewDecodeMaxSize);
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length, options);
    }

    private void scheduleAutoStopAfterRecognize() {
        mainHandler.postDelayed(() -> {
            if (destroyed) {
                return;
            }
            detecting = false;
            running = false;
            stopPreviewAndRelease();
        }, 300);
    }

    private int calculateSampleSize(int width, int height, int maxSize) {
        int sample = 1;
        while (width / sample > maxSize || height / sample > maxSize) {
            sample *= 2;
        }
        return Math.max(1, sample);
    }

    private byte[] bitmapToBgr(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        byte[] bgr = new byte[width * height * 3];
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            bgr[i * 3] = (byte) (pixel & 0xFF);
            bgr[i * 3 + 1] = (byte) ((pixel >> 8) & 0xFF);
            bgr[i * 3 + 2] = (byte) ((pixel >> 16) & 0xFF);
        }
        return bgr;
    }

    private String evaluateFaceQuality(SunmiFaceFeature feature) {
        if (feature == null) {
            return "未检测到人脸";
        }
        if ((qualityMode & SunmiFaceQualityMode.QualityMode_Occlusion) != 0 && feature.getOcclusionScore() > 0.6f) {
            return "人脸遮挡严重，请露出五官";
        }
        if ((qualityMode & SunmiFaceQualityMode.QualityMode_Pose) != 0) {
            SunmiFacePose pose = feature.getPose();
            if (pose != null) {
                if (Math.abs(pose.getYaw()) > 20f) {
                    return "请正对镜头";
                }
                if (Math.abs(pose.getPitch()) > 20f) {
                    return "请保持平视";
                }
                if (Math.abs(pose.getRoll()) > 20f) {
                    return "请端正头部";
                }
            }
            if (feature.getLuminance() < 40f) {
                return "光线过暗，请靠近亮处";
            }
            if (feature.getVarLaplacian() < 20f) {
                return "画面较模糊，请保持稳定";
            }
        }
        if (livenessMode == SunmiFaceLivenessMode.LivenessMode_RGB && feature.getRgbLivenessScore() < 0.6f) {
            return "请使用真人面对镜头";
        }
        return null;
    }

    private JSONObject buildFeatureData(SunmiFaceFeature feature, int imageWidth, int imageHeight) {
        JSONObject data = new JSONObject();
        if (feature == null) {
            return data;
        }
        try {
            data.put("faceRect", faceRectToJson(feature.getFaceRect(), imageWidth, imageHeight));
            data.put("landmark", landmarkToJson(feature, imageWidth, imageHeight));
            data.put("age", ageToJson(feature));
            data.put("gender", genderToJson(feature));
            data.put("occlusionScore", feature.getOcclusionScore());
            data.put("luminance", feature.getLuminance());
            data.put("varLaplacian", feature.getVarLaplacian());
            data.put("rgbLivenessScore", feature.getRgbLivenessScore());
            SunmiFacePose pose = feature.getPose();
            if (pose != null) {
                JSONObject poseData = new JSONObject();
                poseData.put("yaw", pose.getYaw());
                poseData.put("pitch", pose.getPitch());
                poseData.put("roll", pose.getRoll());
                data.put("pose", poseData);
            }
        } catch (Exception ignored) {
        }
        return data;
    }

    private JSONObject faceRectToJson(SunmiFaceRect rect, int imageWidth, int imageHeight) {
        if (rect == null || imageWidth <= 0 || imageHeight <= 0) {
            return null;
        }
        float left = Math.min(rect.getX1(), rect.getX2());
        float right = Math.max(rect.getX1(), rect.getX2());
        float top = Math.min(rect.getY1(), rect.getY2());
        float bottom = Math.max(rect.getY1(), rect.getY2());

        float[] p1 = transformPreviewPoint(left, top, imageWidth, imageHeight);
        float[] p2 = transformPreviewPoint(right, top, imageWidth, imageHeight);
        float[] p3 = transformPreviewPoint(left, bottom, imageWidth, imageHeight);
        float[] p4 = transformPreviewPoint(right, bottom, imageWidth, imageHeight);

        float nx1 = Math.min(Math.min(p1[0], p2[0]), Math.min(p3[0], p4[0]));
        float nx2 = Math.max(Math.max(p1[0], p2[0]), Math.max(p3[0], p4[0]));
        float ny1 = Math.min(Math.min(p1[1], p2[1]), Math.min(p3[1], p4[1]));
        float ny2 = Math.max(Math.max(p1[1], p2[1]), Math.max(p3[1], p4[1]));

        int outWidth = isSwapDimensions() ? imageHeight : imageWidth;
        int outHeight = isSwapDimensions() ? imageWidth : imageHeight;
        JSONObject json = new JSONObject();
        json.put("x1", clamp01(nx1 / outWidth));
        json.put("y1", clamp01(ny1 / outHeight));
        json.put("x2", clamp01(nx2 / outWidth));
        json.put("y2", clamp01(ny2 / outHeight));
        json.put("score", rect.getScore());
        return json;
    }

    private JSONObject landmarkToJson(SunmiFaceFeature feature, int imageWidth, int imageHeight) {
        if (feature == null || feature.getLandmark() == null || imageWidth <= 0 || imageHeight <= 0) {
            return null;
        }
        int outWidth = isSwapDimensions() ? imageHeight : imageWidth;
        int outHeight = isSwapDimensions() ? imageWidth : imageHeight;
        JSONArray points = new JSONArray();
        for (int i = 0; i < SunmiFaceLibConstants.SUNMI_FACE_LANDMARK_LEN; i++) {
            float[] p = transformPreviewPoint(
                    SunmiFaceLib.SunmiFaceLmkArrayGetItem(feature.getLandmark(), i).getX(),
                    SunmiFaceLib.SunmiFaceLmkArrayGetItem(feature.getLandmark(), i).getY(),
                    imageWidth,
                    imageHeight
            );
            JSONObject point = new JSONObject();
            point.put("index", i);
            point.put("x", clamp01(p[0] / outWidth));
            point.put("y", clamp01(p[1] / outHeight));
            points.add(point);
        }
        JSONObject json = new JSONObject();
        json.put("points", points);
        return json;
    }

    private JSONObject ageToJson(SunmiFaceFeature feature) {
        if (feature == null || feature.getAge() == null) {
            return null;
        }
        JSONObject json = new JSONObject();
        json.put("classification", feature.getAge().getClassification());
        json.put("score", feature.getAge().getScore());
        return json;
    }

    private JSONObject genderToJson(SunmiFaceFeature feature) {
        if (feature == null || feature.getGender() == null) {
            return null;
        }
        JSONObject json = new JSONObject();
        json.put("classification", feature.getGender().getClassification());
        json.put("score", feature.getGender().getScore());
        return json;
    }

    private float[] transformPreviewPoint(float x, float y, int imageWidth, int imageHeight) {
        float px = x;
        float py = y;
        if (isPreviewMirrorX()) {
            px = imageWidth - px;
        }
        switch (normalizeRotation(appliedDisplayOrientationDeg)) {
            case 90:
                return new float[]{imageHeight - py, px};
            case 180:
                return new float[]{imageWidth - px, imageHeight - py};
            case 270:
                return new float[]{py, imageWidth - px};
            case 0:
            default:
                return new float[]{px, py};
        }
    }

    private boolean isSwapDimensions() {
        int rotation = normalizeRotation(appliedDisplayOrientationDeg);
        return rotation == 90 || rotation == 270;
    }

    private boolean isPreviewMirrorX() {
        return !"back".equalsIgnoreCase(cameraFacing);
    }

    private int normalizeRotation(int deg) {
        int normalized = deg % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    private int resolveAppliedDisplayOrientation(int cameraId) {
        if (displayOrientationDeg != 360) {
            return normalizeRotation(displayOrientationDeg);
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = 0;
        try {
            Display display = ((android.view.WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            switch (display.getRotation()) {
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
                case Surface.ROTATION_0:
                default:
                    degrees = 0;
                    break;
            }
        } catch (Exception ignored) {
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            int result = (info.orientation + degrees) % 360;
            return (360 - result) % 360;
        }
        return (info.orientation - degrees + 360) % 360;
    }

    private float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }

    private void updateFaceOverlay(JSONObject featureData) {
        JSONObject rect = featureData == null ? null : featureData.getJSONObject("faceRect");
        JSONObject landmark = featureData == null ? null : featureData.getJSONObject("landmark");
        mainHandler.post(() -> faceBoxOverlayView.updateFace(rect, landmark == null ? null : landmark.getJSONArray("points")));
    }

    private void clearFaceOverlay() {
        mainHandler.post(() -> faceBoxOverlayView.updateFace(null, null));
    }

    private JSONObject buildSimpleData(int code) {
        JSONObject data = new JSONObject();
        data.put("code", code);
        data.put("errorString", SunmiFaceSDK.getErrorString(code));
        return data;
    }

    private void openCameraIfPossible() {
        if (!running || !previewReady || destroyed || camera != null) {
            return;
        }
        try {
            int cameraId = resolveCameraId();
            camera = Camera.open(cameraId);
            camera.setPreviewTexture(textureView.getSurfaceTexture());
            appliedDisplayOrientationDeg = resolveAppliedDisplayOrientation(cameraId);
            camera.setDisplayOrientation(appliedDisplayOrientationDeg);
            Camera.Parameters parameters = camera.getParameters();
            applyBestCameraSize(parameters);
            camera.setParameters(parameters);
            camera.setPreviewCallback(this);
            camera.startPreview();
            emitStatus("preview_ready", detecting ? "正在检测，请将人脸对准镜头" : "预览已开启", null);
        } catch (Exception e) {
            emitError("camera_open_failed", "打开相机失败: " + safeMessage(e));
            stopPreviewAndRelease();
        }
    }

    private void stopPreviewAndRelease() {
        clearFaceOverlay();
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
            } catch (Exception ignored) {
            }
            try {
                camera.stopPreview();
            } catch (Exception ignored) {
            }
            try {
                camera.release();
            } catch (Exception ignored) {
            }
            camera = null;
        }
        synchronized (sdkLock) {
            if (sdkHandleReady) {
                try {
                    SunmiFaceSDK.releaseHandle();
                } catch (Exception ignored) {
                }
                sdkHandleReady = false;
                initializedDbPath = "";
            }
        }
    }

    private void restartIfRunning() {
        if (!running) {
            return;
        }
        stopPreviewAndRelease();
        openCameraIfPossible();
    }

    private int resolveCameraId() {
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        boolean front = !"back".equalsIgnoreCase(cameraFacing);
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (front && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
            if (!front && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return 0;
    }

    private void applyBestCameraSize(Camera.Parameters parameters) {
        Camera.Size previewSize = chooseBestSize(parameters.getSupportedPreviewSizes(), 16f / 9f, 1280);
        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
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

    private void emitStatus(String eventType, String message, JSONObject detailData) {
        emit("onStatus", eventType, message, detailData);
    }

    private void emitRecognize(String eventType, String message, JSONObject detailData) {
        emit("onRecognize", eventType, message, detailData);
    }

    private void emitError(String eventType, String message) {
        emit("onError", eventType, message, null);
    }

    private void emit(String eventName, String eventType, String message, JSONObject detailData) {
        mainHandler.post(() -> {
            if (destroyed) {
                return;
            }
            if (!TextUtils.isEmpty(message) && !message.equals(lastMessage)) {
                overlayView.setText(message);
                lastMessage = message;
            }
            if (listener == null) {
                return;
            }
            JSONObject event = new JSONObject();
            event.put("eventType", eventType);
            event.put("message", message);
            if (detailData != null) {
                event.putAll(detailData);
            }
            if ("onRecognize".equals(eventName)) {
                listener.onRecognize(event);
            } else if ("onError".equals(eventName)) {
                listener.onError(event);
            } else {
                listener.onStatus(event);
            }
        });
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        return throwable.getMessage() == null ? throwable.toString() : throwable.getMessage();
    }

    private static class FaceBoxOverlayView extends View {
        private final Paint rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private RectF faceRect;
        private java.util.List<PointF> landmarks;

        FaceBoxOverlayView(Context context) {
            super(context);
            rectPaint.setColor(0xFF27C46B);
            rectPaint.setStyle(Paint.Style.STROKE);
            rectPaint.setStrokeWidth(6f);
            pointPaint.setColor(0xFFFFD54F);
            pointPaint.setStyle(Paint.Style.FILL);
        }

        void updateFace(JSONObject rect, JSONArray points) {
            if (rect == null) {
                faceRect = null;
            } else {
                faceRect = new RectF(
                        rect.getFloatValue("x1"),
                        rect.getFloatValue("y1"),
                        rect.getFloatValue("x2"),
                        rect.getFloatValue("y2")
                );
            }
            if (points == null || points.isEmpty()) {
                landmarks = null;
            } else {
                java.util.ArrayList<PointF> list = new java.util.ArrayList<>();
                for (int i = 0; i < points.size(); i++) {
                    JSONObject point = points.getJSONObject(i);
                    list.add(new PointF(point.getFloatValue("x"), point.getFloatValue("y")));
                }
                landmarks = list;
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (faceRect != null) {
                RectF drawRect = new RectF(
                        faceRect.left * getWidth(),
                        faceRect.top * getHeight(),
                        faceRect.right * getWidth(),
                        faceRect.bottom * getHeight()
                );
                canvas.drawRect(drawRect, rectPaint);
            }
            if (landmarks != null) {
                for (PointF point : landmarks) {
                    canvas.drawCircle(point.x * getWidth(), point.y * getHeight(), 8f, pointPaint);
                }
            }
        }
    }
}
