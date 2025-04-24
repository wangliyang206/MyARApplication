package com.cj.mobile.myarapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

public class SensorMeasureActivity extends AppCompatActivity implements SensorEventListener {
    private static final int CAMERA_PERMISSION_CODE = 100;

    private Camera camera;
    private SurfaceView cameraPreview;
    private TextView distanceText;
    private Button measureButton;

    private SensorManager sensorManager;
    private float[] acceleration = new float[3];
    private float[] rotation = new float[3];
    private long lastTimestamp;
    private double totalDistance;
    private boolean isMeasuring;

    private final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d("Camera", "创建曲面");
            new Handler().postDelayed(() -> initCamera(), 300);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("Camera", "表面已更改: " + width + "x" + height);
            if (camera != null) {
                try {
                    camera.stopPreview();
                    Camera.Parameters params = camera.getParameters();
                    params.setPreviewSize(width, height);
                    camera.setParameters(params);
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    Log.e("Camera", "表面变化错误", e);
                }
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("Camera", "表面损坏");
            releaseCamera();
        }
    };

    private final Camera.ErrorCallback errorCallback = (error, cam) -> {
        String errorMsg;
        switch (error) {
            case Camera.CAMERA_ERROR_SERVER_DIED:
                errorMsg = "相机服务终止";
                break;
            case Camera.CAMERA_ERROR_EVICTED:
                errorMsg = "摄像头资源已占用";
                break;
            default:
                errorMsg = "未知摄像头错误: " + error;
        }
        runOnUiThread(() -> {
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            releaseCamera();
        });
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_measure);

        // 初始化视图
        cameraPreview = findViewById(R.id.cameraPreview);
        distanceText = findViewById(R.id.distanceText);
        measureButton = findViewById(R.id.measureButton);

        // 绑定Surface回调
        cameraPreview.getHolder().addCallback(surfaceCallback);

        // 初始化传感器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // 测量按钮监听
        measureButton.setOnClickListener(v -> {
            if (!isMeasuring) {
                startMeasurement();
            } else {
                stopMeasurement();
            }
        });

        // 检查权限
        checkCameraPermission();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            initCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera();
            } else {
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initCamera() {
        try {
            releaseCamera();

            int cameraId = findBackFacingCamera();
            if (cameraId < 0) {
                Toast.makeText(this, "未找到后置摄像头", Toast.LENGTH_SHORT).show();
                return;
            }

            camera = Camera.open(cameraId);
            camera.setErrorCallback(errorCallback);

            Camera.Parameters params = camera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            Camera.Size optimalSize = getOptimalPreviewSize(
                    params.getSupportedPreviewSizes(),
                    cameraPreview.getWidth(),
                    cameraPreview.getHeight()
            );
            if (optimalSize != null) {
                params.setPreviewSize(optimalSize.width, optimalSize.height);
            }

            camera.setParameters(params);
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(cameraPreview.getHolder());
            camera.startPreview();
        } catch (Exception e) {
            Log.e("Camera", "Init failed: " + e.getMessage());
            Toast.makeText(this, "摄像头初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;

            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return optimalSize;
    }

    private void startMeasurement() {
        isMeasuring = true;
        totalDistance = 0;
        lastTimestamp = System.nanoTime();
        runOnUiThread(() -> {
            measureButton.setText("停止");
            distanceText.setText("请缓慢移动...");
        });

        // 注册传感器
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
    }

    private void stopMeasurement() {
        isMeasuring = false;
        runOnUiThread(() -> {
            measureButton.setText("开始");
            distanceText.setText(String.format(Locale.US, "距离: %.2f m", totalDistance));
        });
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // 未在测量状态时直接返回
        if (!isMeasuring) return;

        long currentTime = System.nanoTime();
        // 计算时间差（单位：秒），用于积分计算
        float deltaTime = (currentTime - lastTimestamp) / 1e9f;
        lastTimestamp = currentTime;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // 低通滤波处理（权重 0.1），用于消除高频噪声
                acceleration[0] = event.values[0] * 0.1f + acceleration[0] * 0.9f;
                acceleration[1] = event.values[1] * 0.1f + acceleration[1] * 0.9f;
                acceleration[2] = event.values[2] * 0.1f + acceleration[2] * 0.9f;
                break;

            case Sensor.TYPE_GYROSCOPE:
                // 积分陀螺仪数据计算旋转角度（弧度）
                // deltaTime 时间间隔内角速度的积分
                rotation[0] += event.values[0] * deltaTime;
                rotation[1] += event.values[1] * deltaTime;
                rotation[2] += event.values[2] * deltaTime;
                break;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // 去除重力分量计算线性加速度
            float[] linearAccel = new float[]{
                    acceleration[0] - (float) Math.sin(rotation[1]) * SensorManager.GRAVITY_EARTH,
                    acceleration[1] - (float) Math.sin(rotation[0]) * SensorManager.GRAVITY_EARTH,
                    acceleration[2] - SensorManager.GRAVITY_EARTH
            };

            // 通过加速度二次积分计算位移（s = 0.5 * a * t²）
            double deltaDistance = 0.5 * Math.sqrt(
                    linearAccel[0] * linearAccel[0] +
                            linearAccel[1] * linearAccel[1] +
                            linearAccel[2] * linearAccel[2]
            ) * deltaTime * deltaTime;

            // 累加总位移
            totalDistance += deltaDistance;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera == null) {
            initCamera();
        }
    }
}