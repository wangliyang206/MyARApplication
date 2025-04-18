package com.cj.mobile.myarapplication.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.MotionEvent;

import com.cj.mobile.myarapplication.model.Ornament;
import com.cj.mobile.myarapplication.presenter.FacePresenter;

import org.rajawali3d.Object3D;
import org.rajawali3d.animation.Animation;
import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.AnimationGroup;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.Renderer;

import java.io.File;
import java.util.ArrayList;
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
    private static final String TAG = AccelerometerRenderer.class.getSimpleName();

    private DirectionalLight mLight;
    private Object3D mContainer;
    private Object3D mMonkey;
    private Object3D mOrnament;
    private Vector3 mAccValues;

    // 装饰模型，数据集合
    private List<Ornament> mOrnaments = new ArrayList<>();
    // 当前装饰模型id
    private int mOrnamentId = -1;
    // 是否绘制模型
    private boolean isBuildMask = false;

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

            mContainer = new Object3D();
            showMaskModel();
            getCurrentScene().addChild(mContainer);

        } catch (Exception e) {
            e.printStackTrace();
        }

        getCurrentScene().setBackgroundColor(0);
    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);
        if (isBuildMask) {
            showMaskModel();
            isBuildMask = false;
        }
        mContainer.setRotation(mAccValues.x, mAccValues.y, mAccValues.z);
    }

    public void setAccelerometerValues(float x, float y, float z) {
        mAccValues.setAll(x, y, z);
    }

    void showMaskModel() {
        try {
            boolean isFaceVisible = true;
            boolean isOrnamentVisible = true;
            if (mMonkey != null) {
                isFaceVisible = mMonkey.isVisible();
                mMonkey.setScale(1.0f);
                mMonkey.setPosition(0, 0, 0);
                mContainer.removeChild(mMonkey);
            }
            if (mOrnament != null) {
                isOrnamentVisible = mOrnament.isVisible();
                mOrnament.setScale(1.0f);
                mOrnament.setPosition(0, 0, 0);
                mContainer.removeChild(mOrnament);
            }

            String modelDir = OBJUtils.getModelDir();
            String imagePath = modelDir + OBJUtils.IMG_FACE;
            String objPath = OBJUtils.DIR_NAME + File.separator + FileUtils.getMD5(imagePath) + "_obj";
            LoaderOBJ parser = new LoaderOBJ(this, objPath);
            parser.parse();
            mMonkey = parser.getParsedObject();
            ATexture texture = mMonkey.getMaterial().getTextureList().get(0);
            mMonkey.getMaterial().removeTexture(texture);
            mMonkey.setScale(0.06f);
            mMonkey.setY(-0.54f);
            mMonkey.setZ(0.15f);
            mMonkey.setVisible(isFaceVisible);

            String texturePath = FileUtils.getMD5(imagePath) + ".jpg";
            Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFilePath(modelDir + texturePath, 1024, 1024);
            mMonkey.getMaterial().addTexture(new Texture("canvas", bitmap));
            mMonkey.getMaterial().enableLighting(false);

            mContainer.addChild(mMonkey);

            if (mOrnamentId >= 0 && mOrnaments.size() > mOrnamentId) {
                Ornament ornament = mOrnaments.get(mOrnamentId);
                LoaderOBJ objParser1 = new LoaderOBJ(mContext.getResources(), mTextureManager, ornament.getModelResId());
                objParser1.parse();
                mOrnament = objParser1.getParsedObject();
                mOrnament.setScale(ornament.getScale());
                mOrnament.setPosition(ornament.getOffsetX(), ornament.getOffsetY(), ornament.getOffsetZ());
                mOrnament.setRotation(ornament.getRotateX(), ornament.getRotateY(), ornament.getRotateZ());
                int color = ornament.getColor();
                if (color != FacePresenter.NO_COLOR) {
                    mOrnament.getMaterial().setColor(color);
                }
                mOrnament.setVisible(isOrnamentVisible);
                mContainer.addChild(mOrnament);

                getCurrentScene().clearAnimations();
                List<Animation3D> animation3Ds = ornament.getAnimation3Ds();
                if (animation3Ds != null && animation3Ds.size() > 0) {
                    final AnimationGroup animGroup = new AnimationGroup();
                    animGroup.setRepeatMode(Animation.RepeatMode.REVERSE_INFINITE);

                    for (Animation3D animation3D : animation3Ds) {
                        animation3D.setTransformable3D(mOrnament);
                        animGroup.addAnimation(animation3D);
                    }

                    getCurrentScene().registerAnimation(animGroup);
                    animGroup.play();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    public int getmOrnamentId() {
        return mOrnamentId;
    }

    public void setmOrnamentId(int mOrnamentId) {
        this.mOrnamentId = mOrnamentId;
    }

    public boolean isBuildMask() {
        return isBuildMask;
    }

    public void setBuildMask(boolean buildMask) {
        isBuildMask = buildMask;
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }
}
