package se.helagro.colorcompare.colorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.flask.colorpicker.ColorCircle;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorChangedListener;
import com.flask.colorpicker.Utils;
import com.flask.colorpicker.builder.ColorWheelRendererBuilder;
import com.flask.colorpicker.builder.PaintBuilder;
import com.flask.colorpicker.renderer.ColorWheelRenderOption;
import com.flask.colorpicker.renderer.ColorWheelRenderer;
import com.hlag.colorcompare.R;

import java.util.ArrayList;

public class mColorPickerView extends View {
    static final String TAG = "mColorView";
    private Point centerPoint;
    private float radius;

    private static final float STROKE_RATIO = 2f;

    private Bitmap colorWheel;
    private Canvas colorWheelCanvas;
    private int density = 10;

    private float lightness = 1;
    private float alpha = 1;

    private Integer initialColor;
    private final Paint colorWheelFill = PaintBuilder.newPaint().color(0).build();
    private final Paint selectorStroke1 = PaintBuilder.newPaint().color(0xffffffff).build();
    private final Paint selectorStroke2 = PaintBuilder.newPaint().color(0xff000000).build();
    private final Paint alphaPatternPaint = PaintBuilder.newPaint().build();
    private ColorCircle currentColorCircle;

    private final ArrayList<OnColorChangedListener> colorChangedListeners = new ArrayList<>();

    private mLightnessSlider mLightnessSlider;
    private mAlphaSlider mAlphaSlider;


    private ColorWheelRenderer renderer;

    private int alphaSliderViewId, lightnessSliderViewId;

    public mColorPickerView(Context context) {
        super(context);
        initWith(context, null);
    }

