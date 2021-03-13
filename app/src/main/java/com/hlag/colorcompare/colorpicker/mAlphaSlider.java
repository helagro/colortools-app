package com.hlag.colorcompare.colorpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.flask.colorpicker.Utils;
import com.flask.colorpicker.builder.PaintBuilder;
import com.hlag.colorcompare.MyApp;
import com.hlag.colorcompare.R;

public class mAlphaSlider extends mAbsCustomSlider {

    public interface OnNewAlphaListener {
        void onNewAlpha(float alpha);
    }

    OnNewAlphaListener onNewAlphaListener;
    public int color;
    private final Paint alphaPatternPaint = PaintBuilder.newPaint().build();
    private final Paint barPaint = PaintBuilder.newPaint().build();
    private final Paint solid = PaintBuilder.newPaint().build();
    private final Paint clearingStroke = PaintBuilder.newPaint().color(0xffaaaaaa).build(); 
    private mColorPickerView colorPicker;

    public mAlphaSlider(Context context) {
        super(context);
    }

    public mAlphaSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public mAlphaSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);

        setRotation(270);

        int mBarAlphaTranslation = (int) getResources().getDimension(R.dimen.alpha_bar_translation);
        setTranslationX(MyApp.isRTL() ? mBarAlphaTranslation : -mBarAlphaTranslation);
    }


    public void setOnNewAlphaListener(OnNewAlphaListener onNewAlphaListener) {
        this.onNewAlphaListener = onNewAlphaListener;
    }

    @Override
    protected void createBitmaps() {
        super.createBitmaps();
        alphaPatternPaint.setShader(PaintBuilder.createAlphaPatternShader(barHeight / 2));
    }

    @Override
    protected void drawBar(Canvas barCanvas) {
        int width = barCanvas.getWidth();
        int height = barCanvas.getHeight();

        barCanvas.drawRect(0, 0, width, height, alphaPatternPaint);
        int l = Math.max(2, width / 256);
        for (int x = 0; x <= width; x += l) {
            float alpha = (float) x / (width - 1);
            barPaint.setColor(color);
            barPaint.setAlpha(Math.round(alpha * 255));
            barCanvas.drawRect(x, 0, x + l, height, barPaint);
        }
    }

    @Override
    protected void onValueChanged(float value) {
        onNewAlphaListener.onNewAlpha(value);
        if (colorPicker != null)
            colorPicker.setAlphaValue(value);
    }

    @Override
    protected void drawHandle(Canvas canvas, float x, float y) {
        solid.setColor(color);
        solid.setAlpha(Math.round(value * 255));
        canvas.drawCircle(x, y, handleRadius, clearingStroke);
        if (value < 1)
            canvas.drawCircle(x, y, handleRadius * 0.75f, alphaPatternPaint);
        canvas.drawCircle(x, y, handleRadius * 0.75f, solid);
    }

    public void setColorPicker(mColorPickerView colorPicker) {
        this.colorPicker = colorPicker;
    }

    public void setColor(int color) {
        this.color = color;
        this.value = Utils.getAlphaPercent(color);
        if (bar != null) {
            updateBar();
            invalidate();
        }
    }

}
