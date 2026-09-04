package com.hhhdeveloper.swiftpdf.ui.cv.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.hhhdeveloper.swiftpdf.models.cv.CvModel;
import com.hhhdeveloper.swiftpdf.ui.cv.renderers.CvCanvasRenderer;

public class CvPreviewView extends View {

    private CvModel cvModel;
    private float zoomScale = 1.0f;

    public CvPreviewView(Context context) {
        super(context);
    }

    public CvPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CvPreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCvModel(CvModel cvModel) {
        this.cvModel = cvModel;
        invalidate(); // Trigger immediate repaint on keystroke / template switch
    }

    public void setZoomScale(float zoomScale) {
        this.zoomScale = Math.max(0.5f, Math.min(2.0f, zoomScale));
        requestLayout();
        invalidate();
    }

    public float getZoomScale() {
        return zoomScale;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) width = 600;
        // Maintain A4 aspect ratio (595 : 842 pt)
        int height = (int) (width * (CvCanvasRenderer.A4_HEIGHT_PT / CvCanvasRenderer.A4_WIDTH_PT) * zoomScale);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cvModel != null) {
            CvCanvasRenderer.render(canvas, cvModel, getWidth(), getHeight());
        }
    }
}
