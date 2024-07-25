package se.helagro.colorcompare;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridView;

import java.util.Calendar;

public class LibGridView extends GridView {

    public interface OnNoItemClickListener {
        void onNoItemClicked();
    }

    private OnNoItemClickListener mOnNoItemClickListener;

    private static final int MAX_CLICK_DURATION = 200;
    private long startClickTime;
    private final float[] startCoords = new float[2];


    public LibGridView(Context context) {
        super(context);
    }

    public LibGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LibGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnNoItemClickListener(OnNoItemClickListener listener) {
        mOnNoItemClickListener = listener;
    }


    //only for calculating when to call OnNoItemClickListener
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startClickTime = Calendar.getInstance().getTimeInMillis();
                startCoords[0] = event.getX();
                startCoords[1] = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (Calendar.getInstance().getTimeInMillis() - startClickTime < MAX_CLICK_DURATION &&
                        pointToPosition((int) event.getX(), (int) event.getY()) == -1) {

                    View focusChild = getFocusedChild();
                    if (focusChild != null && focusChild.findFocus() instanceof EditTxtKeyboard) {
                        ((EditTxtKeyboard) focusChild.findFocus()).removeFocus();
                    } else {
                        double dpBetween = Math.sqrt(Math.pow(startCoords[0] - event.getX(), 2) + Math.pow(startCoords[1] - event.getY(), 2));
                        if (mOnNoItemClickListener != null && MyApp.pxToDp((int) Math.round(dpBetween), getContext()) < 40) {
                            mOnNoItemClickListener.onNoItemClicked();
                        }
                    }

                }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public int getSolidColor() {
        return 0xff222222;
    }


}