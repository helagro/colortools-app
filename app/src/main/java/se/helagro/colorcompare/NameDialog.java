package se.helagro.colorcompare;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.core.graphics.ColorUtils;

import com.hlag.colorcompare.R;

public class NameDialog extends Dialog {
    public interface OnNameEnteredListener {
        void onNameEntered(String name);
    }

    public static final float LUMINANCE_THRESHOLD = 0.4f;
    private final OnNameEnteredListener onNameEnteredListener;
    private int color;

    NameDialog(final Context context, int color, final OnNameEnteredListener onNameEnteredListener) {
        super(context);
        this.onNameEnteredListener = onNameEnteredListener;
        this.color = color;

        show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_input_dialog);

        final Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        final EditText editText = findViewById(R.id.name_dialog_edittext);
        editText.requestFocus();
        final Button doneBtn = findViewById(R.id.name_dialog_done);
        final Button cancelBtn = findViewById(R.id.name_dialog_cancel);

        color = color | 0xFF000000;
        editText.setTextColor(color);
        if (ColorUtils.calculateLuminance(color) > LUMINANCE_THRESHOLD) {

            findViewById(R.id.name_dialog_grid).setBackgroundTintList((ColorStateList.valueOf(-10066330)));

            final ColorStateList tint = ColorStateList.valueOf(-6974059);
            doneBtn.setBackgroundTintList(tint);
            cancelBtn.setBackgroundTintList(tint);
        }

        doneBtn.setOnClickListener(view -> {
            onNameEnteredListener.onNameEntered(editText.getText().toString());
            dismiss();
        });

        cancelBtn.setOnClickListener(view -> dismiss());
    }

}
