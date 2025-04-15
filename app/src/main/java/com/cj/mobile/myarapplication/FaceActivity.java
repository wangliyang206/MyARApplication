package com.cj.mobile.myarapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cj.mobile.myarapplication.adapter.FilterAdapter;
import com.cj.mobile.myarapplication.adapter.OrnamentAdapter;
import com.cj.mobile.myarapplication.camera.MyCameraRenderer;
import com.cj.mobile.myarapplication.model.Ornament;
import com.cj.mobile.myarapplication.ui.AutoFitTextureView;
import com.cj.mobile.myarapplication.ui.CameraUtils;
import com.cj.mobile.myarapplication.ui.CustomBottomSheet;
import com.cj.mobile.myarapplication.util.AccelerometerRenderer;
import com.cj.mobile.myarapplication.util.OBJUtils;
import com.simoncherry.dlib.VisionDetRet;

import org.rajawali3d.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

/**
 * @ProjectName: MyARApplication
 * @Package: com.cj.mobile.myarapplication
 * @ClassName: FaceActivity
 * @Description:
 * @Author: WLY
 * @CreateDate: 2025/4/2 15:53
 */
public class FaceActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "FaceActivity";
    /*-------------------------------------视图-------------------------------------*/
    private AutoFitTextureView textureView;
    private SurfaceView renderSurface;
    private ImageView ivDraw;
    // 底部按钮
    private LinearLayout mLayoutBottomBtn;

    // 装饰
    private RecyclerView mRvOrnament;
    // 滤镜
    private RecyclerView mRvFilter;
    /*-------------------------------------对象-------------------------------------*/
    private AccelerometerRenderer mRenderer;                                                        // 渲染器

    private CustomBottomSheet mOrnamentSheet;                                                       // 装饰 - 底部弹窗
    private OrnamentAdapter mOrnamentAdapter;                                                       // 装饰 - 适配器
    private List<Ornament> mOrnaments = new ArrayList<>();                                          // 装饰 - 数据

    private CustomBottomSheet mFilterSheet;                                                         // 滤镜 - 底部弹窗
    private List<Integer> mFilters = new ArrayList<>();                                             // 滤镜 - 数据
    private FilterAdapter mFilterAdapter;                                                           // 滤镜 - 适配器

    private int mOrnamentId = -1;
    private boolean isBuildMask = false;
    // 相机渲染
    private MyCameraRenderer mCameraRenderer;
    private OnGetImageListener mOnGetPreviewListener = null;

    // 线程
    private Handler mUIHandler;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private HandlerThread inferenceThread;
    private Handler inferenceHandler;
    private ProgressDialog mDialog;

    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;

    // 是否绘制人脸关键点
    private boolean isDrawLandMark = true;
    private Paint mFaceLandmarkPaint;

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // 当屏幕关闭并重新打开时，SurfaceTexture已经可用，并且不会调用“onSurfaceTextureAvailable”。
        // 在这种情况下，我们可以打开一个相机并从这里开始预览（否则，我们会等到SurfaceTextureListener中的曲面准备就绪）。
        if (textureView.isAvailable()) {
            CameraUtils.openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            if (mOnGetPreviewListener == null) {
                initGetPreviewListener();
            }
            if (mCameraRenderer == null) {
                mCameraRenderer = new MyCameraRenderer(this);
            }
            textureView.setSurfaceTextureListener(mCameraRenderer);
        }
    }

    @Override
    public void onPause() {
        CameraUtils.closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRvOrnament.setAdapter(null);
        CameraUtils.releaseReferences();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);

        initView();
        initFacialKeyPoints();
        initRenderer();
        initOrnamentSheet();
        initFilterSheet();
        initCamera();
    }

    /**
     * 初始化视图
     */
    private void initView() {
        textureView = findViewById(R.id.camera_preview);
        renderSurface = findViewById(R.id.render_surface);
        ivDraw = (ImageView) findViewById(R.id.iv_facial_key_points);
        mLayoutBottomBtn = (LinearLayout) findViewById(R.id.layout_bottom_btn);

        // 关键设置1：设置SurfaceView透明模式
        renderSurface.setTransparent(true);
        // 关键设置2：确保渲染层在预览层之上
        renderSurface.setZOrderOnTop(true);
        // 关键设置3：禁用不透明缓冲
        renderSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        Button btnOrnament = (Button) findViewById(R.id.btn_ornament_sheet);
        Button btnFilterSheet = (Button) findViewById(R.id.btn_filter_sheet);

        btnOrnament.setOnClickListener(this);
        btnFilterSheet.setOnClickListener(this);
    }

    /**
     * 初始化人脸关键点
     */
    private void initFacialKeyPoints() {
        mUIHandler = new Handler(Looper.getMainLooper());
        mFaceLandmarkPaint = new Paint();
        mFaceLandmarkPaint.setColor(Color.YELLOW);
        mFaceLandmarkPaint.setStrokeWidth(2);
        mFaceLandmarkPaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * 初始化渲染器
     */
    private void initRenderer() {
        mRenderer = new AccelerometerRenderer(this);
        renderSurface.setSurfaceRenderer(mRenderer);
    }

    private void initOrnamentSheet() {
        mOrnamentAdapter = new OrnamentAdapter(getApplicationContext(), mOrnaments);
        mOrnamentAdapter.setOnItemClickListener(position -> {
            mOrnamentSheet.dismiss();
            mOrnamentId = position;
            isBuildMask = true;
        });

        View sheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_bottom_sheet, null);
        mRvOrnament = (RecyclerView) sheetView.findViewById(R.id.rv_gallery);
        mRvOrnament.setAdapter(mOrnamentAdapter);
        mRvOrnament.setLayoutManager(new GridLayoutManager(getApplicationContext(), 4));
        mOrnamentSheet = new CustomBottomSheet(getApplicationContext());
        mOrnamentSheet.setContentView(sheetView);
        mOrnamentSheet.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);
    }

    private void initFilterSheet() {
        for (int i=0; i<21; i++) {
            mFilters.add(i);
        }

        mFilterAdapter = new FilterAdapter(getApplicationContext(), mFilters);
        mFilterAdapter.setOnItemClickListener(position -> {
            String resName = "filter" + position;
            int resId = getResources().getIdentifier(resName, "string", getPackageName());
            mCameraRenderer.setSelectedFilter(resId);
        });

        View sheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_filter_sheet, null);
        mRvFilter = (RecyclerView) sheetView.findViewById(R.id.rv_filter);
        mRvFilter.setAdapter(mFilterAdapter);
        mRvFilter.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        mFilterSheet = new CustomBottomSheet(getApplicationContext());
        mFilterSheet.setContentView(sheetView);
        mFilterSheet.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet)
                .setBackgroundResource(android.R.color.transparent);
        mFilterSheet.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mFilterSheet.setOnDismissListener(dialog -> mLayoutBottomBtn.setVisibility(View.VISIBLE));
    }

    private void initCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        int orientation = getResources().getConfiguration().orientation;
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        CameraUtils.init(textureView, cameraManager, orientation, rotation);
    }

    /**
     * 初始化获取预览数据的监听器
     */
    private void initGetPreviewListener() {
        mOnGetPreviewListener = new OnGetImageListener();
        showDialog("提示", "正在初始化...");
        Thread mThread = new Thread() {
            @Override
            public void run() {
                mOnGetPreviewListener.initialize(getApplicationContext(),  inferenceHandler);
                dismissDialog();
            }
        };
        mThread.start();

        mOnGetPreviewListener.setLandMarkListener(new OnGetImageListener.LandMarkListener() {
            @Override
            public void onLandmarkChange(final List<VisionDetRet> results) {
                mUIHandler.post(() -> {
//                        handleMouthOpen(results);
                    if (!isDrawLandMark) {
                        ivDraw.setImageResource(0);
                    }
                });

                if (isDrawLandMark) {
                    inferenceHandler.post(() -> {
                        if (results != null && results.size() > 0) {
                            drawLandMark(results.get(0));
                        }
                    });
                }
            }

            @Override
            public void onRotateChange(float x, float y, float z) {
                rotateModel(x, y, z);
            }

            @Override
            public void onTransChange(float x, float y, float z) {
                // mRenderer.mContainer.setPosition(x/20, -y/20, z/20);
                mRenderer.getCurrentCamera().setPosition(-x/200, y/200, z/100);
            }

            @Override
            public void onMatrixChange(ArrayList<Double> elementList) {
            }
        });

        mOnGetPreviewListener.setBuildMaskListener((bitmap, landmarks) -> {
            Log.e("rotateList", "onGetSuitableFace");
            new Handler().post(() -> {
                OBJUtils.buildFaceModel(getApplicationContext(), bitmap, landmarks);
                isBuildMask = true;
            });
        });

        CameraUtils.setOnGetPreviewListener(mOnGetPreviewListener);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ornament_sheet:
                mOrnamentSheet.show();
                break;
            case R.id.btn_filter_sheet:
                mLayoutBottomBtn.setVisibility(View.GONE);
                mFilterSheet.show();
                break;
        }
    }

    /**
     * 启动后台线程
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        inferenceHandler = new Handler(inferenceThread.getLooper());

        CameraUtils.setBackgroundHandler(backgroundHandler);
    }

    /**
     * 停止后台线程
     */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        inferenceThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;

            inferenceThread.join();
            inferenceThread = null;
        } catch (final InterruptedException e) {
            Log.e(TAG, "error" ,e );
        }
    }

    /**
     * 显示对话框
     */
    private void showDialog(final String title, final String content) {
        mDialog = ProgressDialog.show(this, title, content, true);
    }

    /**
     * 隐藏对话框
     */
    private void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    /**
     * 旋转模型
     */
    private void rotateModel(float x, float y, float z) {
        if (mRenderer != null) {
            boolean isJumpX = false;
            boolean isJumpY = false;
            boolean isJumpZ = false;
            float rotateX = x;
            float rotateY = y;
            float rotateZ = z;

            if (Math.abs(lastX-x) > 90) {
                Log.e("rotateException", "X 跳变");
                isJumpX = true;
                rotateX = lastX;
            }
            if (Math.abs(lastY-y) > 90) {
                Log.e("rotateException", "Y 跳变");
                isJumpY = true;
                rotateY = lastY;
            }
            if (Math.abs(lastZ-z) > 90) {
                Log.e("rotateException", "Z 跳变");
                isJumpZ = true;
                rotateZ = lastZ;
            }

            mRenderer.setAccelerometerValues(rotateZ, rotateY, -rotateX);

            if (!isJumpX) lastX = x;
            if (!isJumpY) lastY = y;
            if (!isJumpZ) lastZ = z;
        }
    }

    /**
     * 绘制人脸标记
     */
    private void drawLandMark(VisionDetRet ret) {
        float resizeRatio = 1.0f;
        //float resizeRatio = 2.5f;    // 预览尺寸 480x320  /  截取尺寸 192x128  (另外悬浮窗尺寸是 810x540)
        Rect bounds = new Rect();
        bounds.left = (int) (ret.getLeft() * resizeRatio);
        bounds.top = (int) (ret.getTop() * resizeRatio);
        bounds.right = (int) (ret.getRight() * resizeRatio);
        bounds.bottom = (int) (ret.getBottom() * resizeRatio);

        Size previewSize = CameraUtils.getPreviewSize();
        if (previewSize != null) {
            final Bitmap mBitmap = Bitmap.createBitmap(previewSize.getHeight(), previewSize.getWidth(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mBitmap);
            canvas.drawRect(bounds, mFaceLandmarkPaint);

            final ArrayList<Point> landmarks = ret.getFaceLandmarks();
            for (Point point : landmarks) {
                int pointX = (int) (point.x * resizeRatio);
                int pointY = (int) (point.y * resizeRatio);
                canvas.drawCircle(pointX, pointY, 2, mFaceLandmarkPaint);
            }

            mUIHandler.post(() -> ivDraw.setImageBitmap(mBitmap));
        }
    }
}
