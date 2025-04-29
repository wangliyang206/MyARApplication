package com.cj.mobile.myarapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/**
 * @ProjectName: MyARApplication
 * @Package: com.cj.mobile.myarapplication
 * @ClassName: ARMeasureActivity
 * @Description: 两点测量，手动选择起点和终点，并计算距离
 * @Author: WLY
 * @CreateDate: 2025/4/23 11:24
 */
public class ARMeasureActivity extends AppCompatActivity implements SensorEventListener {
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final int ASPECT_RATIO = AspectRatio.RATIO_4_3; // int类型（值=1）
    private float FOCAL_LENGTH = 3.97f; // 典型4800万像素主摄焦距
    private static final float SENSOR_WIDTH = 6.17f; // 1/2.0英寸传感器宽度（mm）
    private static final float SENSOR_HEIGHT = 4.54f; // 对应4:3比例

    private PreviewView previewView;
    private TextView distanceText, statusText;
    private Button measureButton;
    private ImageView startMarker, endMarker;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Point startPoint, endPoint;
    private boolean isMeasuring = false;
    private float[] orientation = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure);

        previewView = findViewById(R.id.previewView);
        distanceText = findViewById(R.id.distanceText);
        statusText = findViewById(R.id.statusText);
        measureButton = findViewById(R.id.measureButton);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        measureButton.setOnClickListener(v -> toggleMeasurement());

        // 初始化标记视图
        startMarker = findViewById(R.id.startMarker);
        endMarker = findViewById(R.id.endMarker);
        // 加载动画（淡入效果）
        startMarker.setAlpha(0f);
        endMarker.setAlpha(0f);

        requestPermissions();
    }

    private void requestPermissions() {
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(ASPECT_RATIO) // 1.2.3有效方法
                .build();

        // 关键兼容修改：CameraSelector在1.2.3使用addLensFacing
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);

        previewView.setOnTouchListener((v, event) -> {
            if (isMeasuring && event.getAction() == MotionEvent.ACTION_DOWN) {
                handleTap(event.getX(), event.getY());
                return true;
            }
            return false;
        });
    }

    private void handleTap(float rawX, float rawY) {
        if (!isDeviceLevel()) {
            statusText.setText("请保持手机水平！");
            return;
        }

        // 修复比例计算（基于实际预览尺寸）
        int previewWidth = previewView.getWidth();
        int previewHeight = previewView.getHeight();

        // 计算实际预览区域（考虑4:3缩放）
        float scale = Math.min(
                (float) previewWidth / ASPECT_RATIO,
                (float) previewHeight / 1
        );
        float displayWidth = scale * ASPECT_RATIO;
        float displayHeight = scale * 1;
        float left = (previewWidth - displayWidth) / 2;
        float top = (previewHeight - displayHeight) / 2;

        // 转换为传感器坐标系
        float sensorX = (rawX - left) / displayWidth * SENSOR_WIDTH;
        float sensorY = (rawY - top) / displayHeight * SENSOR_HEIGHT;

        if (startPoint == null) {
            startPoint = new Point(sensorX, sensorY);
            // 显示起点标记
            setMarkerPosition(startMarker, rawX, rawY);
            startMarker.setVisibility(View.VISIBLE);
            startMarker.animate().alpha(1f).setDuration(300).start();
            statusText.setText("选择终点...");
        } else {
            endPoint = new Point(sensorX, sensorY);
            // 显示终点标记
            setMarkerPosition(endMarker, rawX, rawY);
            endMarker.setVisibility(View.VISIBLE);
            endMarker.animate().alpha(1f).setDuration(300).start();
            calculateDistance();
        }
    }

    /**
     * 设置标记位置
     */
    private void setMarkerPosition(ImageView marker, float screenX, float screenY) {
        // 获取预览视图的实际显示区域
        int viewWidth = previewView.getWidth();
        int viewHeight = previewView.getHeight();

        // 计算居中显示的预览区域（考虑4:3比例）
        float scale = Math.min(
                (float) viewWidth / ASPECT_RATIO,
                (float) viewHeight / 1
        );
        float displayWidth = scale * ASPECT_RATIO;
        float displayHeight = scale * 1;

        // 计算有效点击区域
        float left = (viewWidth - displayWidth) / 2;
        float top = (viewHeight - displayHeight) / 2;
        float right = left + displayWidth;
        float bottom = top + displayHeight;

        // 坐标边界检查
        float adjustedX = Math.max(left, Math.min(screenX, right));
        float adjustedY = Math.max(top, Math.min(screenY, bottom));

        // 转换为相对布局坐标
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) marker.getLayoutParams();
        params.leftMargin = (int) (adjustedX - marker.getWidth() / 2);
        params.topMargin = (int) (adjustedY - marker.getHeight() / 2);

        // 限制在父容器范围内
        params.leftMargin = Math.max(0, Math.min(params.leftMargin, viewWidth - marker.getWidth()));
        params.topMargin = Math.max(0, Math.min(params.topMargin, viewHeight - marker.getHeight()));

        marker.setLayoutParams(params);
    }

    // 测量完成后清除标记
    private void clearMarkers() {
        startMarker.setVisibility(View.INVISIBLE);
        endMarker.setVisibility(View.INVISIBLE);
        startMarker.setAlpha(0f);
        endMarker.setAlpha(0f);
    }

    /**
     * 计算距离
     */
    private void calculateDistance() {
        if (startPoint == null || endPoint == null) return;

        // 实际物理尺寸转换（像素->毫米）
        float dx_mm = (endPoint.x - startPoint.x) * (SENSOR_WIDTH / previewView.getWidth());
        float dy_mm = (endPoint.y - startPoint.y) * (SENSOR_HEIGHT / previewView.getHeight());

        // 三维空间距离公式（单位：毫米）
        float distanceMM = (float) Math.sqrt(
                Math.pow(dx_mm, 2) +
                        Math.pow(dy_mm, 2) +
                        Math.pow(FOCAL_LENGTH, 2)
        );

        // 增加有效性检查
        if (Float.isNaN(distanceMM) || Float.isInfinite(distanceMM)) {
            showMessage("测量失败，请重试", "错误");
            return;
        }

        // 转换为厘米
        float distanceCM = distanceMM * 0.1f;

        runOnUiThread(() -> {
            showMessage(String.format("距离: %.2fcm", distanceCM), "测量完成！");
        });
    }

    private void showMessage(String msg, String tips) {
        runOnUiThread(() -> {
            distanceText.setText(tips);
            statusText.setText(msg);
        });
    }

    /**
     * 切换测量状态
     */
    private void toggleMeasurement() {
        isMeasuring = !isMeasuring;
        measureButton.setText(isMeasuring ? "结束测量" : "开始测量");
        clearMarkers(); // 新增清除逻辑
        startPoint = endPoint = null;
        distanceText.setText("等待测量...");
        statusText.setText("点击选择起点");
    }

    private boolean isDeviceLevel() {
//        return Math.abs(orientation[1]) < 10; // 俯仰角小于10度（水平判断）
        return Math.abs(orientation[1]) < 5; // 严格水平判断（±5度）
    }

    // 传感器回调
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] gravity = event.values;
            orientation[1] = (float) Math.toDegrees(Math.atan2(gravity[1], gravity[2]));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    static class Point {
        float x, y;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
