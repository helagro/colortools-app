package se.helagro.colorcompare.colorlibrary;

import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import se.helagro.colorcompare.Color;
import se.helagro.colorcompare.EditTxtKeyboard;

public class ColorViewHolder {
    View colorDisplay;
    ConstraintLayout infoOverlay;
    EditTxtKeyboard nameEdit;
    EditTxtKeyboard colorEdit;

    int pos;
    Color color;
}