    public mColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWith(context, attrs);
    }

    public mColorPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWith(context, attrs);
    }

    private void initWith(Context context, AttributeSet attrs) {
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ColorPickerPreference);

        density = typedArray.getInt(R.styleable.ColorPickerPreference_density, 10);
        initialColor = typedArray.getInt(R.styleable.ColorPickerPreference_initialColor, 0xffffffff);

        ColorPickerView.WHEEL_TYPE wheelType = ColorPickerView.WHEEL_TYPE.indexOf(typedArray.getInt(R.styleable.ColorPickerPreference_wheelType, 0));
        ColorWheelRenderer renderer = ColorWheelRendererBuilder.getRenderer(wheelType);

        alphaSliderViewId = typedArray.getResourceId(R.styleable.ColorPickerPreference_alphaSliderView, 0);
        lightnessSliderViewId = typedArray.getResourceId(R.styleable.ColorPickerPreference_lightnessSliderView, 0);

        setRenderer(renderer);
        setDensity(density);
        setInitialColor(initialColor);

        typedArray.recycle();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        updateColorWheel();
        currentColorCircle = findNearestByColor(initialColor);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (alphaSliderViewId != 0)
            setmAlphaSlider(getRootView().findViewById(alphaSliderViewId));
        if (lightnessSliderViewId != 0)
            setmLightnessSlider(getRootView().findViewById(lightnessSliderViewId));

        updateColorWheel();
        currentColorCircle = findNearestByColor(initialColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        colorWheel = null;
        updateColorWheel();
    }

    private void updateColorWheel() {
        int width = getMeasuredWidth();
        final int height = getMeasuredHeight();

        if (height < width)
            width = height;
        if (width <= 0)
            return;
        if (colorWheel == null) {
            colorWheel = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            colorWheelCanvas = new Canvas(colorWheel);
            alphaPatternPaint.setShader(PaintBuilder.createAlphaPatternShader(8));
        }
        drawColorWheel();
        invalidate();
    }

    private void drawColorWheel() {
        colorWheelCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

        if (renderer == null) return;

        float half = colorWheelCanvas.getWidth() / 2f;
        float strokeWidth = STROKE_RATIO * (1f + ColorWheelRenderer.GAP_PERCENTAGE);
        float maxRadius = half - strokeWidth - half / density;
        float cSize = maxRadius / (density - 1) / 2;

        final ColorWheelRenderOption colorWheelRenderOption = renderer.getRenderOption();
        colorWheelRenderOption.density = this.density;
        colorWheelRenderOption.maxRadius = maxRadius;
        colorWheelRenderOption.cSize = cSize;
        colorWheelRenderOption.strokeWidth = strokeWidth;
        colorWheelRenderOption.alpha = alpha;
        colorWheelRenderOption.lightness = lightness;
        colorWheelRenderOption.targetCanvas = colorWheelCanvas;

        renderer.initWith(colorWheelRenderOption);
        renderer.draw();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = 0;
        if (widthMode == MeasureSpec.UNSPECIFIED)
            width = widthMeasureSpec;
        else if (widthMode == MeasureSpec.AT_MOST)
            width = MeasureSpec.getSize(widthMeasureSpec);
        else if (widthMode == MeasureSpec.EXACTLY)
            width = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = 0;
        if (heightMode == MeasureSpec.UNSPECIFIED)
            height = widthMeasureSpec;
        else if (heightMode == MeasureSpec.AT_MOST)
            height = MeasureSpec.getSize(heightMeasureSpec);
        else if (widthMode == MeasureSpec.EXACTLY)
            height = MeasureSpec.getSize(heightMeasureSpec);
        int squareDimen = width;
        if (height < width)
            squareDimen = height;
        setMeasuredDimension(squareDimen, squareDimen);
    }


    private boolean isOutsideCircle(Point touchedPoint) {
        final int distance = (int) Math.round(Math.pow(touchedPoint.x - centerPoint.x, 2) + Math.pow(touchedPoint.y - centerPoint.y, 2));
        return distance > Math.pow(radius, 2);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(isOutsideCircle(new Point(Math.round(event.getX()), Math.round(event.getY())))) return false;
            case MotionEvent.ACTION_MOVE: {
                int lastSelectedColor = getSelectedColor();
                currentColorCircle = findNearestByPosition(event.getX(), event.getY());
                int selectedColor = getSelectedColor();

                callOnColorChangedListeners(lastSelectedColor, selectedColor);

                initialColor = selectedColor;
                setColorToSliders(selectedColor);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                int selectedColor = getSelectedColor();
                setColorToSliders(selectedColor);
                invalidate();
                break;
            }
        }
        return true;
    }

    protected void callOnColorChangedListeners(int oldColor, int newColor) {
        if (oldColor != newColor) {
            for (OnColorChangedListener listener : colorChangedListeners) {
                try {
                    listener.onColorChanged(newColor);
                } catch (Exception e) {
                    //Squash individual listener exceptions
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final int backgroundColor = 0x00000000;
        canvas.drawColor(backgroundColor);
        if (colorWheel != null)
            canvas.drawBitmap(colorWheel, 0, 0, null);
        if (currentColorCircle != null) {
            final float maxRadius = canvas.getWidth() / 2f - STROKE_RATIO * (1f + ColorWheelRenderer.GAP_PERCENTAGE);
            final float size = maxRadius / density / 2;
            colorWheelFill.setColor(android.graphics.Color.HSVToColor(currentColorCircle.getHsvWithLightness(this.lightness)));
            colorWheelFill.setAlpha((int) (alpha * 0xff));
            canvas.drawCircle(currentColorCircle.getX(), currentColorCircle.getY(), size * STROKE_RATIO, selectorStroke1);
            canvas.drawCircle(currentColorCircle.getX(), currentColorCircle.getY(), size * (1 + (STROKE_RATIO - 1) / 2), selectorStroke2);

            canvas.drawCircle(currentColorCircle.getX(), currentColorCircle.getY(), size, alphaPatternPaint);
            canvas.drawCircle(currentColorCircle.getX(), currentColorCircle.getY(), size, colorWheelFill);
        }

        if (centerPoint == null) {
            centerPoint = new Point(getWidth() / 2, getHeight() / 2);
            radius = (float) getWidth() / 2 + 4;
        }
    }

    private ColorCircle findNearestByPosition(float x, float y) {
        ColorCircle near = null;
        double minDist = Double.MAX_VALUE;

        for (ColorCircle colorCircle : renderer.getColorCircleList()) {
            final double dist = colorCircle.sqDist(x, y);
            if (minDist > dist) {
                minDist = dist;
                near = colorCircle;
            }
        }

        return near;
    }

    private ColorCircle findNearestByColor(int color) {
        final float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);
        ColorCircle near = null;
        double minDiff = Double.MAX_VALUE;
        final double x = hsv[1] * Math.cos(hsv[0] * Math.PI / 180);
        final double y = hsv[1] * Math.sin(hsv[0] * Math.PI / 180);

        for (ColorCircle colorCircle : renderer.getColorCircleList()) {
            float[] hsv1 = colorCircle.getHsv();
            final double x1 = hsv1[1] * Math.cos(hsv1[0] * Math.PI / 180);
            final double y1 = hsv1[1] * Math.sin(hsv1[0] * Math.PI / 180);
            final double dx = x - x1;
            final double dy = y - y1;
            final double dist = dx * dx + dy * dy;
            if (dist < minDiff) {
                minDiff = dist;
                near = colorCircle;
            }
        }

        return near;
    }

    public int getSelectedColor() {
        int color = 0;
        if (currentColorCircle != null)
            color = android.graphics.Color.HSVToColor(currentColorCircle.getHsvWithLightness(this.lightness));
        return Utils.adjustAlpha(this.alpha, color);
    }

    public void setInitialColor(int color) {
        final float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);

        this.alpha = Utils.getAlphaPercent(color);
        this.lightness = hsv[2];
        this.initialColor = color;
        setColorToSliders(color);
        currentColorCircle = findNearestByColor(color);
    }

    public void setLightness(float lightness) {
        final int lastSelectedColor = getSelectedColor();

        this.lightness = lightness;
        this.initialColor = android.graphics.Color.HSVToColor(Utils.alphaValueAsInt(this.alpha), currentColorCircle.getHsvWithLightness(lightness));
        if (this.mAlphaSlider != null)
            this.mAlphaSlider.setColor(this.initialColor);

        callOnColorChangedListeners(lastSelectedColor, this.initialColor);

        updateColorWheel();
        invalidate();
    }

    public void setColor(int color) {
        setInitialColor(color);
        updateColorWheel();
        invalidate();
    }

    public void setAlphaValue(float alpha) {
        final int lastSelectedColor = getSelectedColor();

        this.alpha = alpha;
        this.initialColor = android.graphics.Color.HSVToColor(Utils.alphaValueAsInt(this.alpha), currentColorCircle.getHsvWithLightness(this.lightness));
        if (this.mLightnessSlider != null)
            this.mLightnessSlider.setColor(this.initialColor);

        callOnColorChangedListeners(lastSelectedColor, this.initialColor);

        updateColorWheel();
        invalidate();
    }

    public void addOnColorChangedListener(OnColorChangedListener listener) {
        this.colorChangedListeners.add(listener);
    }

    public void setmLightnessSlider(mLightnessSlider mLightnessSlider) {

        this.mLightnessSlider = mLightnessSlider;
        if (mLightnessSlider != null) {
            this.mLightnessSlider.setColorPicker(this);
            this.mLightnessSlider.setColor(getSelectedColor());
        }
    }

    public void setmAlphaSlider(mAlphaSlider mAlphaSlider) {
        this.mAlphaSlider = mAlphaSlider;
        if (mAlphaSlider != null) {
            this.mAlphaSlider.setColorPicker(this);
            this.mAlphaSlider.setColor(getSelectedColor());
        }
    }

    public void setDensity(int density) {
        this.density = Math.max(2, density);
        invalidate();
    }

    public void setRenderer(ColorWheelRenderer renderer) {
        this.renderer = renderer;
        invalidate();
    }

    private void setColorToSliders(int selectedColor) {
        if (mLightnessSlider != null)
            mLightnessSlider.setColor(selectedColor);
        if (mAlphaSlider != null)
            mAlphaSlider.setColor(selectedColor);
    }

}
