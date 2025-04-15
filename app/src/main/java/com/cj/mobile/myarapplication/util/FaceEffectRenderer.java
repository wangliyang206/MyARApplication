package com.cj.mobile.myarapplication.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;
import android.view.MotionEvent;

import com.cj.mobile.myarapplication.R;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.renderer.Renderer;

import java.io.File;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @ProjectName: MyARApplication
 * @Package: com.cj.mobile.myarapplication.util
 * @ClassName: FaceEffectRenderer
 * @Description:
 * @Author: WLY
 * @CreateDate: 2025/4/3 17:40
 */
public class FaceEffectRenderer extends Renderer {
    private Object3D model;
    private long lastUpdateTime;
    private boolean isModelValid = false;

    public FaceEffectRenderer(Context context) {
        super(context);
    }

    @Override
    protected void initScene() {
        // 关键设置1：设置场景背景透明
        getCurrentScene().setBackgroundColor(0); // ARGB: 0x00000000

//        // 关键设置2：设置相机背景透明
//        getCurrentCamera().setBackgroundColor(0);
//
//        // 关键设置3：启用透明混合
//        getCurrentScene().setBlendFunc(
//                GLES20.GL_SRC_ALPHA,
//                GLES20.GL_ONE_MINUS_SRC_ALPHA
//        );

        try {
            // 灯光设置
            DirectionalLight mLight = new DirectionalLight(0.1f, -1.0f, -1.0f);
            mLight.setColor(1.0f, 1.0f, 1.0f);
            mLight.setPower(1);
            getCurrentScene().addLight(mLight);

            // ###########方式一，加载assets目录下的资源
//            File modelFile = CommUtil.copyAssetToCache(getContext(), "models/girl OBJ.obj");
//            File mtlFile = CommUtil.copyAssetToCache(getContext(), "models/girl OBJ.mtl");
//            // 拷贝图片文件到本地存储
//            CommUtil.copyAssetToCache(getContext(), "models/tEXTURE/BOdy Skin Base Color.png");
//            CommUtil.copyAssetToCache(getContext(), "models/tEXTURE/bot color.jpg");
//            CommUtil.copyAssetToCache(getContext(), "models/tEXTURE/COLORS.jpg");
//            CommUtil.copyAssetToCache(getContext(), "models/tEXTURE/FACE Base Color apha.png");
//            CommUtil.copyAssetToCache(getContext(), "models/tEXTURE/top color.png");
//            CommUtil.copyAssetToCache(getContext(), "models/tEXTURE/top normal.png");
//
//            LoaderOBJ loader = new LoaderOBJ(this, modelFile);


            // ###########方式二，加载raw目录下的资源
//            LoaderOBJ loader = new LoaderOBJ(this, R.raw.base_mask_obj);
//            LoaderOBJ loader = new LoaderOBJ(this, R.raw.monkey);
            LoaderOBJ loader = new LoaderOBJ(this, R.raw.glasses_obj);

            loader.parse();
            model = loader.getParsedObject();

            model.setScale(0.005f);
            //mGlasses.setZ(-0.2f);
            model.setZ(0.3f);
            model.rotate(Vector3.Axis.X, -90.0f);

            // 是否自带材质
            if (model.getMaterial() == null) {
                // 手动设置材质
                Material material = new Material();
//            material.setColor(0x66FF0000); // 半透明红色

                // 加载纹理材质
//                material.addTexture(new Texture("BOdy Skin Base Color", BitmapFactory.decodeStream(
//                        getContext().getAssets().open("models/tEXTURE/BOdy Skin Base Color.png")
//                )));
//                material.addTexture(new Texture("bot color", BitmapFactory.decodeStream(
//                        getContext().getAssets().open("models/tEXTURE/bot color.jpg")
//                )));
//                material.addTexture(new Texture("COLORS", BitmapFactory.decodeStream(
//                        getContext().getAssets().open("models/tEXTURE/COLORS.jpg")
//                )));
//                material.addTexture(new Texture("FACE Base Color apha", BitmapFactory.decodeStream(
//                        getContext().getAssets().open("models/tEXTURE/FACE Base Color apha.png")
//                )));
//                material.addTexture(new Texture("top color", BitmapFactory.decodeStream(
//                        getContext().getAssets().open("models/tEXTURE/top color.png")
//                )));
//                material.addTexture(new Texture("top normal", BitmapFactory.decodeStream(
//                        getContext().getAssets().open("models/tEXTURE/top normal.png")
//                )));


                model.setMaterial(material);
//            model.setScale(0.08f);
//            model.setPosition(0, 0, -1); // 初始位置
            }
            getCurrentScene().addChild(model);

            isModelValid = true;
            Log.i("Model", "3D模型加载成功");
        } catch (Exception e) {
            Log.e("Model", "模型加载失败: " + e.getMessage());

            // 创建备用立方体
            model = new Cube(0.5f);
            Material fallbackMat = new Material();
            fallbackMat.setColor(0xFF00FF00);
            model.setMaterial(fallbackMat);
            getCurrentScene().addChild(model);

            isModelValid = false;
        }
    }

    public boolean isModelLoaded() {
        return model != null;
    }

    public void updateFacePosition(List<android.graphics.PointF> facePoints) {
        if (model == null) {
            Log.w("FaceEffect", "模型尚未初始化");
            return;
        }

        if (!isModelValid) {
            model.setVisible(false);
            return;
        }

        if (facePoints == null || facePoints.isEmpty()) {
            model.setVisible(false);
            return;
        }

        // 限制更新频率（最低30FPS）
        if (System.currentTimeMillis() - lastUpdateTime < 33) {
            return;
        }

        try {
            android.graphics.PointF point = facePoints.get(0);

            // 坐标转换（带异常捕获）
            float x = (point.x * 2 - 1) * 1.5f;
            float y = -(point.y * 2 - 1) * 1.5f;

            model.setPosition(x, y, -1);
            model.setVisible(true);

            startRendering();
            lastUpdateTime = System.currentTimeMillis();

        } catch (IndexOutOfBoundsException e) {
            Log.e("FaceEffect", "无效的人脸坐标数据");
        }
    }

    @Override
    public void onRenderSurfaceCreated(EGLConfig config, GL10 gl, int width, int height) {
        super.onRenderSurfaceCreated(config, gl, width, height);
        // 关键设置4：启用OpenGL透明通道
        gl.glEnable(GLES20.GL_BLEND);
        gl.glClearColor(0, 0, 0, 0); // 透明清屏
    }

    @Override
    protected void onRender(long elapsedTime, double deltaTime) {
        super.onRender(elapsedTime, deltaTime);
        // 禁用自动旋转等额外操作
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset,
                                 float xOffsetStep, float yOffsetStep,
                                 int xPixelOffset, int yPixelOffset) {
        // 禁用壁纸效果相关回调
    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }
}