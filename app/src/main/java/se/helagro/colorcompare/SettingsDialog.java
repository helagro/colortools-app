package se.helagro.colorcompare;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;

import com.hlag.colorcompare.R;

public class SettingsDialog extends DialogFragment implements CompoundButton.OnCheckedChangeListener {

    SettingsDialog(int color) {
        this.color = color;
    }

    private final static String TAG = "SettingsDialog";
    final static String NIGHT_MODE_MODE = "night_mode";
    final static String IS_ARGB = "is_argb";
    final static String BYTE_ALPHA = "byte_alpha";

    private int color;
    private RadioButton argb, rgba, byte_alpha, float_alpha;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.settings_dialog, container, false);

        final Spinner dark_mode_opt = v.findViewById(R.id.dark_mode_opt);
        final ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(getContext(), R.array.dark_mode_options, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dark_mode_opt.setAdapter(arrayAdapter);

        dark_mode_opt.setSelection(MainActivity.nightMode, false);
        dark_mode_opt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == MainActivity.nightMode) {
                    return;
                }
                MyApp.getSp(getContext()).edit().putInt(NIGHT_MODE_MODE, position).apply();

                parent.setOnItemSelectedListener(null);
                getActivity().recreate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        argb = v.findViewById(R.id.opt_argb);
        rgba = v.findViewById(R.id.opt_rgba);
        byte_alpha = v.findViewById(R.id.opt_byte_alpha);
        float_alpha = v.findViewById(R.id.opt_float_alpha);

        argb.setChecked(MainActivity.is_argb);
        rgba.setChecked(!MainActivity.is_argb);
        byte_alpha.setChecked(MainActivity.byte_alpha);
        float_alpha.setChecked(!MainActivity.byte_alpha);

        argb.setOnCheckedChangeListener(this);
        rgba.setOnCheckedChangeListener(this);
        byte_alpha.setOnCheckedChangeListener(this);
        float_alpha.setOnCheckedChangeListener(this);


        v.findViewById(R.id.opt_colorpicker).setOnClickListener(view -> {
            Intent licenseIntent = new Intent(getContext(), LicensesActivity.class);
            licenseIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            getActivity().startActivity(licenseIntent);
        });


        return v;
    }

    static void setDarkMode(int position) {
        int nightModeMode;
        switch (position) {
            case 0:
                nightModeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
            case 1:
                nightModeMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                nightModeMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
        }

        AppCompatDelegate.setDefaultNightMode(nightModeMode);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) {
            return;
        }

        switch (buttonView.getId()) {
            case R.id.opt_argb:
                isArgbChange(true);
                rgba.setChecked(false);
                break;
            case R.id.opt_rgba:
                isArgbChange(false);
                argb.setChecked(false);
                break;
            case R.id.opt_byte_alpha:
                isByteAlphaChange(true);
                float_alpha.setChecked(false);
                break;
            case R.id.opt_float_alpha:
                isByteAlphaChange(false);
                byte_alpha.setChecked(false);
                break;
        }
    }

    private void isArgbChange(boolean isArgb) {
        MainActivity.is_argb = isArgb;
        MyApp.getSp(getContext()).edit().putBoolean(IS_ARGB, isArgb).apply();

        if (!ColorConvert.isOpaque(color)) {
            MainActivity.wasOpaque = true;
            ((MainActivity) getActivity()).onInputColor(color);
        }
    }

    private void isByteAlphaChange(boolean isByteValue) {
        MainActivity.byte_alpha = isByteValue;
        MyApp.getSp(getContext()).edit().putBoolean(BYTE_ALPHA, isByteValue).apply();

        if (!ColorConvert.isOpaque(color)) {
            ((MainActivity) getActivity()).onInputColor(color);
        }

    }

    @Override
    public void onPause() {
        dismiss();
        super.onPause();
    }

}
