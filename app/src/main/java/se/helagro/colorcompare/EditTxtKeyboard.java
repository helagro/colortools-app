package se.helagro.colorcompare;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.widget.AppCompatEditText;

public class EditTxtKeyboard extends AppCompatEditText {

    public interface OnBackPressedListener {
        void onBackPressed();
    }

    private OnBackPressedListener onBackPressedListener;


    public EditTxtKeyboard(Context context) {
        super(context);
    }

    public EditTxtKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTxtKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnBackPressedListener(final OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            clearFocus();
            if (onBackPressedListener != null)
                onBackPressedListener.onBackPressed();
        }
        return false;
    }


    @Override
    public void onEditorAction(int actionCode) {
        super.onEditorAction(actionCode);
        removeFocus();
    }

    public void removeFocus() {
        final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        clearFocus();
    }

}

