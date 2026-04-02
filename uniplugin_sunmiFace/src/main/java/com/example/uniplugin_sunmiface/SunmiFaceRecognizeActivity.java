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
    // 预览显示方向（只影响预览，不自动修正图片像素方向）
    private int displayOrientationDeg = 90;
    // 保存图片时是否旋转/镜像到“前端期望”的方向
    private int captureImageRotationDeg = 0;
    private boolean captureMirrorX = false;
    private boolean pendingAutoCapture = false;
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
        preferFrontCamera = getIntent().getBooleanExtra("preferFrontCamera", true);
        autoCaptureDelayMs = Math.max(300, getIntent().getIntExtra("autoCaptureDelayMs", 800));
        displayOrientationDeg = getIntent().getIntExtra("displayOrientationDeg", 90);
        captureImageRotationDeg = getIntent().getIntExtra("captureImageRotationDeg", 0);
        captureMirrorX = getIntent().getBooleanExtra("captureMirrorX", false);
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

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.VERTICAL);
        bottomBar.setPadding(32, 32, 32, 48);
        bottomBar.setBackgroundColor(0x66000000);

        statusView = new TextView(this);
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(16);
        statusView.setText("请将人脸对准镜头，检测到人脸后会自动识别");
        bottomBar.addView(statusView);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_HORIZONTAL);
        actions.setPadding(0, 24, 0, 0);

        Button cancelButton = new Button(this);
        cancelButton.setText("取消");
        cancelButton.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
        actions.addView(cancelButton);

        Button switchButton = new Button(this);
        switchButton.setText("切换摄像头");
        switchButton.setOnClickListener(v -> switchCamera());
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        switchParams.leftMargin = 32;
        actions.addView(switchButton, switchParams);

        bottomBar.addView(actions);

        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bottomParams.gravity = Gravity.BOTTOM;
        rootView.addView(bottomBar, bottomParams);
        return rootView;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = Camera.open(resolveCameraId());
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(displayOrientationDeg);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPictureFormat(android.graphics.PixelFormat.JPEG);
            applyBestCameraSize(parameters);
            camera.setParameters(parameters);
            updatePreviewLayout(parameters);
            camera.setFaceDetectionListener(this);
            camera.startPreview();
            startFaceDetectionIfSupported();
        } catch (Exception e) {
            statusView.setText("打开相机失败: " + e.getMessage());
            releaseCamera();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        cancelAutoCapture();
        releaseCamera();
    }

    private void capture() {
        if (camera == null || isCapturing) {
            return;
        }
        isCapturing = true;
        statusView.setText("正在拍照识别...");
        try {
            camera.takePicture(null, null, this);
        } catch (Exception e) {
            isCapturing = false;
            statusView.setText("拍照失败: " + e.getMessage());
        }
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        if (faces == null || faces.length == 0) {
            if (!isCapturing) {
                statusView.setText("请将人脸对准镜头，系统会自动识别");
            }
            cancelAutoCapture();
            return;
        }
        if (isCapturing) {
            return;
        }
        statusView.setText("检测到人脸，正在自动识别...");
        if (!pendingAutoCapture) {
            pendingAutoCapture = true;
            mainHandler.postDelayed(autoCaptureRunnable, autoCaptureDelayMs);
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        try {
            File imageFile = new File(getCacheDir(), "sunmi-face-capture-" + System.currentTimeMillis() + ".jpg");
            // 如果需要对齐前端坐标系/预览方向，就在保存阶段把图片做一次旋转/镜像
            if (captureImageRotationDeg != 0 || captureMirrorX) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (bitmap != null) {
                    Matrix matrix = new Matrix();
                    if (captureImageRotationDeg != 0) {
                        matrix.postRotate(captureImageRotationDeg);
                    }
                    if (captureMirrorX) {
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
            finish();
        } catch (IOException e) {
            isCapturing = false;
            pendingAutoCapture = false;
            statusView.setText("保存照片失败: " + e.getMessage());
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
        cancelAutoCapture();
        releaseCamera();
    }

    private void startFaceDetectionIfSupported() {
        if (camera == null) {
            return;
        }
        try {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getMaxNumDetectedFaces() > 0) {
                camera.startFaceDetection();
            } else {
                statusView.setText("当前设备不支持实时人脸检测，请保持正对镜头");
                if (!pendingAutoCapture && !isCapturing) {
                    pendingAutoCapture = true;
                    mainHandler.postDelayed(autoCaptureRunnable, 1500);
                }
            }
        } catch (Exception e) {
            statusView.setText("自动检测不可用，将尝试直接拍照识别");
            if (!pendingAutoCapture && !isCapturing) {
                pendingAutoCapture = true;
                mainHandler.postDelayed(autoCaptureRunnable, 1500);
            }
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

    private void cancelAutoCapture() {
        pendingAutoCapture = false;
        mainHandler.removeCallbacks(autoCaptureRunnable);
    }

    private void switchCamera() {
        preferFrontCamera = !preferFrontCamera;
        isCapturing = false;
        cancelAutoCapture();
        releaseCamera();
        try {
            if (surfaceHolder != null) {
                surfaceCreated(surfaceHolder);
            }
        } catch (Exception e) {
            statusView.setText("切换摄像头失败: " + e.getMessage());
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.setFaceDetectionListener(null);
            } catch (Exception ignored) {
            }
            try {
                camera.stopFaceDetection();
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
}
