package com.cj.mobile.myarapplication.util;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.util.Log;
import android.view.MotionEvent;

import com.cj.mobile.myarapplication.R;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderAWD;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.Renderer;

import java.util.List;

/**
 * @ProjectName: MyARApplication
 * @Package: com.cj.mobile.myarapplication.util
 * @ClassName: AccelerometerRenderer
 * @Description:
 * @Author: WLY
 * @CreateDate: 2025/4/11 9:40
 */
public class AccelerometerRenderer extends Renderer {
    private DirectionalLight mLight;
    private Object3D mContainer;
    private Object3D mMonkey;
    private Object3D mGlasses;
    private Object3D mHairBand;
    private Object3D mMoustache;
    private Vector3 mAccValues;

    public AccelerometerRenderer(Context context) {
        super(context);
        mAccValues = new Vector3();
    }

    @Override
    protected void initScene() {
        try {
            mLight = new DirectionalLight(0.1f, -1.0f, -1.0f);
            mLight.setColor(1.0f, 1.0f, 1.0f);
            mLight.setPower(1);
            getCurrentScene().addLight(mLight);

            final LoaderAWD parser = new LoaderAWD(mContext.getResources(), mTextureManager, R.raw.head_object_new);
            parser.parse();
            mMonkey = parser.getParsedObject();
            mMonkey.setScale(0.005f);
            //mMonkey.setZ(-2f);
//                getCurrentScene().addChild(mMonkey);

//            int[] resourceIds = new int[]{R.drawable.posx, R.drawable.negx,
//                    R.drawable.posy, R.drawable.negy, R.drawable.posz,
//                    R.drawable.negz};

            Material material = new Material();
            material.enableLighting(true);
            material.setDiffuseMethod(new DiffuseMethod.Lambert());

//            CubeMapTexture envMap = new CubeMapTexture("environmentMap",
//                    resourceIds);
//            envMap.isEnvironmentTexture(true);
//            material.addTexture(envMap);
            material.setColorInfluence(0);
            mMonkey.setMaterial(material);

            LoaderOBJ objParser1 = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.glasses_obj);
            objParser1.parse();
            mGlasses = objParser1.getParsedObject();
            mGlasses.setScale(0.005f);
            //mGlasses.setZ(-0.2f);
            mGlasses.setZ(0.3f);
            mGlasses.rotate(Vector3.Axis.X, -90.0f);

            LoaderOBJ objParser2 = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.hair_band_obj);
            objParser2.parse();
            mHairBand = objParser2.getParsedObject();
            mHairBand.setScale(0.006f);
            mHairBand.setY(0.27f);
            mHairBand.setZ(-0.25f);
            mHairBand.rotate(Vector3.Axis.X, -90.0f);

            LoaderOBJ objParser3 = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.moustache_obj);
            objParser3.parse();
            mMoustache = objParser3.getParsedObject();
            mMoustache.setScale(0.007f);
            mMoustache.setY(-0.25f);
            mMoustache.setZ(0.3f);
            mMoustache.rotate(Vector3.Axis.X, -90.0f);

            mContainer = new Object3D();
            mContainer.addChild(mMonkey);
            mContainer.addChild(mGlasses);
            mContainer.addChild(mHairBand);
            mContainer.addChild(mMoustache);
            getCurrentScene().addChild(mContainer);
            //getCurrentCamera().setZ(20);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // -- set the background color to be transparent
        // you need to have called setGLBackgroundTransparent(true); in the activity
        // for this to work.
        getCurrentScene().setBackgroundColor(0);
    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);
        //mMonkey.setRotation(mAccValues.x, mAccValues.y, mAccValues.z);
        mContainer.setRotation(mAccValues.x, mAccValues.y, mAccValues.z);
    }

    public void updateFacePosition(List<PointF> facePoints) {
        if (mContainer == null) {
            Log.w("FaceEffect", "模型尚未初始化");
            return;
        }

        try {
            android.graphics.PointF point = facePoints.get(0);

            // 坐标转换（带异常捕获）
            float x = (point.x * 2 - 1) * 1.5f;
            float y = -(point.y * 2 - 1) * 1.5f;

//            mContainer.setPosition(x, y, -1);
            setAccelerometerValues(x, y, -1);

        } catch (IndexOutOfBoundsException e) {
            Log.e("FaceEffect", "无效的人脸坐标数据");
        }
    }

    public void setAccelerometerValues(float x, float y, float z) {
        mAccValues.setAll(x, y, z);
    }

    /**
     * 切换线框
     */
    void toggleWireframe() {
        mMonkey.setDrawingMode(mMonkey.getDrawingMode() == GLES20.GL_TRIANGLES ? GLES20.GL_LINES
                : GLES20.GL_TRIANGLES);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }
}
