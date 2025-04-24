package com.cj.mobile.myarapplication.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
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
    private Paint linePaint;
    private Paint crosshairPaint;
    private PointF startPoint = new PointF();
    private PointF currentEndPoint = new PointF();
    private PointF targetEndPoint = new PointF();
    private boolean isMeasuring;
    private float lineSmoothness = 0.3f; // 0~1，值越大越平滑

    public CrosshairView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        // 测量线画笔配置
        linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

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
        // 初始化准星和测量线起点到屏幕中心
        startPoint.set(w / 2f, h / 2f);
        resetEndPoints();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCrosshair(canvas);
        if (isMeasuring) {
            updateLinePosition();
            drawMeasurementLine(canvas);
        }
    }

    private void drawCrosshair(Canvas canvas) {
        // 绘制准星十字线
        float crossSize = 40f; // 准星大小
        canvas.drawLine(
                startPoint.x - crossSize, startPoint.y,
                startPoint.x + crossSize, startPoint.y,
                crosshairPaint
        );
        canvas.drawLine(
                startPoint.x, startPoint.y - crossSize,
                startPoint.x, startPoint.y + crossSize,
                crosshairPaint
        );
    }

    private void drawMeasurementLine(Canvas canvas) {
        // 绘制测量线（带箭头）
        canvas.drawLine(
                startPoint.x, startPoint.y,
                currentEndPoint.x, currentEndPoint.y,
                linePaint
        );
        drawArrow(canvas, currentEndPoint);
    }

    private void drawArrow(Canvas canvas, PointF endPoint) {
        // 在测量线末端绘制箭头
        float arrowSize = 20f;
        float angle = (float) Math.atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x);

        // 绘制左侧箭头
        canvas.drawLine(
                endPoint.x,
                endPoint.y,
                (float) (endPoint.x - arrowSize * Math.cos(angle - Math.PI / 6)),
                (float) (endPoint.y - arrowSize * Math.sin(angle - Math.PI / 6)),
                linePaint
        );

        // 绘制右侧箭头
        canvas.drawLine(
                endPoint.x,
                endPoint.y,
                (float) (endPoint.x - arrowSize * Math.cos(angle + Math.PI / 6)),
                (float) (endPoint.y - arrowSize * Math.sin(angle + Math.PI / 6)),
                linePaint
        );
    }

    private void updateLinePosition() {
        // 平滑过渡到目标位置
        currentEndPoint.x += (targetEndPoint.x - currentEndPoint.x) * lineSmoothness;
        currentEndPoint.y += (targetEndPoint.y - currentEndPoint.y) * lineSmoothness;

        // 强制重绘实现动画效果
        if (Math.abs(targetEndPoint.x - currentEndPoint.x) > 0.1f ||
                Math.abs(targetEndPoint.y - currentEndPoint.y) > 0.1f) {
            postInvalidate();
        }
    }

    public void updateMeasurementLine(float offsetX, float offsetY) {
        // 更新目标终点位置（基于屏幕中心偏移）
        targetEndPoint.x = startPoint.x + offsetX;
        targetEndPoint.y = startPoint.y + offsetY;
        invalidate();
    }

    public void startMeasurement() {
        isMeasuring = true;
        resetEndPoints();
        postInvalidate();
    }

    public void stopMeasurement() {
        isMeasuring = false;
        resetEndPoints();
    }

    private void resetEndPoints() {
        targetEndPoint.set(startPoint.x, startPoint.y);
        currentEndPoint.set(startPoint.x, startPoint.y);
    }

    // 可选：动态调整平滑度（0-1）
    public void setLineSmoothness(float smoothness) {
        lineSmoothness = Math.max(0, Math.min(1, smoothness));
    }
}