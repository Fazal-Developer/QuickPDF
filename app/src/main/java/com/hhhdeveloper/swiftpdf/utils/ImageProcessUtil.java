package com.hhhdeveloper.swiftpdf.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

public class ImageProcessUtil {

    public static Bitmap rotate(Bitmap source, float degrees) {
        if (degrees == 0) return source;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap crop(Bitmap source, RectF normalizedRect) {
        float left = Math.max(0, Math.min(normalizedRect.left, 1f));
        float top = Math.max(0, Math.min(normalizedRect.top, 1f));
        float right = Math.max(0, Math.min(normalizedRect.right, 1f));
        float bottom = Math.max(0, Math.min(normalizedRect.bottom, 1f));

        if (left >= right || top >= bottom) return source;

        int x = (int) (left * source.getWidth());
        int y = (int) (top * source.getHeight());
        int width = (int) ((right - left) * source.getWidth());
        int height = (int) ((bottom - top) * source.getHeight());

        // Ensure bounds are non-zero and safe
        x = Math.max(0, Math.min(x, source.getWidth() - 1));
        y = Math.max(0, Math.min(y, source.getHeight() - 1));
        width = Math.max(1, Math.min(width, source.getWidth() - x));
        height = Math.max(1, Math.min(height, source.getHeight() - y));

        return Bitmap.createBitmap(source, x, y, width, height);
    }

    public static Bitmap toGrayscale(Bitmap bmpOriginal) {
        Bitmap bmpGrayscale = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public static Bitmap toDocScan(Bitmap bmpOriginal) {
        Bitmap bmpDoc = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpDoc);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);

        // Document high-contrast text threshold
        float scale = 3.2f;
        float translate = -280f;
        float[] matrix = {
            scale, 0, 0, 0, translate,
            0, scale, 0, 0, translate,
            0, 0, scale, 0, translate,
            0, 0, 0, 1, 0
        };
        cm.set(matrix);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpDoc;
    }

    public static Bitmap toEnhanced(Bitmap bmpOriginal) {
        Bitmap bmpEnhanced = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpEnhanced);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        
        // Enhance vibrancy & contrast
        float scale = 1.35f;
        float translate = -35f;
        float[] matrix = {
            scale, 0, 0, 0, translate,
            0, scale, 0, 0, translate,
            0, 0, scale, 0, translate,
            0, 0, 0, 1, 0
        };
        cm.set(matrix);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpEnhanced;
    }

    public static RectF getBitmapRectInImageView(int viewW, int viewH, int bitmapW, int bitmapH) {
        float bitmapRatio = (float) bitmapW / bitmapH;
        float viewRatio = (float) viewW / viewH;
        RectF rect = new RectF();
        if (bitmapRatio > viewRatio) {
            float height = viewW / bitmapRatio;
            float top = (viewH - height) / 2;
            rect.set(0, top, viewW, top + height);
        } else {
            float width = viewH * bitmapRatio;
            float left = (viewW - width) / 2;
            rect.set(left, 0, left + width, viewH);
        }
        return rect;
    }
}
