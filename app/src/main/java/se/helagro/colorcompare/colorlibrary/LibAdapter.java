package se.helagro.colorcompare.colorlibrary;

import static se.helagro.colorcompare.ColorConvert.ColorIntFromString;
import static se.helagro.colorcompare.ColorConvert.ColorIntToString;

import android.content.Context;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.Editable;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.ColorUtils;

import com.hlag.colorcompare.R;

import java.util.ArrayList;

import se.helagro.colorcompare.Color;
import se.helagro.colorcompare.DbHelper;
import se.helagro.colorcompare.EditTxtKeyboard;
import se.helagro.colorcompare.NameDialog;
import se.helagro.colorcompare.TileDrawable;

class LibAdapter extends ArrayAdapter<Color> {

    static final private String TAG = "LibAdapter";
    // from init
    private final DbHelper dbHelper;
    private final TileDrawable tileDrawable;
    private final OnColorClickedListener onColorClickedListener;
    private final ArrayList<Color> colors;
    private final int currentColor;
    private final LayerDrawable currentColorDraw;
    private final int checkedId;
    private final View.OnLongClickListener selectAllOnLongClick = v -> {
        ((EditTxtKeyboard) v).selectAll();
        return false;
    };
    // for cursor jump
    private int selectedPos = -1;
    private int selectedId = -1;

    LibAdapter(@NonNull final Context context, final int checkedId, final ArrayList<Color> colors, final int currentColor, final OnColorClickedListener onColorClickedListener) {
        super(context, R.layout.color_row_layout, colors);

        this.onColorClickedListener = onColorClickedListener;
        this.colors = colors;
        this.currentColor = currentColor;
        this.checkedId = checkedId;

        dbHelper = DbHelper.getInstance(context.getApplicationContext());
        tileDrawable = new TileDrawable(
                AppCompatResources.getDrawable(getContext(), R.drawable.checkered_pattern),
                Shader.TileMode.REPEAT
        );
        currentColorDraw = (LayerDrawable) AppCompatResources.getDrawable(getContext(), R.drawable.color_preview);
        ((GradientDrawable) currentColorDraw.getDrawable(1)).setColor(currentColor);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ColorViewHolder viewHolder;

        //setup views
        if (convertView == null) {

            viewHolder = new ColorViewHolder();
            viewHolder.pos = position;
            viewHolder.color = getItem(position);

            convertView = LayoutInflater.from(getContext()).inflate(R.layout.color_row_layout, parent, false);
            convertView.setOnClickListener(view -> {
                onColorClickedListener.onColorClicked(viewHolder.color.color);
            });

            convertView.findViewById(R.id.card_color_show_pattern).setBackground(tileDrawable);
            convertView.findViewById(R.id.color_row_del).setOnClickListener(view ->
                    inflateMenu(viewHolder.color, viewHolder, view));
            viewHolder.colorDisplay = convertView.findViewById(R.id.card_color_show_color);
            viewHolder.infoOverlay = convertView.findViewById(R.id.color_row_overlay);

            viewHolder.nameEdit = convertView.findViewById(R.id.color_row_name);
            viewHolder.nameEdit.setOnLongClickListener(selectAllOnLongClick);
            viewHolder.nameEdit.setOnFocusChangeListener((view, hasFocus) -> {
                if (hasFocus) {
                    onEditTextFocus(viewHolder.pos, viewHolder.nameEdit.getId(), (GridView) parent);
                } else {
                    final Editable editable = viewHolder.nameEdit.getText();

                    if (editable == null)
                        viewHolder.color.name = "";
                    else
                        viewHolder.color.name = editable.toString();

                    dbHelper.updateColor(viewHolder.color);
                }
            });

            viewHolder.colorEdit = convertView.findViewById(R.id.color_row_color);
            viewHolder.colorEdit.setOnLongClickListener(selectAllOnLongClick);
            viewHolder.colorEdit.setOnFocusChangeListener((view, hasFocus) -> {
                if (hasFocus) {
                    onEditTextFocus(viewHolder.pos, viewHolder.colorEdit.getId(), (GridView) parent);
                } else {
                    final Editable editable = viewHolder.colorEdit.getText();
                    if (editable == null) {
                        Log.e(TAG, "Editable is null");
                        return;
                    }

                    final double colorInp = ColorIntFromString(editable.toString());
                    try {
                        setViewColors((int) colorInp, viewHolder);
                        viewHolder.color.color = (int) colorInp;
                        dbHelper.updateColor(viewHolder.color);
                    } catch (IllegalArgumentException e){
                        Toast.makeText(getContext(), R.string.non_valid_color_input, Toast.LENGTH_LONG).show();
                        viewHolder.colorEdit.setText(ColorIntToString(checkedId, viewHolder.color.color));
                    }
                }
            });

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ColorViewHolder) convertView.getTag();
            viewHolder.pos = position;
            viewHolder.color = getItem(position);

            if (position == selectedPos) {
                final EditTxtKeyboard editTxtKeyboard = convertView.findViewById(selectedId);
                final int[] selection = {editTxtKeyboard.getSelectionStart(), editTxtKeyboard.getSelectionEnd()};
                viewHolder.nameEdit.post(() -> {
                    if (position == selectedPos) {
                        try {
                            editTxtKeyboard.setSelection(selection[0], selection[1]);
                        } catch (IndexOutOfBoundsException ignored) {
                        }
                    }
                });
            }
        }

        final Color color = viewHolder.color;
        if (color == null) {
            viewHolder.nameEdit.setText("");
        } else {
            viewHolder.nameEdit.setText(color.name);
            setViewColors(viewHolder.color.color, viewHolder);
        }
        return convertView;
    }

    private void onEditTextFocus(final int pos, final int id, final GridView gridView) {
        selectedPos = pos;
        selectedId = id;
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        gridView.post(() -> gridView.setSelection(pos));
    }

    private void setViewColors(int color, final ColorViewHolder viewHolder) {
        viewHolder.colorDisplay.setBackgroundColor(color);
        viewHolder.colorEdit.setText(ColorIntToString(checkedId, color));

        color = color | 0xFF000000; // Set alpha to full

        viewHolder.nameEdit.setTextColor(color);
        viewHolder.colorEdit.setTextColor(color);

        if (ColorUtils.calculateLuminance(color) > NameDialog.LUMINANCE_THRESHOLD)
            viewHolder.infoOverlay.setBackgroundColor(-1436129690);
        else
            viewHolder.infoOverlay.setBackgroundColor(-1428168737);
    }

    private void inflateMenu(final Color color, final ColorViewHolder viewHolder, final View imgBtn) {
        final PopupMenu popupMenu = new PopupMenu(getContext(), imgBtn);
        popupMenu.inflate(R.menu.color_more);

        final MenuItem updateColorItem = popupMenu.getMenu().getItem(0);
        updateColorItem.setIcon(currentColorDraw);

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.delete_color) {
                dbHelper.delColor(color.id);
                colors.remove(color);
                notifyDataSetChanged();

            } else if (item.getItemId() == R.id.update_color) {
                color.color = currentColor;
                setViewColors(color.color, viewHolder);
                dbHelper.updateColor(color);
            }

            return true;
        });

        final MenuPopupHelper menuPopupHelper = new MenuPopupHelper(
                new ContextThemeWrapper(getContext(), R.style.Color_Opt_Popup), (MenuBuilder) popupMenu.getMenu(), imgBtn); // baseview
        menuPopupHelper.setForceShowIcon(true);
        menuPopupHelper.show();
    }

    interface OnColorClickedListener {
        void onColorClicked(int color);
    }

}
