package com.hhhdeveloper.swiftpdf.ui.imagetopdf;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class CropOverlayView extends View {

    private final Paint borderPaint = new Paint();
    private final Paint shadowPaint = new Paint();
    private final Paint handlePaint = new Paint();

    private final RectF cropRect = new RectF();
    private final float handleRadius = 36f;
    private final float touchThreshold = 70f;
    private int activeHandle = -1; // 0: Top-Left, 1: Top-Right, 2: Bottom-Right, 3: Bottom-Left, 4: Center drag

    private float lastX, lastY;

    public CropOverlayView(Context context) {
        super(context);
        init();
    }

    public CropOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        borderPaint.setColor(Color.parseColor("#FB8C00"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setAntiAlias(true);

        shadowPaint.setColor(Color.argb(140, 0, 0, 0));
        shadowPaint.setStyle(Paint.Style.FILL);

        handlePaint.setColor(Color.parseColor("#FB8C00"));
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Default crop box: centered, occupying 85% of viewport
        float paddingX = w * 0.08f;
        float paddingY = h * 0.08f;
        cropRect.set(paddingX, paddingY, w - paddingX, h - paddingY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // 1. Draw outer dim shadow
        // Top shadow
        canvas.drawRect(0, 0, w, cropRect.top, shadowPaint);
        // Left shadow
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, shadowPaint);
        // Right shadow
        canvas.drawRect(cropRect.right, cropRect.top, w, cropRect.bottom, shadowPaint);
        // Bottom shadow
        canvas.drawRect(0, cropRect.bottom, w, h, shadowPaint);

        // 2. Draw border
        canvas.drawRect(cropRect, borderPaint);

        // 3. Draw corner handles
        canvas.drawCircle(cropRect.left, cropRect.top, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.right, cropRect.top, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.right, cropRect.bottom, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.left, cropRect.bottom, handleRadius, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                activeHandle = getTouchedHandle(x, y);
                lastX = x;
                lastY = y;
                return activeHandle != -1;

            case MotionEvent.ACTION_MOVE:
                if (activeHandle != -1) {
                    float dx = x - lastX;
                    float dy = y - lastY;
                    moveHandle(activeHandle, dx, dy);
                    lastX = x;
                    lastY = y;
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeHandle = -1;
                break;
        }
        return super.onTouchEvent(event);
    }

    private int getTouchedHandle(float x, float y) {
        // 0: Top-Left
        if (Math.hypot(x - cropRect.left, y - cropRect.top) < touchThreshold) return 0;
        // 1: Top-Right
        if (Math.hypot(x - cropRect.right, y - cropRect.top) < touchThreshold) return 1;
        // 2: Bottom-Right
        if (Math.hypot(x - cropRect.right, y - cropRect.bottom) < touchThreshold) return 2;
        // 3: Bottom-Left
        if (Math.hypot(x - cropRect.left, y - cropRect.bottom) < touchThreshold) return 3;
        // 4: Center drag (inside the box)
        if (cropRect.contains(x, y)) return 4;

        return -1;
    }

    private void moveHandle(int handle, float dx, float dy) {
        float minSize = 120f;
        float w = getWidth();
        float h = getHeight();

        switch (handle) {
            case 0: // Top-Left
                cropRect.left = Math.max(0, Math.min(cropRect.left + dx, cropRect.right - minSize));
                cropRect.top = Math.max(0, Math.min(cropRect.top + dy, cropRect.bottom - minSize));
                break;
            case 1: // Top-Right
                cropRect.right = Math.min(w, Math.max(cropRect.right + dx, cropRect.left + minSize));
                cropRect.top = Math.max(0, Math.min(cropRect.top + dy, cropRect.bottom - minSize));
                break;
            case 2: // Bottom-Right
                cropRect.right = Math.min(w, Math.max(cropRect.right + dx, cropRect.left + minSize));
                cropRect.bottom = Math.min(h, Math.max(cropRect.bottom + dy, cropRect.top + minSize));
                break;
            case 3: // Bottom-Left
                cropRect.left = Math.max(0, Math.min(cropRect.left + dx, cropRect.right - minSize));
                cropRect.bottom = Math.min(h, Math.max(cropRect.bottom + dy, cropRect.top + minSize));
                break;
            case 4: // Center Drag (move whole rect)
                float rectW = cropRect.width();
                float rectH = cropRect.height();

                cropRect.left = Math.max(0, Math.min(cropRect.left + dx, w - rectW));
                cropRect.top = Math.max(0, Math.min(cropRect.top + dy, h - rectH));
                cropRect.right = cropRect.left + rectW;
                cropRect.bottom = cropRect.top + rectH;
                break;
        }
    }

    public RectF getNormalizedCropRect() {
        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return new RectF(0, 0, 1, 1);
        return new RectF(
                cropRect.left / w,
                cropRect.top / h,
                cropRect.right / w,
                cropRect.bottom / h
        );
    }

    public void setCropRect(RectF rect) {
        cropRect.set(rect);
        invalidate();
    }
}
