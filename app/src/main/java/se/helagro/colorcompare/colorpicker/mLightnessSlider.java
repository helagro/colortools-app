package se.helagro.colorcompare.colorpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.flask.colorpicker.Utils;
import com.flask.colorpicker.builder.PaintBuilder;
import se.helagro.colorcompare.MyApp;

public class mLightnessSlider extends mAbsCustomSlider {

    private int color;
    private final Paint solid = PaintBuilder.newPaint().build();
    private final Paint barPaint = PaintBuilder.newPaint().build();
    private final Paint innerClearingStroke = PaintBuilder.newPaint().color(0xff000000).build();
    private final Paint clearingStroke = PaintBuilder.newPaint().color(0xff555555).build();
    private mColorPickerView colorPicker;

    public mLightnessSlider(Context context) {
        super(context);
        defConstructor();
    }

    public mLightnessSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        defConstructor();
    }

    public mLightnessSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        defConstructor();
    }

    void defConstructor() {
        this.handleRadius = MyApp.dpToPx(getContext(), 12);
    }


    public void setColor(int color) {
        this.color = color;
        this.value = Utils.lightnessOfColor(color);
        if (bar != null) {
            updateBar();
            invalidate();
        }
    }

    @Override
    protected void drawBar(Canvas barCanvas) {
        final int width = barCanvas.getWidth();
        final int height = barCanvas.getHeight();

        final float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);
        final int l = Math.max(2, width / 256);
        for (int x = 0; x <= width; x += l) {
            hsv[2] = (float) x / (width - 1);
            barPaint.setColor(Color.HSVToColor(hsv));
            barCanvas.drawRect(x, 0, x + l, height, barPaint);
        }
    }


    @Override
    protected void drawHandle(Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y, handleRadius, clearingStroke);
        canvas.drawCircle(x, y, handleRadius * 0.8f, innerClearingStroke);

        solid.setColor(Utils.colorAtLightness(color, value));
        canvas.drawCircle(x, y, handleRadius * 0.60f, solid);
    }


    public void setColorPicker(mColorPickerView colorPicker) {
        this.colorPicker = colorPicker;
    }


    @Override
    protected void onValueChanged(float value) {
        if (colorPicker != null)
            colorPicker.setLightness(value);
    }
}
