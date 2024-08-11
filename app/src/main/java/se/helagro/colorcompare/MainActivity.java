package se.helagro.colorcompare;

import static android.graphics.Color.alpha;

import android.content.SharedPreferences;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Selection;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;

import com.hlag.colorcompare.R;

import java.util.ArrayList;

import se.helagro.colorcompare.colorlibrary.LibDialog;
import se.helagro.colorcompare.colorpicker.MyAlphaSlider;
import se.helagro.colorcompare.colorpicker.MyColorPickerView;
import se.helagro.colorcompare.colorpicker.MyLightnessSlider;

public class MainActivity extends AppCompatActivity implements LibDialog.LibColorPickedListener,
        MyAlphaSlider.OnNewAlphaListener, PopupMenu.OnMenuItemClickListener {

    /* ------------------------ VARIABLES ----------------------- */

    private final static String DISPLAY_MODE_BTN_ID = "checkbtn_id_clr";
    private final static String TAG = "Color Activity";

    protected static boolean is_argb;
    protected static boolean byte_alpha;
    protected static int nightMode;
    static boolean wasOpaque = true;

    private boolean dimAllowed = true;
    private int currentColor;
    private int clrShowSelected = 0;

    private SharedPreferences sp;
    private Color savedColor;
    private ArrayList<Color> colors;
    private DbHelper dbHelper;

    private FrameLayout root;
    private RadioGroup clrShowGroup;
    private EditTxtKeyboard colorText;
    private MyColorPickerView colorWheel;
    private RadioGroup displayOptGroup;

    /* ------------------------ LIFECYCLE ----------------------- */

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);

        loadFromStorage();
        SettingsDialog.setDarkMode(nightMode);
        setContentView(R.layout.activity_main);

        root = findViewById(R.id.root);
        getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.black));
        getWindow().setNavigationBarColor(-16777216);

        setupMenu();

        // Setup views
        final ImageButton arrow = findViewById(R.id.clr_arrow);
        final MyLightnessSlider mBarLightness = findViewById(R.id.v_lightness_slider);
        final MyAlphaSlider mBarAlpha = findViewById(R.id.alpha_slider);

        colorWheel = findViewById(R.id.color_picker_view);
        colorText = findViewById(R.id.editText_color);
        displayOptGroup = findViewById(R.id.color_radiogroup);
        clrShowGroup = findViewById(R.id.clr_show_group);

        clrShowGroup.setBackground(
                new TileDrawable(
                        AppCompatResources.getDrawable(this, R.drawable.checkered_pattern),
                        Shader.TileMode.REPEAT));

        mBarAlpha.setOnNewAlphaListener(this);
        colorText.setOnBackPressedListener(this::updateClrText);
        arrow.setOnClickListener(view1 -> attentionToast(getString(R.string.arrow_message)));

        /* ------------------------ LISTENERS ----------------------- */

        clrShowGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            clrShowSelected = clrShowGroup.getCheckedRadioButtonId() == R.id.clr_show ? 0 : 1;
            arrow.animate().rotation(180 * clrShowSelected).setDuration(250);

            currentColor = ((ColorDrawable) clrShowGroup.getChildAt(clrShowSelected).getBackground()).getColor();
            colorWheel.setColor(currentColor);
            updateClrText();
            onNewAlpha((float) alpha(currentColor) / 255);
        });

        colorWheel.addOnColorChangedListener(i -> {
            currentColor = i;
            clrShowGroup.getChildAt(clrShowSelected).setBackgroundColor(i);
            updateClrText();
        });

        colorText.setOnEditorActionListener(
                (textView, i, keyEvent) -> {
                    final String text = textView.getText().toString();

                    try {
                        final int color = ColorConvert.ColorIntFromString(text);
                        onInputColor(color);
                    } catch (final IllegalArgumentException e) {
                        attentionToast(getString(R.string.non_valid_color_input));
                        updateClrText();
                    }

                    return false;
                });

        displayOptGroup.setOnCheckedChangeListener((radioGroup, id) -> {
            updateClrText();
            sp.edit().putInt(DISPLAY_MODE_BTN_ID, id).apply();
        });

        /* ---------------------- RESTORE STATE --------------------- */

        final int color = sp.getInt("color_int", -16729344);
        mBarLightness.post(() -> mBarLightness.setColor(color));
        mBarAlpha.post(() -> mBarAlpha.setColor(color));
        onInputColor(color);

        clrShowGroup.getChildAt(1).setBackgroundColor(sp.getInt("color_int2", -2236451));

        final int checked = sp.getInt(DISPLAY_MODE_BTN_ID, R.id.color_hex_btn);
        ((RadioButton) findViewById(checked)).setChecked(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        sp.edit()
                .putInt("color_int", ((ColorDrawable) clrShowGroup.getChildAt(0).getBackground()).getColor())
                .putInt("color_int2", ((ColorDrawable) clrShowGroup.getChildAt(1).getBackground()).getColor())
                .apply();

        MyApp.hideKeyboardFrom(this, this.getCurrentFocus());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void recreate() {
        clrShowGroup.check(R.id.clr_show);
        super.recreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DbHelper.getInstance(this).close();
    }

    /* ------------------------ LISTENERS ----------------------- */

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        if (item.getItemId() == R.id.more_save) {
            if (savedColor == null) { // check from where it was made
                new NameDialog(this, currentColor, name -> {
                    final Color color = new Color(name, currentColor, -1);
                    dbHelper.updateColor(color);
                    colors.add(0, color);
                });
            } else {
                dbHelper.delColor(savedColor.id);
                colors.remove(savedColor);
            }

        } else if (item.getItemId() == R.id.more_save_lib) {
            new LibDialog(colors, currentColor, displayOptGroup.getCheckedRadioButtonId(), this)
                    .show(getSupportFragmentManager(), "LibDialog");
        } else if (item.getItemId() == R.id.more_opt) {
            new SettingsDialog(currentColor).show(getSupportFragmentManager(), "MainAct");
        }

        return true;
    }

    @Override
    public void onInputColor(final int color) {
        colorWheel.setColor(color);

        currentColor = color;
        clrShowGroup.getChildAt(clrShowSelected).setBackgroundColor(color);
        updateClrText();
        onNewAlpha(alpha(color) / 255f);
    }

    @Override
    public void onNewAlpha(final float alpha) {
        final boolean isOpaque = (alpha == 1);

        if (isOpaque != wasOpaque) {
            if (isOpaque) {
                ((RadioButton) displayOptGroup.getChildAt(1)).setText("RGB");

            } else {
                String alphaRgbLabel = is_argb ? "ARGB" : "RGBA";
                ((RadioButton) displayOptGroup.getChildAt(1)).setText(alphaRgbLabel);
            }
            wasOpaque = isOpaque;
        }
    }

    /* ---------------------- OTHER METHODS --------------------- */

    void loadFromStorage() {
        sp = MyApp.getSp(this);

        nightMode = sp.getInt(SettingsDialog.NIGHT_MODE_MODE, 0);
        is_argb = sp.getBoolean(SettingsDialog.IS_ARGB, true);
        byte_alpha = sp.getBoolean(SettingsDialog.BYTE_ALPHA, true);

        dbHelper = DbHelper.getInstance(getApplicationContext());
        colors = dbHelper.getColors();
    }

    private void setupMenu() {
        final ImageButton moreBtn = findViewById(R.id.more);
        final PopupMenu moreMenu = new PopupMenu(this, moreBtn);

        moreMenu.inflate(R.menu.more);
        moreMenu.setOnMenuItemClickListener(this);

        final MenuPopupHelper menuPopupHelper = new MenuPopupHelper(
                new ContextThemeWrapper(this, R.style.Color_Opt_Popup),
                (MenuBuilder) moreMenu.getMenu(),
                moreBtn);
        menuPopupHelper.setForceShowIcon(true);

        menuPopupHelper.setOnDismissListener(() -> {
            lighten();
            updateClrText();
        });

        moreBtn.setOnClickListener(view12 -> {
            darken();

            savedColor = null;
            final MenuItem saveItem = moreMenu.getMenu().getItem(0);

            for (final Color color : colors) {
                if (color.color == currentColor) {
                    saveItem.setIcon(R.drawable.ic_delete);
                    saveItem.setTitle(getString(R.string.Remove_color_text) + color.name + "\"");
                    savedColor = color;
                    break;
                }
            }

            if (savedColor == null) {
                saveItem.setIcon(R.drawable.ic_save);
                saveItem.setTitle(R.string.save);
            }

            menuPopupHelper.show();
            colorText.setText("");
        });

    }

    private void updateClrText() {
        colorText.setText(
                ColorConvert.ColorIntToString(displayOptGroup.getCheckedRadioButtonId(), currentColor));

        if (colorText.hasFocus()) {
            final int position = colorText.length();
            final Editable editable = colorText.getText();
            Selection.setSelection(editable, position);
        }
    }

    public void attentionToast(final String message) {
        dimAllowed = false;
        darken();
        new Handler().postDelayed(() -> {
            dimAllowed = true;
            lighten();
        }, 3500);
        Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void darken() {
        root.animate().alpha(0.7f).setDuration(500);
    }

    private void lighten() {
        if (!dimAllowed)
            return;

        try {
            root.animate().alpha(1f).setDuration(500);
        } catch (Exception ignored) {
        }
    }

}
