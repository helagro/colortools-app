package com.hlag.colorcompare;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;


public class ContactDialog extends Dialog {
    ContactDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_dialog_layout);


        findViewById(R.id.rate_btn).setOnClickListener(v -> {
            anyClicked();

            final Uri uri = Uri.parse("market://details?id=" + getContext().getPackageName());
            final Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                getContext().startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + getContext().getPackageName())));
            }

            Toast.makeText(getContext().getApplicationContext(), R.string.thanks, Toast.LENGTH_LONG).show();
        });

        findViewById(R.id.contact_btn).setOnClickListener(v -> {
            anyClicked();
            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("plain/text");
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"info.hlag@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "App Feedback");
            try {
                getContext().startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            } catch (Exception ignored){}
        });

    }

    private void anyClicked(){
        MyApp.getSp(getContext()).edit().putBoolean("rated", true).apply();
        MainActivity.rated = true;
        dismiss();
    }
}
