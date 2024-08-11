package se.helagro.colorcompare;

import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hlag.colorcompare.R;

import java.io.InputStream;

public class LicensesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        getWindow().setNavigationBarColor(-16777216);

        findViewById(R.id.back_btn).setOnClickListener(v -> {
            finish();
        });

        final TextView textView = findViewById(R.id.quadflask_textview);
        try {
            final Resources res = getResources();
            final InputStream in_s = res.openRawResource(R.raw.quadflask_license);
            final byte[] b = new byte[in_s.available()];
            in_s.read(b);
            in_s.close();
            textView.setText(new String(b));
        } catch (Exception e) {
            textView.setText(R.string.can_not_show_license);
        }
    }

}
