package com.hlag.colorcompare;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;

public class LibDialog extends DialogFragment implements LibAdapter.OnColorClickedListener {
    interface LibColorPickedListener {
        void onInputColor(int color);
    }

    static final String TAG = "LibDialog";
    private ArrayList<Color> colors;
    private int currentColor;
    private int checkedId;
    private LibColorPickedListener libColorPickedListener;

    LibDialog(@NonNull ArrayList<Color> colors, int currentColor, int checkedId, LibColorPickedListener libColorPickedListener) {
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

        final Window window = getDialog().getWindow();
        window.setBackgroundDrawable(new ColorDrawable(-1291845632));
        window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);


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
        MyApp.hideKeyboardFrom(getContext(), getView().getRootView());

    }

    @Override
    public void onDetach() {
        super.onDetach();
        libColorPickedListener = null;
    }
}
