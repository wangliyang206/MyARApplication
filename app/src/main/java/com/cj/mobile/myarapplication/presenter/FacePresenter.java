package com.cj.mobile.myarapplication.presenter;

import android.graphics.Color;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.cj.mobile.myarapplication.R;
import com.cj.mobile.myarapplication.model.Ornament;

import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.RotateOnAxisAnimation;
import org.rajawali3d.animation.ScaleAnimation3D;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * @ProjectName: MyARApplication
 * @Package: com.cj.mobile.myarapplication.presenter
 * @ClassName: FacePresenter
 * @Description:
 * @Author: WLY
 * @CreateDate: 2025/4/18 16:29
 */
public class FacePresenter {
    public final static int NO_COLOR = 2333;


    public List<Ornament> getPresetOrnament() {
        List<Ornament> ornaments = new ArrayList<>();
        ornaments.add(getGlass());
        ornaments.add(getMoustache());
        ornaments.add(getHeart());
        ornaments.add(getCatEar());
        ornaments.add(getTigerNose());
        ornaments.add(getCatMask());
        ornaments.add(getPantherMask());
        ornaments.add(getVMask());
        ornaments.add(getDevilMask());
        ornaments.add(getGasMask());
        ornaments.add(getIronMan());
        ornaments.add(getRingHat());
        return ornaments;
    }

    private Ornament getGlass() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.glasses_obj);
        ornament.setImgResId(R.drawable.ic_glasses);
        ornament.setScale(0.005f);
        ornament.setOffset(0, 0, 0.2f);
        ornament.setRotate(-90.0f, 90.0f, 90.0f);
        ornament.setColor(Color.BLACK);
        return ornament;
    }

    private Ornament getMoustache() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.moustache_obj);
        ornament.setImgResId(R.drawable.ic_moustache);
        ornament.setScale(0.15f);
        ornament.setOffset(0, -0.25f, 0.2f);
        ornament.setRotate(-90.0f, 90.0f, 90.0f);
        ornament.setColor(Color.BLACK);
        return ornament;
    }

    private Ornament getCatEar() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.cat_ear_obj);
        ornament.setImgResId(R.drawable.ic_cat);
        ornament.setScale(11.0f);
        ornament.setOffset(0, 0.6f, -0.2f);
        ornament.setRotate(0.0f, 0.0f, 0.0f);
        ornament.setColor(0xffe06666);

        List<Animation3D> animation3Ds = new ArrayList<>();
        Animation3D anim = new RotateOnAxisAnimation(Vector3.Axis.X, -30);
        anim.setDurationMilliseconds(300);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setRepeatCount(2);
        animation3Ds.add(anim);
        ornament.setAnimation3Ds(animation3Ds);

        return ornament;
    }

    private Ornament getTigerNose() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.tiger_nose_obj);
        ornament.setImgResId(R.drawable.ic_tiger);
        ornament.setScale(0.002f);
        ornament.setOffset(0, -0.3f, 0.2f);
        ornament.setRotate(0.0f, 0.0f, 0.0f);
        ornament.setColor(0xffe06666);
        return ornament;
    }

    private Ornament getHeart() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.heart_eyes_obj);
        ornament.setImgResId(R.drawable.ic_heart);
        ornament.setScale(0.17f);
        ornament.setOffset(0, 0.0f, 0.1f);
        ornament.setRotate(0.0f, 0.0f, 0.0f);
        ornament.setColor(0xffcc0000);

        List<Animation3D> animation3Ds = new ArrayList<>();
        Animation3D anim = new ScaleAnimation3D(new Vector3(0.3f, 0.3f, 0.3f));
        anim.setDurationMilliseconds(300);
        anim.setInterpolator(new LinearInterpolator());
        animation3Ds.add(anim);
        ornament.setAnimation3Ds(animation3Ds);
        return ornament;
    }

    private Ornament getVMask() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.v_mask_obj);
        ornament.setImgResId(R.drawable.ic_v_mask);
        ornament.setScale(0.12f);
        ornament.setOffset(0, -0.1f, 0.0f);
        ornament.setRotate(0, 0, 0);
        ornament.setColor(Color.BLACK);
        return ornament;
    }

    private Ornament getCatMask() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.cat_mask_obj);
        ornament.setImgResId(R.drawable.ic_cat_mask);
        ornament.setScale(0.12f);
        ornament.setOffset(0, -0.1f, -0.1f);
        ornament.setRotate(0, 0, 0);
        ornament.setColor(Color.DKGRAY);
        return ornament;
    }

    private Ornament getPantherMask() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.panther_obj);
        ornament.setImgResId(R.drawable.ic_panther_mask);
        ornament.setScale(0.12f);
        ornament.setOffset(0, -0.1f, 0.0f);
        ornament.setRotate(0, 0, 0);
        ornament.setColor(NO_COLOR);
        return ornament;
    }

    private Ornament getDevilMask() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.devil_mask_obj);
        ornament.setImgResId(R.drawable.ic_devil_mask);
        ornament.setScale(0.13f);
        ornament.setOffset(0, -0.15f, 0.0f);
        ornament.setRotate(0, 0, 0);
        ornament.setColor(0xff660000);
        return ornament;
    }

    private Ornament getGasMask() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.gas_mask_obj);
        ornament.setImgResId(R.drawable.ic_gas_mask);
        ornament.setScale(0.11f);
        ornament.setOffset(0, -0.2f, 0.0f);
        ornament.setRotate(0, 0, 0);
        ornament.setColor(0xff333333);
        return ornament;
    }

    private Ornament getIronMan() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.iron_man_obj);
        ornament.setImgResId(R.drawable.ic_iron_man);
        ornament.setScale(0.11f);
        ornament.setOffset(0, -0.1f, 0.0f);
        ornament.setRotate(0, 0, 0);
        ornament.setColor(NO_COLOR);
        return ornament;
    }

    private Ornament getRingHat() {
        Ornament ornament = new Ornament();
        ornament.setModelResId(R.raw.ring_hat_obj);
        ornament.setImgResId(R.drawable.ic_ring_hat);
        ornament.setScale(0.12f);
        ornament.setOffset(0, 0.2f, 0.0f);
        ornament.setRotate(0, 0, 0);
        ornament.setColor(NO_COLOR);
        return ornament;
    }
}
