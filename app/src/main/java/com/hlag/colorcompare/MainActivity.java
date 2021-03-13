

package com.hlag.colorcompare;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.hlag.colorcompare.colorpicker.mAlphaSlider;
import com.hlag.colorcompare.colorpicker.mColorPickerView;
import com.hlag.colorcompare.colorpicker.mLightnessSlider;

import java.util.ArrayList;

import static android.graphics.Color.alpha;
import static com.hlag.colorcompare.ColorConvert.ColorIntFromString;
import static com.hlag.colorcompare.ColorConvert.ColorIntToString;
import static com.hlag.colorcompare.ColorConvert.noErr;

public class MainActivity extends AppCompatActivity implements LibDialog.LibColorPickedListener, mAlphaSlider.OnNewAlphaListener, PopupMenu.OnMenuItemClickListener, BillingHelper.BillingListener {
    /*
        starting overrides
        ui methods
        my implementation methods
        helper methods
        closing overrides
     */

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

    private TextView noAdsView;
    private AdView adView;


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

        BillingHelper.getInstance(this);
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
                    double color = ColorIntFromString(textView.getText().toString());
                    if (noErr(color)) {
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


    private boolean adsSetup = false;

    @Override
    public void setupAds() {
        if (bought_premium || adsSetup) {
            return;
        }
        adsSetup = true;

        //No-ads view setup
        noAdsView = findViewById(R.id.no_ads_premium_ads);
        final SpannableStringBuilder ssb = new SpannableStringBuilder(getText(R.string.premium) + " ");
        final Drawable d1 = ContextCompat.getDrawable(this, R.drawable.ic_no_ads_5);
        final int wAndH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 25, this.getResources().getDisplayMetrics());
        d1.setBounds(0, 0, wAndH, wAndH);
        ssb.setSpan(new ImageSpan(d1, ImageSpan.ALIGN_BASELINE), ssb.length() - 1, ssb.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        noAdsView.setText(ssb, TextView.BufferType.SPANNABLE);
        noAdsView.setOnClickListener(v -> buyPremium());

        //ads view
        MobileAds.initialize(getApplicationContext(), initializationStatus -> {

        });
        adView = findViewById(R.id.adView);
        final AdRequest adRequest = new AdRequest.Builder().addTestDevice("924D31683A7863ECD8454901234999F4").build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                noAdsView.setVisibility(View.GONE);
                adView.setVisibility(View.VISIBLE);
                super.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                adView.setVisibility(View.GONE);
                noAdsView.setVisibility(View.VISIBLE);
            }
        });
    }


    public void disableAds() {
        if (bought_premium) { //don't disable ads twice
            return;
        }

        adsSetup = true;
        bought_premium = true;

        if (adView == null) {
            adView = findViewById(R.id.adView);
            noAdsView = findViewById(R.id.no_ads_premium_ads);
        }
        adView.setAdListener(null);
        adView.destroy();

        final ViewGroup parent = (ViewGroup) adView.getParent();
        parent.removeView(adView);
        root.removeView(noAdsView);
        root.invalidate();
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
            for (com.hlag.colorcompare.Color color : colors) {
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
        switch (item.getItemId()) {
            case R.id.more_save:
                if (savedColor == null) { //check from where it was made
                    if (bought_premium || colors.size() < 8) {
                        new NameDialog(this, currentColor, name -> {
                            final Color color = new com.hlag.colorcompare.Color(name, currentColor, -1);
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
                break;
            case R.id.more_save_lib:
                new LibDialog(colors, currentColor, displayOptGroup.getCheckedRadioButtonId(), this).show(getSupportFragmentManager(), "LibDialog");
                break;
            case R.id.more_rate:
                new ContactDialog(this).show();

                break;
            case R.id.more_ads:
                buyPremium();
                break;
            case R.id.more_opt:
                new SettingsDialog(currentColor).show(getSupportFragmentManager(), "MainAct");
                break;
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
                ColorIntToString(displayOptGroup.getCheckedRadioButtonId(), currentColor));
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
    public Activity getActivity() {
        return this;
    }

    void buyPremium() {
        final ArrayList<String> skuList = new ArrayList<>();
        skuList.add("com.hlag.colorcompare.premium");
        BillingHelper.getInstance(this).buy(skuList);
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
        disableAds();
        BillingHelper.getInstance(this).close();
        bought_premium = false;
    }


}
