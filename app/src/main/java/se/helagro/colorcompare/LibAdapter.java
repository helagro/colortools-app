package se.helagro.colorcompare;

import android.content.Context;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
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
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;

import static se.helagro.colorcompare.ColorConvert.ColorIntFromString;
import static se.helagro.colorcompare.ColorConvert.ColorIntToString;
import static se.helagro.colorcompare.ColorConvert.noErr;

import com.hlag.colorcompare.R;

class LibAdapter extends ArrayAdapter<Color> {

    interface OnColorClickedListener {
        void onColorClicked(int color);
    }

    static final private String TAG = "LibAdapter";

    //from init
    private DbHelper dbHelper;
    private TileDrawable tileDrawable;
    private OnColorClickedListener onColorClickedListener;
    private ArrayList<Color> colors;
    private int currentColor;
    private final LayerDrawable currentColorDraw;
    private int checkedId;

    //for cursor jump
    private int selectedPos = -1;
    private int selectedId = -1;

   private final  View.OnLongClickListener selectAllOnLongClick = v -> {
       ((EditTxtKeyboard)v).selectAll();
       return false;
   };


    LibAdapter(@NonNull Context context, int checkedId, ArrayList<Color> colors, int currentColor, OnColorClickedListener onColorClickedListener) {
        super(context, R.layout.color_row_layout, colors);

        this.onColorClickedListener = onColorClickedListener;
        this.colors = colors;
        this.currentColor = currentColor;
        this.checkedId = checkedId;

        dbHelper = DbHelper.getInstance(context.getApplicationContext());
        tileDrawable = new TileDrawable(getContext().getDrawable(R.drawable.checkered_pattern), Shader.TileMode.REPEAT);
        currentColorDraw = (LayerDrawable) context.getDrawable(R.drawable.color_preview);
        ((GradientDrawable)currentColorDraw.getDrawable(1)).setColor(currentColor);
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
            convertView.setOnClickListener(view -> { ;
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
                    viewHolder.color.name = viewHolder.nameEdit.getText().toString();
                    dbHelper.updateColor(viewHolder.color);
                }
            });


            viewHolder.colorEdit = convertView.findViewById(R.id.color_row_color);
            viewHolder.colorEdit.setOnLongClickListener(selectAllOnLongClick);
            viewHolder.colorEdit.setOnFocusChangeListener((view, hasFocus) -> {
                if (hasFocus) {
                    onEditTextFocus(viewHolder.pos, viewHolder.colorEdit.getId(), (GridView) parent);
                } else {
                    double colorInp = ColorIntFromString(viewHolder.colorEdit.getText().toString());
                    if (noErr(colorInp)) {
                        setViewColors((int) colorInp, viewHolder);
                        viewHolder.color.color = (int) colorInp;
                        dbHelper.updateColor(viewHolder.color);
                    } else {
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

            if(position == selectedPos){
                final EditTxtKeyboard editTxtKeyboard = convertView.findViewById(selectedId);
                final int[] selection = {editTxtKeyboard.getSelectionStart(), editTxtKeyboard.getSelectionEnd()};
                viewHolder.nameEdit.post(() -> {
                    if(position == selectedPos){
                        try{editTxtKeyboard.setSelection(selection[0], selection[1]);}catch (IndexOutOfBoundsException ignored){}
                    }
                });
            }

        }

        viewHolder.nameEdit.setText(viewHolder.color.name);
        setViewColors(viewHolder.color.color, viewHolder);
        return convertView;
    }

    private void onEditTextFocus(final int pos, final int id, final GridView gridView) {
        selectedPos = pos;
        selectedId = id;
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        gridView.post(() -> {
            gridView.setSelection(pos);
        });
    }





    private void setViewColors(int color, ColorViewHolder viewHolder) {
        viewHolder.colorDisplay.setBackgroundColor(color);
        viewHolder.colorEdit.setText(ColorIntToString(checkedId, color));

        color = color | 0xFF000000;

        viewHolder.nameEdit.setTextColor(color);
        viewHolder.colorEdit.setTextColor(color);

        if (ColorUtils.calculateLuminance(color) > NameDialog.LUMINANCE_THRESHOLD)
            viewHolder.infoOverlay.setBackgroundColor(-1436129690);
        else
            viewHolder.infoOverlay.setBackgroundColor(-1428168737);
    }


    private void inflateMenu(Color color, ColorViewHolder viewHolder, View imgBtn) {
        final PopupMenu popupMenu = new PopupMenu(getContext(), imgBtn);
        popupMenu.inflate(R.menu.color_more);

        final MenuItem updateColorItem = popupMenu.getMenu().getItem(0);
        updateColorItem.setIcon(currentColorDraw);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.delete_color:
                    dbHelper.delColor(color.id);
                    colors.remove(color);
                    notifyDataSetChanged();
                    break;
                case R.id.update_color:
                    color.color = currentColor;
                    setViewColors(color.color, viewHolder);
                    dbHelper.updateColor(color);
            }
            return true;
        });

        final MenuPopupHelper menuPopupHelper = new MenuPopupHelper(
                new ContextThemeWrapper(getContext(), R.style.Color_Opt_Popup), (MenuBuilder) popupMenu.getMenu(), imgBtn); //baseview
        menuPopupHelper.setForceShowIcon(true);
        menuPopupHelper.show();
    }

}
