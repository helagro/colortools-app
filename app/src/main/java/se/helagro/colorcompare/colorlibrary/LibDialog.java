package se.helagro.colorcompare.colorlibrary;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.hlag.colorcompare.R;

import java.util.ArrayList;

import se.helagro.colorcompare.Color;
import se.helagro.colorcompare.MyApp;

public class LibDialog extends DialogFragment implements LibAdapter.OnColorClickedListener {
    public interface LibColorPickedListener {
        void onInputColor(int color);
    }

    static final String TAG = "LibDialog";
    private final ArrayList<Color> colors;
    private final int currentColor;
    private final int checkedId;
    private LibColorPickedListener libColorPickedListener;

    public LibDialog(@NonNull ArrayList<Color> colors, int currentColor, int checkedId, LibColorPickedListener libColorPickedListener) {
        this.colors = colors;
        this.currentColor = currentColor;
        this.checkedId = checkedId;
        this.libColorPickedListener = libColorPickedListener;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.AppTheme);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.color_lib_dialog, container, false);

        final Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            if (window != null){
                window.setBackgroundDrawable(new ColorDrawable(-1291845632));
                window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            }
        }


        final LibGridView gridView = v.findViewById(R.id.dialog_gridview);
        gridView.setAdapter(new LibAdapter(getContext(), checkedId, colors, currentColor, this));
        gridView.setOnNoItemClickListener(this::dismiss);
        gridView.setEmptyView(v.findViewById(R.id.lib_empty_message));

        ((View) gridView.getParent()).setOnClickListener(view -> dismiss());

        return v;
    }

    @Override
    public void onColorClicked(int color) {
        libColorPickedListener.onInputColor(color);
        dismiss();
    }


    @Override
    public void onPause() {
        super.onPause();

        View view = getView();
        if (view != null)
            MyApp.hideKeyboardFrom(getContext(), view.getRootView());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        libColorPickedListener = null;
    }
}
