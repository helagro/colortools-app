

package se.helagro.colorcompare;

import static android.graphics.Color.alpha;

import android.annotation.SuppressLint;
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
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;

import com.hlag.colorcompare.R;

import java.util.ArrayList;

import se.helagro.colorcompare.colorpicker.mAlphaSlider;
import se.helagro.colorcompare.colorpicker.mColorPickerView;
import se.helagro.colorcompare.colorpicker.mLightnessSlider;

public class MainActivity extends AppCompatActivity implements LibDialog.LibColorPickedListener, mAlphaSlider.OnNewAlphaListener, PopupMenu.OnMenuItemClickListener {

    private final static String TAG = "Color Activity";
    final static String DISPLAY_MODE_BTN_ID = "checkbtn_id_clr";

    private SharedPreferences sp;
    private DbHelper dbHelper;
    private static boolean bought_premium = false;
    protected static boolean rated;
    protected static boolean is_argb;
    protected static boolean byte_alpha;
    protected static int nightMode;

    private ArrayList<Color> colors;
    private int currentColor;
    private Color savedColor;
    private int clrShowSelected = 0;

    private FrameLayout root;
    private RadioGroup clrShowGroup;
    private EditTxtKeyboard colorText;
    private mColorPickerView colorWheel;
    private RadioGroup displayOptGroup;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        loadFromStorage();
        SettingsDialog.setDarkMode(nightMode);

        super.onCreate(null);
        setContentView(R.layout.activity_main);

        root = findViewById(R.id.root);
        getWindow().setBackgroundDrawable(getDrawable(R.drawable.black));
        getWindow().setNavigationBarColor(-16777216);

        if (sp.getBoolean(MyApp.SP_CRASHED, false)) { //resets in savedinstancestate
            attention_toast(getString(R.string.crash_apoligy));
        }

        setupMenu();

        //setup views
        clrShowGroup = findViewById(R.id.clr_show_group);
        clrShowGroup.setBackground(new TileDrawable(this.getDrawable(R.drawable.checkered_pattern), Shader.TileMode.REPEAT));
        final ImageButton arrow = findViewById(R.id.clr_arrow);
        colorWheel = findViewById(R.id.color_picker_view);
        colorText = findViewById(R.id.editText_color);
        final mLightnessSlider mBarLightness = findViewById(R.id.v_lightness_slider);
        final mAlphaSlider mBarAlpha = findViewById(R.id.alpha_slider);
        displayOptGroup = findViewById(R.id.color_radiogroup);

        clrShowGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            clrShowSelected = clrShowGroup.getCheckedRadioButtonId() == R.id.clr_show ? 0 : 1;
            arrow.animate().rotation(180 * clrShowSelected).setDuration(300);

