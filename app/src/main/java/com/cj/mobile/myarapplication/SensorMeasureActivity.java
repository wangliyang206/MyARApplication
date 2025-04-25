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

import com.cj.mobile.myarapplication.ui.CrosshairView;

import java.util.List;
import java.util.Locale;

public class SensorMeasureActivity extends AppCompatActivity implements SensorEventListener {
    private static final int CAMERA_PERMISSION_CODE = 100;

    /*—————————————————————————————————————————————控件—————————————————————————————————————————————*/
    // 相机预览
    private SurfaceView cameraPreview;
    // 十字准星 和 测量线
    private CrosshairView crosshairView;
    // 显示距离
    private TextView distanceText;
    // 测量按钮
    private Button measureButton;

    /*———————————————————————————————————————————业务变量———————————————————————————————————————————*/
    // 照相机 对象
    private Camera camera;
    // 传感器管理器 对象
    private SensorManager sensorManager;
    // 存储经过低通滤波处理后的三轴加速度值（单位：m/s²）。
    // 对应手机X/Y/Z轴的加速度数据，用于计算实际运动加速度（去除重力分量）
    private float[] acceleration = new float[3];
    // 记录通过陀螺仪积分计算的三轴旋转角度（单位：弧度）。
    // 用于消除重力分量时计算手机姿态，对应绕X/Y/Z轴的旋转量。
    private float[] rotation = new float[3];
    // 记录上一次传感器事件的时间戳（纳秒级）。
    // 用于计算两次传感器数据采集的时间间隔（deltaTime），是加速度积分计算位移的关键时间参数。
    private long lastTimestamp;
    // 累计存储通过加速度二次积分计算出的总位移量（单位：米）。
    // 最终会显示在UI上作为测量结果。
    private double totalDistance;
    // 是否正在测量
    private boolean isMeasuring;

    // 优化后的参数配置
    private static final float ACCELERATION_THRESHOLD = 0.15f;      // 加速度阈值(m/s²)
    private static final float STATIONARY_DURATION = 1.0f;          // 静止判定时间(s)
    // 新增成员变量
    private float[] lastLinearAccel = new float[3];
    private long stationaryStartTime;
    private boolean isStationary;

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
        crosshairView = findViewById(R.id.crosshairView);
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
                // 使用改进的低通滤波（权重调整）
                final float alpha = 0.8f;
                acceleration[0] = alpha * acceleration[0] + (1 - alpha) * event.values[0];
                acceleration[1] = alpha * acceleration[1] + (1 - alpha) * event.values[1];
                acceleration[2] = alpha * acceleration[2] + (1 - alpha) * event.values[2];
                break;

            case Sensor.TYPE_GYROSCOPE:
                // 优化角度积分（增加漂移补偿）
                rotation[0] += (event.values[0] - 0.01f) * deltaTime; // 减去微小漂移
                rotation[1] += (event.values[1] - 0.01f) * deltaTime;
                rotation[2] += (event.values[2] - 0.01f) * deltaTime;
                break;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // 改进的重力分量计算
            float[] gravity = {
                    (float) (Math.sin(rotation[1]) * SensorManager.GRAVITY_EARTH),
                    (float) (-Math.sin(rotation[0]) * SensorManager.GRAVITY_EARTH),
                    (float) (Math.cos(rotation[0]) * Math.cos(rotation[1]) * SensorManager.GRAVITY_EARTH)
            };

            // 计算线性加速度（当前加速度 - 重力）
            float[] linearAccel = {
                    acceleration[0] - gravity[0],
                    acceleration[1] - gravity[1],
                    acceleration[2] - gravity[2]
            };

            // 运动状态检测
            float accelMagnitude = (float) Math.sqrt(
                    linearAccel[0] * linearAccel[0] +
                            linearAccel[1] * linearAccel[1] +
                            linearAccel[2] * linearAccel[2]
            );

            // 动态阈值检测（结合历史数据）
            float deltaAccel = Math.abs(accelMagnitude -
                    (float) Math.sqrt(
                            lastLinearAccel[0]*lastLinearAccel[0] +
                                    lastLinearAccel[1]*lastLinearAccel[1] +
                                    lastLinearAccel[2]*lastLinearAccel[2]
                    )
            );

            // 状态机判断
            if (accelMagnitude < ACCELERATION_THRESHOLD && deltaAccel < 0.1f) {
                if (!isStationary) {
                    stationaryStartTime = System.currentTimeMillis();
                    isStationary = true;
                } else if ((System.currentTimeMillis() - stationaryStartTime) / 1000f > STATIONARY_DURATION) {
                    return; // 持续静止超过阈值时跳过计算
                }
            } else {
                isStationary = false;
                System.arraycopy(linearAccel, 0, lastLinearAccel, 0, 3);
            }

            // 有效运动时积分计算
            if (!isStationary) {
                double deltaDistance = 0.5 * accelMagnitude * deltaTime * deltaTime;
                totalDistance += deltaDistance;

                // 速度衰减补偿（防止积分漂移）
                totalDistance *= 0.995;
            }
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