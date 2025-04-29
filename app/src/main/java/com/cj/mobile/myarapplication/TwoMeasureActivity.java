package com.cj.mobile.myarapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.SizeF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
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

@ExperimentalCamera2Interop
/**
 * @ProjectName: MyARApplication
 * @Package: com.cj.mobile.myarapplication
 * @ClassName: ARMeasureActivity
 * @Description: 两点测量，手动选择起点和终点，并计算距离
 * @Author: WLY
 * @CreateDate: 2025/4/23 11:24
 */
public class TwoMeasureActivity extends AppCompatActivity implements SensorEventListener {
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final int ASPECT_RATIO = AspectRatio.RATIO_4_3; // int类型（值=1）
    private float FOCAL_LENGTH = 3.97f; // 典型4800万像素主摄焦距
    private static float SENSOR_WIDTH = 6.17f; // 1/2.0英寸传感器宽度（mm）
    private static float SENSOR_HEIGHT = 4.54f; // 对应4:3比例

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

    /**
     * 绑定预览视图
     */
    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(ASPECT_RATIO) // 1.2.3有效方法
                .build();

        // 关键兼容修改：CameraSelector在1.2.3使用addLensFacing
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            // 获取相机参数
            Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);

            // 通过 Camera2 API 获取焦距
            String cameraId = Camera2CameraInfo.from(camera.getCameraInfo()).getCameraId();
            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

            SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            if (sensorSize != null) {
                SENSOR_WIDTH = sensorSize.getWidth();  // 实际传感器宽度(mm)
                SENSOR_HEIGHT = sensorSize.getHeight(); // 实际传感器高度(mm)
            }

            // 获取可用焦距列表（单位：毫米）
            float[] focalLengths = characteristics.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            );

            if (focalLengths != null && focalLengths.length > 0) {
                FOCAL_LENGTH = focalLengths[0]; // 使用第一个可用焦距
            }
        } catch (CameraAccessException e) {
//            e.printStackTrace();
            FOCAL_LENGTH = 3.97f; // 使用默认值
        }

        previewView.setOnTouchListener((v, event) -> {
            if (isMeasuring && event.getAction() == MotionEvent.ACTION_DOWN) {
                handleTap(event.getX(), event.getY());
                return true;
            }
            return false;
        });
    }

    private void handleTap(float rawX, float rawY) {
        // 添加预览视图有效性检查
        if (previewView.getWidth() <= 0 || previewView.getHeight() <= 0) {
            statusText.setText("相机初始化中...");
            return;
        }

        if (!isDeviceLevel()) {
            statusText.setText("请保持手机水平！");
            return;
        }

        // 获取带保护的预览尺寸
        int previewWidth = Math.max(1, previewView.getWidth());
        int previewHeight = Math.max(1, previewView.getHeight());

        // 计算显示区域（增加除零保护）
        float aspectRatioValue = ASPECT_RATIO == AspectRatio.RATIO_4_3 ? 4 / 3f : 1f;
        float scaleFactor = Math.min(
                previewWidth / Math.max(1f, aspectRatioValue),
                previewHeight / 1f
        );

        float displayWidth = scaleFactor * aspectRatioValue;
        float displayHeight = scaleFactor * 1f;

        // 边界计算（增加容错）
        float leftMargin = Math.max(0, (previewWidth - displayWidth) / 2);
        float topMargin = Math.max(0, (previewHeight - displayHeight) / 2);
        float rightBound = Math.min(previewWidth, leftMargin + displayWidth);
        float bottomBound = Math.min(previewHeight, topMargin + displayHeight);

        // 坐标约束
        float clampedX = Math.max(leftMargin, Math.min(rawX, rightBound));
        float clampedY = Math.max(topMargin, Math.min(rawY, bottomBound));

        // 传感器坐标转换（增加有效性验证）
        float validDisplayWidth = Math.max(1, displayWidth);
        float validDisplayHeight = Math.max(1, displayHeight);

        float sensorX = (clampedX - leftMargin) / validDisplayWidth * SENSOR_WIDTH;
        float sensorY = (clampedY - topMargin) / validDisplayHeight * SENSOR_HEIGHT;

        // NaN值防御
        if (Float.isNaN(sensorX) || Float.isNaN(sensorY)) {
            showMessage("坐标转换错误", "请重新测量");
            resetMeasurement();
            return;
        }

        // 防抖处理：避免点击相同位置
        if (startPoint != null) {
//            float minDistance = 5f; // 调整为5毫米
            float minDistance = SENSOR_WIDTH * 0.03f; // 传感器宽度的3%
            float distance = (float) Math.hypot(sensorX - startPoint.x, sensorY - startPoint.y);

            if (distance < minDistance) {
                statusText.setText("两点间距需大于" + minDistance + "mm");
                return;
            }
        }

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

    private void resetMeasurement() {
        startPoint = null;
        endPoint = null;
        clearMarkers();
        statusText.setText("点击重新开始测量");
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

        // 计算像素位移（毫米）
        float dx_mm = endPoint.x - startPoint.x;
        float dy_mm = endPoint.y - startPoint.y;

        // 添加安全校验
        if (FOCAL_LENGTH <= 0 || dx_mm == 0 && dy_mm == 0) {
            showMessage("设备参数异常", "测量失败");
            return;
        }

        // 使用透视投影公式（单位：毫米）
        // 真实距离 = 像素距离 * (物距 / 焦距)
        // 动态计算物距（假设用户保持30cm距离）
        final float OBJECT_DISTANCE = 300f; // 毫米
        float pixelDistance = (float) Math.sqrt(dx_mm * dx_mm + dy_mm * dy_mm);
        float realDistanceMM = pixelDistance * (OBJECT_DISTANCE / FOCAL_LENGTH);

        // 精度校验
        if (Float.isNaN(realDistanceMM) || realDistanceMM < 1) {
            showMessage("请重新选择测量点", "无效操作");
            return;
        }

        // 转换为厘米
        float distanceCM = realDistanceMM * 0.1f;

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

    /**
     * 设备水平判断
     */
    private boolean isDeviceLevel() {
        return Math.abs(orientation[1]) < 10; // 俯仰角小于10度（水平判断）
//        return Math.abs(orientation[1]) < 5; // 严格水平判断（±5度）
//        return true;          // 允许任意角度（用于测试）
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
