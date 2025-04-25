package com.cj.mobile.myarapplication.model;

/**
 * @ProjectName: MyARApplication
 * @Package: com.cj.mobile.myarapplication.model
 * @ClassName: Vector2
 * @Description:
 * @Author: WLY
 * @CreateDate: 2025/4/24 15:10
 */
public class Vector2 {
    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float x;
    public float y;

    // 新增距离计算方法
    public float distanceTo(Vector2 other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return (float) Math.sqrt(dx*dx + dy*dy);
    }

    public float length() {
        return (float) Math.sqrt(x*x + y*y);
    }

    public void normalize() {
        float len = length();
        if(len != 0) {
            x /= len;
            y /= len;
        }
    }

    public void set(float x, float y){
        this.x = x;
        this.y = y;
    }
}
