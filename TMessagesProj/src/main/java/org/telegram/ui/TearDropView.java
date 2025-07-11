package org.telegram.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

class TearDropView extends FrameLayout {

    public TearDropView(Context context) {
        super(context);
        setWillNotDraw(false);

        init();
    }

    private int progress = 0;
    private final Paint splinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<PointF> controlPoints = new ArrayList<>();
    private final List<PointF> splinePoints = new ArrayList<>();
    private Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap circleBitmap;
    private float cx, cy, r;
    private float circleY;
    private float circleRadius = 100f;
    private Bitmap[] smallIcons = new Bitmap[6];
    private Bitmap blurredBitmap;
    private float lastBlurRadius = -1;
    private float lastScale = -1;

    private Paint bgPaint;

    private final Path clipPath = new Path();
    private final RectF dstRectF = new RectF();

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.BLACK);
    }

    double[] angles = {
            Math.toRadians(-70), Math.toRadians(-90), Math.toRadians(-110),
            Math.toRadians(70), Math.toRadians(90), Math.toRadians(110)
    };
    float[] radii = {1f, 1.3f, 1f, 1f, 1.3f, 1f};
    float[] startProgress = {0.45f, 0.3f, 0.55f, 0.6f, 0.25f, 0.4f};
    float[] endProgress   = {0.85f, 0.7f, 0.95f, 1.0f, 0.65f, 0.80f};

    private void drawSmallIcons(Canvas canvas) {
        if (smallIcons == null) return;

        float maxOffset = 300f;
        float globalProgress = Math.min(1f, progress / 100f);


        for (int i = 0; i < 6; i++) {
            float pStart = startProgress[i];
            float pEnd = endProgress[i];

            float localProgress;
            if (globalProgress <= pStart) {
                localProgress = 0f;
            } else if (globalProgress >= pEnd) {
                localProgress = 1f;
            } else {
                localProgress = (globalProgress - pStart) / (pEnd - pStart);
                localProgress = (float) (1 - Math.cos(localProgress * Math.PI)) / 2f; // ease in-out
            }

            float startX = cx;
            float startY = circleY;
            float endX = (float) (cx + Math.sin(angles[i]) * maxOffset * radii[i]);
            float endY = (float) (circleY + Math.cos(angles[i]) * maxOffset * radii[i]);
            float controlX = (startX + endX) / 2f;
            float controlY = (startY + endY) / 2f + 140;

            float t = localProgress;
            float oneMinusT = 1 - t;

            float iconX = oneMinusT * oneMinusT * startX + 2 * oneMinusT * t * controlX + t * t * endX;
            float iconY = oneMinusT * oneMinusT * startY + 2 * oneMinusT * t * controlY + t * t * endY;

            if (progress == 0) {
                iconX = cx;
                iconY = circleY;
            }

            Bitmap bmp = smallIcons[i];
            if (bmp == null) continue;

            float halfW = bmp.getWidth() / 2f;
            float halfH = bmp.getHeight() / 2f;

            canvas.save();
            Path circlePath = new Path();
            circlePath.addCircle(iconX, iconY, Math.min(halfW, halfH), Path.Direction.CW);
            canvas.clipPath(circlePath);

            canvas.drawBitmap(bmp, iconX - halfW, iconY - halfH, null);
            canvas.restore();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawSmallIcons(canvas);

        if (splinePoints.size() > 1) {
            Path backgroundPath = new Path();
            PointF first = splinePoints.get(0);
            backgroundPath.moveTo(first.x, first.y);

            for (int i = 1; i < splinePoints.size(); i++) {
                backgroundPath.lineTo(splinePoints.get(i).x, splinePoints.get(i).y);
            }
            for (int i = splinePoints.size() - 1; i >= 0; i--) {
                backgroundPath.lineTo(splinePoints.get(i).x, cy);
            }
            backgroundPath.close();

            canvas.drawPath(backgroundPath, bgPaint);
        }

        for (int i = 1; i < splinePoints.size(); i++) {
            PointF p1 = splinePoints.get(i - 1);
            PointF p2 = splinePoints.get(i);
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, splinePaint);
        }

        float blurRadius, scale;
        if (progress < 54) {
            if (progress <= 20) {
                blurRadius = 25f;
                scale = 0.1f;
            } else {
                float t = (progress - 20f) / (54f - 20f);
                float gamma = 3f;
                t = (float) Math.pow(t, gamma);
                blurRadius = 25f * (1f - t);
                scale = 0.1f + t * (1f - 0.1f);
            }
            Bitmap blurred = getBlurredBitmap(blurRadius, scale);
            int bitmapAlpha = (progress <= 20) ? 0 : ((progress >= 54) ? 255 : (int)(255 * (progress - 20f) / (54f - 20f)));
            bitmapPaint.setAlpha(bitmapAlpha);

            dstRectF.set(cx - circleRadius, circleY - circleRadius, cx + circleRadius, circleY + circleRadius);
            clipPath.reset();
            clipPath.addCircle(cx, circleY, circleRadius, Path.Direction.CW);

            canvas.save();
            canvas.clipPath(clipPath);
            canvas.drawBitmap(blurred, null, dstRectF, bitmapPaint);
            canvas.restore();

        } else {
            bitmapPaint.setAlpha(255);
            dstRectF.set(cx - circleRadius, circleY - circleRadius, cx + circleRadius, circleY + circleRadius);
            clipPath.reset();
            clipPath.addCircle(cx, circleY, circleRadius, Path.Direction.CW);

            canvas.save();
            canvas.clipPath(clipPath);
            canvas.drawBitmap(circleBitmap, null, dstRectF, bitmapPaint);
            canvas.restore();
        }
    }

    private Bitmap getBlurredBitmap(float blurRadius, float scale) {
        if (blurredBitmap != null && blurRadius == lastBlurRadius && scale == lastScale) {
            return blurredBitmap;
        }
        int scaledWidth = (int)(circleBitmap.getWidth() * scale);
        int scaledHeight = (int)(circleBitmap.getHeight() * scale);
        scaledWidth = Math.max(1, scaledWidth);
        scaledHeight = Math.max(1, scaledHeight);

        Bitmap smallBitmap = Bitmap.createScaledBitmap(circleBitmap, scaledWidth, scaledHeight, true);
        lastBlurRadius = blurRadius;
        lastScale = scale;
        return smallBitmap;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (blurredBitmap != null) {
            blurredBitmap.recycle();
            blurredBitmap = null;
        }
    }

    private void updateGeometry() {
        r = 350;
        splinePoints.clear();
        controlPoints.clear();

        float progressRatio = progress / 100f;
        circleY = cy - 50 + progressRatio * 335f;

        int totalPoints = 21;
        circleRadius = 30f + (progress / 100f) * 130;

        float startX = cx - r;
        float stepX = (2 * r) / (totalPoints - 1);

        List<PointF> basePoints = new ArrayList<>();

        // 1️⃣ Генерация базовых точек
        for (int i = 0; i < totalPoints; i++) {
            float x = startX + i * stepX;
            float y = cy;
            float dx = x - cx;

            if (progress >= 54) {
                if (i == totalPoints / 2) {
                    float t = Math.min((progress - 55f) / 5f, 1f); // от 0 до 1 при progress = 55..60
                    y = lerp(20f, 0f, t);
                }
            } else {

                if (Math.abs(dx) < circleRadius) {
                    if (progress < 30 || (i > 8 && i < totalPoints - 9)) {
                        float dy = (float) Math.sqrt(circleRadius * circleRadius - dx * dx);
                        float circleSurfaceY = circleY + dy;
                        if (circleSurfaceY > cy) {
                            y = circleSurfaceY;
                        }
                    }
                }

                if (progress > 35 && i <= 8) {
                    float shiftAmount = (progress - 35) * 3f;
                    x += shiftAmount;
                }

                if (progress > 35 && i >= totalPoints - 9) {
                    float shiftAmount = (progress - 35) * 3f;
                    x -= shiftAmount;
                }

                if (progress > 35 && (i == 8 || i == totalPoints - 9)) {
                    float yShift = (progress - 30) * 0.55f;
                    y += yShift;
                }
            }

            basePoints.add(new PointF(x, y));

        }

        if (progress <= 53) {

            List<PointF> refinedPoints = new ArrayList<>();

            for (int i = 0; i < basePoints.size() - 1; i++) {
                PointF p1 = basePoints.get(i);
                PointF p2 = basePoints.get(i + 1);

                refinedPoints.add(p1);

                float angle1 = (float) Math.atan2(p1.y - circleY, p1.x - cx);
                float angle2 = (float) Math.atan2(p2.y - circleY, p2.x - cx);

                float deltaAngle = angle2 - angle1;
                if (deltaAngle > Math.PI) deltaAngle -= 2 * Math.PI;
                if (deltaAngle < -Math.PI) deltaAngle += 2 * Math.PI;

                float arcLength = Math.abs(deltaAngle) * circleRadius;

                if (arcLength > stepX * 1.3) {
                    int insertCount = (int) (arcLength / stepX);

                    for (int j = 1; j <= insertCount; j++) {
                        float t = j / (float) (insertCount + 1);
                        float angle = angle1 + deltaAngle * t;

                        float x = cx + circleRadius * (float) Math.cos(angle);
                        float y = circleY + circleRadius * (float) Math.sin(angle);

                        refinedPoints.add(new PointF(x, y));
                    }
                }
            }

            refinedPoints.add(basePoints.get(basePoints.size() - 1));
            controlPoints.addAll(refinedPoints);
        } else {
            controlPoints.addAll(basePoints);
        }

        splinePoints.addAll(BSpline.computeBSpline(controlPoints, 20));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cx = w / 2f;
        cy = 0;

        updateGeometry();
    }

    public void setProgress(int progress) {
        this.progress = progress;
        updateGeometry();
        invalidate();
    }

    public void setCircleBitmap(Bitmap bitmap, Bitmap bitmapEmoji) {
        this.circleBitmap = bitmap;

        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        bitmapPaint.setShader(shader);

        int newWidth = 70;
        int newHeight = 70;
        Bitmap emoji = Bitmap.createScaledBitmap(bitmapEmoji, newWidth, newHeight, true);

        smallIcons[0] = emoji;
        smallIcons[1] = emoji;
        smallIcons[2] = emoji;
        smallIcons[3] = emoji;
        smallIcons[4] = emoji;
        smallIcons[5] = emoji;
    }

    private float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

}

class BSpline {

    public static List<PointF> computeBSpline(List<PointF> controlPoints, int resolution) {
        List<PointF> curve = new ArrayList<>();

        int n = controlPoints.size();
        if (n < 4) return curve;

        for (int i = 1; i < n - 2; i++) {
            for (int j = 0; j <= resolution; j++) {
                float t = (float) j / resolution;

                float b0 = ((1 - t) * (1 - t) * (1 - t)) / 6.0f;
                float b1 = (3 * t * t * t - 6 * t * t + 4) / 6.0f;
                float b2 = (-3 * t * t * t + 3 * t * t + 3 * t + 1) / 6.0f;
                float b3 = (t * t * t) / 6.0f;

                PointF p0 = controlPoints.get(i - 1);
                PointF p1 = controlPoints.get(i);
                PointF p2 = controlPoints.get(i + 1);
                PointF p3 = controlPoints.get(i + 2);

                float x = b0 * p0.x + b1 * p1.x + b2 * p2.x + b3 * p3.x;
                float y = b0 * p0.y + b1 * p1.y + b2 * p2.y + b3 * p3.y;

                curve.add(new PointF(x, y));
            }
        }

        return curve;
    }
}

