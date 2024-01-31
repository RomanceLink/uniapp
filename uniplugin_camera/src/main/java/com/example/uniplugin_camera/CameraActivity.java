package com.example.uniplugin_camera;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.lang.reflect.Method;

public class CameraActivity{
    private static final String TAG = "CameraModule";
    private Camera colorCamera;
    private Camera infraredCamera;

    // Initialize both cameras
    public void initCameras(SurfaceHolder colorSurfaceHolder, SurfaceHolder infraredSurfaceHolder) {
        colorCamera = Camera.open(0);  // Adjust the camera index based on your device
        infraredCamera = Camera.open(1);  // Adjust the camera index based on your device

        initCamera(colorCamera, colorSurfaceHolder);
        initCamera(infraredCamera, infraredSurfaceHolder);
    }

    private void initCamera(Camera camera, SurfaceHolder surfaceHolder) {
        try {
            camera.setPreviewDisplay(surfaceHolder);
            Camera.Parameters parameters = camera.getParameters();
            setCameraDisplayOrientation(parameters);
            camera.setParameters(parameters);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            releaseCameras();
        }
    }

    // Set the display orientation for the camera
    private void setCameraDisplayOrientation(Camera.Parameters parameters) {
        if (Build.VERSION.SDK_INT >= 8) {
            colorCamera.setDisplayOrientation(0);
            infraredCamera.setDisplayOrientation(0);
        } else {
            parameters.setRotation(0);
        }
    }

    // Take a photo from both cameras
    public void takePhotos(Camera.PictureCallback colorCallback, Camera.PictureCallback infraredCallback) {
        if (colorCamera != null && infraredCamera != null) {
            colorCamera.takePicture(null, null, colorCallback);
            infraredCamera.takePicture(null, null, infraredCallback);
        }
    }

    // Release both cameras
    public void releaseCameras() {
        if (colorCamera != null) {
            colorCamera.stopPreview();
            colorCamera.release();
            colorCamera = null;
        }

        if (infraredCamera != null) {
            infraredCamera.stopPreview();
            infraredCamera.release();
            infraredCamera = null;
        }
    }
}