            currentColor = ((ColorDrawable) clrShowGroup.getChildAt(clrShowSelected).getBackground()).getColor();
            colorWheel.setColor(currentColor);
            updateClrText();
            onNewAlpha(alpha(currentColor) / 255);
        });
        arrow.setOnClickListener(view1 -> {
            attention_toast(getString(R.string.arrow_message));
        });
        colorWheel.addOnColorChangedListener(i -> {
            currentColor = i;
            clrShowGroup.getChildAt(clrShowSelected).setBackgroundColor(i);
            updateClrText();
        });
        mBarAlpha.setOnNewAlphaListener(this);
        colorText.setOnEditorActionListener(
                (textView, i, keyEvent) -> {
                    double color = ColorConvert.ColorIntFromString(textView.getText().toString());
                    if (ColorConvert.noErr(color)) {
                        onInputColor((int) color);
                    } else {
                        if (textView.getText().toString().equals("crash_and_reset")) {
                            throw new NullPointerException();
                        }
                        attention_toast(getString(R.string.non_valid_color_input));
                        updateClrText();
                    }
                    return false;
                });
        colorText.setOnBackPressedListener(this::updateClrText);
        colorText.setOnLongClickListener(v -> {
            colorText.selectAll();
            return false;
        });
        displayOptGroup.setOnCheckedChangeListener((radioGroup, id) -> {
            updateClrText();
            sp.edit().putInt(DISPLAY_MODE_BTN_ID, id).apply();
        });


        //restores last state
        final int color = sp.getInt("color_int", -16729344);
        mBarLightness.post(() -> mBarLightness.setColor(color));
        mBarAlpha.post(() -> mBarAlpha.setColor(color));
        onInputColor(color);

        clrShowGroup.getChildAt(1).setBackgroundColor(sp.getInt("color_int2", -2236451));

        final int checked = sp.getInt(DISPLAY_MODE_BTN_ID, R.id.color_hex_btn);
        try {
            ((RadioButton) findViewById(checked)).setChecked(true);
        } catch (Exception ignored) {
        }

    }


    void loadFromStorage() {
        sp = MyApp.getSp(this);
        rated = sp.getBoolean(MyApp.SP_RATED, false);
        nightMode = sp.getInt(SettingsDialog.NIGHT_MODE_MODE, 0);
        is_argb = sp.getBoolean(SettingsDialog.IS_ARGB, true);
        byte_alpha = sp.getBoolean(SettingsDialog.BYTE_ALPHA, true);
        dbHelper = DbHelper.getInstance(this.getApplicationContext());
        colors = dbHelper.getColors();
    }

    private void setupMenu() {
        final ImageButton moreBtn = findViewById(R.id.more);
        final PopupMenu moreMenu = new PopupMenu(this, moreBtn);

        moreMenu.inflate(R.menu.more);
        moreMenu.setOnMenuItemClickListener(this);

        final MenuPopupHelper menuPopupHelper = new MenuPopupHelper(new ContextThemeWrapper(this, R.style.Color_Opt_Popup), (MenuBuilder) moreMenu.getMenu(), moreBtn);
        menuPopupHelper.setForceShowIcon(true);

        menuPopupHelper.setOnDismissListener(() -> {
            lighten();
            updateClrText();
        });

        moreBtn.setOnClickListener(view12 -> {
            darken();

            savedColor = null;
            final MenuItem saveItem = moreMenu.getMenu().getItem(0);
            for (Color color : colors) {
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

            if (rated) {
                hideMenuItem(moreMenu, 2);
            }
            if (bought_premium) {
                hideMenuItem(moreMenu, 3);
            }

            menuPopupHelper.show();
            colorText.setText("");
        });

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.more_save){
            if (savedColor == null) { //check from where it was made
                if (bought_premium || colors.size() < 8) {
                    new NameDialog(this, currentColor, name -> {
                        final Color color = new Color(name, currentColor, -1);
                        dbHelper.updateColor(color);
                        colors.add(0, color);
                    });
                } else {
                    attention_toast(getString(R.string.need_premium_for_space));
                }
            } else {
                dbHelper.delColor(savedColor.id);
                colors.remove(savedColor);
            }
        } else if(item.getItemId() == R.id.more_save_lib){
            new LibDialog(colors, currentColor, displayOptGroup.getCheckedRadioButtonId(), this).show(getSupportFragmentManager(), "LibDialog");
        } else if(item.getItemId() == R.id.more_rate){
            new ContactDialog(this).show();
        }
        else if(item.getItemId() == R.id.more_opt){
            new SettingsDialog(currentColor).show(getSupportFragmentManager(), "MainAct");
        }

        return true;
    }

    private void hideMenuItem(PopupMenu popupMenu, int index) {
        popupMenu.getMenu().getItem(index).setVisible(false);
    }


    @Override
    public void onInputColor(int color) {
        colorWheel.setColor(color);

        currentColor = color;
        clrShowGroup.getChildAt(clrShowSelected).setBackgroundColor(color);
        updateClrText();
        onNewAlpha(alpha(color) / 255f);
    }


    static boolean wasOpaque = true;

    @Override
    public void onNewAlpha(float alpha) {
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


    @SuppressLint("SetTextI18n")
    private void updateClrText() {
        colorText.setText(
                ColorConvert.ColorIntToString(displayOptGroup.getCheckedRadioButtonId(), currentColor));
        if (colorText.hasFocus()) {
            int position = colorText.length();
            Editable etext = colorText.getText();
            Selection.setSelection(etext, position);
        }
    }


    private boolean dimAllowed = true;

    public void attention_toast(String message) {
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
        if (dimAllowed) {
            try {
                root.animate().alpha(1f).setDuration(500);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sp.edit().putInt("color_int", ((ColorDrawable) clrShowGroup.getChildAt(0).getBackground()).getColor()).apply();
        sp.edit().putInt("color_int2", ((ColorDrawable) clrShowGroup.getChildAt(1).getBackground()).getColor()).apply();

        MyApp.hideKeyboardFrom(this, this.getCurrentFocus());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        sp.edit().putBoolean(MyApp.SP_CRASHED, false).apply();
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
        bought_premium = false;
    }

}
