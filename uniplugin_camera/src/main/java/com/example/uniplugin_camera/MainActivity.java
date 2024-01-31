package com.example.uniplugin_camera;

import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    SurfaceView surfaceview1, surfaceview2,surfaceview3,surfaceview4;
    SurfaceHolder surfaceholder1, surfaceholder2, surfaceholder3, surfaceholder4;
    String TAG = "MainActivity";
    private Camera camera1 = null, camera2,camera3,camera4;
    Camera.Parameters parameters;
    private ReadThread mReadThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

//        surfaceview1 = (SurfaceView) findViewById(R.id.surfaceview1);
//        surfaceview2 = (SurfaceView) findViewById(R.id.surfaceview2);
//        surfaceview3 = (SurfaceView) findViewById(R.id.surfaceview3);
//        surfaceview4 = (SurfaceView) findViewById(R.id.surfaceview4);

        mReadThread = new ReadThread();
        mReadThread.start();
    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            Message msg = new Message();
            msg.what = 1;
            msg.obj = "传递的内容";
            handler.sendMessage(msg);

        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            surfaceholder1 = surfaceview1.getHolder();
            surfaceholder1.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceholder1.addCallback(new surfaceholderCallbackBack());

            surfaceholder2 = surfaceview2.getHolder();
            surfaceholder2.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceholder2.addCallback(new surfaceholderCallbackFont());

            surfaceholder3 = surfaceview3.getHolder();
            surfaceholder3.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceholder3.addCallback(new surfaceholderCallbackFont2());

            surfaceholder4 = surfaceview4.getHolder();
            surfaceholder4.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceholder4.addCallback(new surfaceholderCallbackFont3());
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    /**
     * 后置摄像头回调
     */
    class surfaceholderCallbackBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // 获取camera对象
            int cameraCount = Camera.getNumberOfCameras();
            if (cameraCount > 0) {
                if (cameraCount == 2) {
                    camera1 = Camera.open(0);
                }else if (cameraCount == 3) {
                    camera1 = Camera.open(1);
                }else
                    camera1 = Camera.open(0);
                try {
                    // 设置预览监听
                    camera1.setPreviewDisplay(holder);
                    Camera.Parameters parameters = camera1.getParameters();

                    if (MainActivity.this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                        parameters.set("orientation", "portrait");
                        camera1.setDisplayOrientation(0);
                        parameters.setRotation(0);
                    } else {
                        parameters.set("orientation", "landscape");
                        camera1.setDisplayOrientation(0);
                        parameters.setRotation(0);
                    }
                    camera1.setParameters(parameters);
                    // 启动摄像头预览
                    camera1.startPreview();
                    System.out.println("camera.startpreview");

                } catch (IOException e) {
                    e.printStackTrace();
                    camera1.release();
                    System.out.println("camera.release");
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            /*camera1.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        initCamera();// 实现相机的参数初始化
                        camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
                    }
                }
            });*/

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

        // 相机参数的初始化设置
        private void initCamera() {
            parameters = camera1.getParameters();
            parameters.setPictureFormat(PixelFormat.JPEG);
            //parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
            setDispaly(parameters, camera1);
            camera1.setParameters(parameters);
            camera1.startPreview();
            //camera1.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
        }

        // 控制图像的正确显示方向
        private void setDispaly(Camera.Parameters parameters, Camera camera) {
            if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
                setDisplayOrientation(camera, 0);
            } else {
                parameters.setRotation(0);
            }

        }

        // 实现的图像的正确显示
        private void setDisplayOrientation(Camera camera, int i) {
            Method downPolymorphic;
            try {
                downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
                if (downPolymorphic != null) {
                    downPolymorphic.invoke(camera, new Object[]{i});
                }
            } catch (Exception e) {
                Log.e("Came_e", "图像出错");
            }
        }
    }

    class surfaceholderCallbackFont implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // 获取camera对象
            int cameraCount = Camera.getNumberOfCameras();
            //if(true)
            //return;
            Log.e("HGT", "cameraCount:"+cameraCount);
            if (cameraCount == 2) {
                camera2 = Camera.open(1);
            }
            else if (cameraCount == 3) {
                camera2 = Camera.open(2);
            }else
                camera2 = Camera.open(1);
            try {
                // 设置预览监听
                camera2.setPreviewDisplay(holder);
                Camera.Parameters parameters = camera2.getParameters();

                if (MainActivity.this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    parameters.set("orientation", "portrait");
                    camera2.setDisplayOrientation(0);
                    parameters.setRotation(0);
                } else {
                    parameters.set("orientation", "landscape");
                    camera2.setDisplayOrientation(0);
                    parameters.setRotation(0);
                }
                camera2.setParameters(parameters);
                // 启动摄像头预览
                camera2.startPreview();
                System.out.println("camera.startpreview");

            } catch (IOException e) {
                e.printStackTrace();
                camera2.release();
                System.out.println("camera.release");
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            /*camera2.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        parameters = camera2.getParameters();
                        parameters.setPictureFormat(PixelFormat.JPEG);
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
                        setDispaly(parameters, camera2);
                        camera2.setParameters(parameters);
                        camera2.startPreview();
                        camera2.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
                        camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
                    }
                }
            });*/

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

        // 控制图像的正确显示方向
        private void setDispaly(Camera.Parameters parameters, Camera camera) {
            if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
                setDisplayOrientation(camera, 0);
            } else {
                parameters.setRotation(0);
            }

        }

        // 实现的图像的正确显示
        private void setDisplayOrientation(Camera camera, int i) {
            Method downPolymorphic;
            try {
                downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
                if (downPolymorphic != null) {
                    downPolymorphic.invoke(camera, new Object[]{i});
                }
            } catch (Exception e) {
                Log.e("Came_e", "图像出错");
            }
        }
    }

    class surfaceholderCallbackFont2 implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // 获取camera对象
            int cameraCount = Camera.getNumberOfCameras();
            //if(true)
            //return;
            Log.e("HGT", "cameraCount2:"+cameraCount);
            if (cameraCount == 4) {
                camera3 = Camera.open(2);

                try {
                    // 设置预览监听
                    camera3.setPreviewDisplay(holder);
                    Camera.Parameters parameters = camera3.getParameters();

                    if (MainActivity.this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                        parameters.set("orientation", "portrait");
                        camera3.setDisplayOrientation(0);
                        parameters.setRotation(0);
                    } else {
                        parameters.set("orientation", "landscape");
                        camera3.setDisplayOrientation(0);
                        parameters.setRotation(0);
                    }
                    camera3.setParameters(parameters);
                    // 启动摄像头预览
                    camera3.startPreview();
                    System.out.println("camera.startpreview");

                } catch (IOException e) {
                    e.printStackTrace();
                    camera3.release();
                    System.out.println("camera.release");
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            /*camera2.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        parameters = camera2.getParameters();
                        parameters.setPictureFormat(PixelFormat.JPEG);
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
                        setDispaly(parameters, camera2);
                        camera2.setParameters(parameters);
                        camera2.startPreview();
                        camera2.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
                        camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
                    }
                }
            });*/

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

        // 控制图像的正确显示方向
        private void setDispaly(Camera.Parameters parameters, Camera camera) {
            if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
                setDisplayOrientation(camera, 0);
            } else {
                parameters.setRotation(0);
            }

        }

        // 实现的图像的正确显示
        private void setDisplayOrientation(Camera camera, int i) {
            Method downPolymorphic;
            try {
                downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
                if (downPolymorphic != null) {
                    downPolymorphic.invoke(camera, new Object[]{i});
                }
            } catch (Exception e) {
                Log.e("Came_e", "图像出错");
            }
        }
    }

    class surfaceholderCallbackFont3 implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // 获取camera对象
            int cameraCount = Camera.getNumberOfCameras();
            //if(true)
            //return;
            Log.e("HGT", "cameraCount3:"+cameraCount);
            if (cameraCount == 4) {
                camera4 = Camera.open(3);

                try {
                    // 设置预览监听
                    camera4.setPreviewDisplay(holder);
                    Camera.Parameters parameters = camera4.getParameters();

                    if (MainActivity.this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                        parameters.set("orientation", "portrait");
                        camera4.setDisplayOrientation(0);
                        parameters.setRotation(0);
                    } else {
                        parameters.set("orientation", "landscape");
                        camera4.setDisplayOrientation(0);
                        parameters.setRotation(0);
                    }
                    camera4.setParameters(parameters);
                    // 启动摄像头预览
                    camera4.startPreview();
                    System.out.println("camera.startpreview");

                } catch (IOException e) {
                    e.printStackTrace();
                    camera4.release();
                    System.out.println("camera.release");
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            /*camera2.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        parameters = camera2.getParameters();
                        parameters.setPictureFormat(PixelFormat.JPEG);
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
                        setDispaly(parameters, camera2);
                        camera2.setParameters(parameters);
                        camera2.startPreview();
                        camera2.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
                        camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
                    }
                }
            });*/

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

        // 控制图像的正确显示方向
        private void setDispaly(Camera.Parameters parameters, Camera camera) {
            if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
                setDisplayOrientation(camera, 0);
            } else {
                parameters.setRotation(0);
            }

        }

        // 实现的图像的正确显示
        private void setDisplayOrientation(Camera camera, int i) {
            Method downPolymorphic;
            try {
                downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
                if (downPolymorphic != null) {
                    downPolymorphic.invoke(camera, new Object[]{i});
                }
            } catch (Exception e) {
                Log.e("Came_e", "图像出错");
            }
        }
    }
}


