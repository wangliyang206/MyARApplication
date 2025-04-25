package com.cj.mobile.myarapplication.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * @ProjectName: MyARApplication
 * @Package: com.cj.mobile.myarapplication.ui
 * @ClassName: CrosshairView
 * @Description: 显示准星和测量线
 * @Author: WLY
 * @CreateDate: 2025/4/24 10:54
 */
public class CrosshairView extends View {
    private Paint crosshairPaint;

    public CrosshairView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        // 准星画笔配置
        crosshairPaint = new Paint();
        crosshairPaint.setColor(Color.WHITE);
        crosshairPaint.setStrokeWidth(4f);
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCrosshair(canvas);
    }

    private void drawCrosshair(Canvas canvas) {
        // 绘制准星十字线
        float crossSize = 40f;
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // 绘制水平线
        canvas.drawLine(
                centerX - crossSize, centerY,
                centerX + crossSize, centerY,
                crosshairPaint
        );
        // 绘制垂直线
        canvas.drawLine(
                centerX, centerY - crossSize,
                centerX, centerY + crossSize,
                crosshairPaint
        );
    }

}