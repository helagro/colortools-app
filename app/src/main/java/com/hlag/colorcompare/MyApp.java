package com.hlag.colorcompare;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.Locale;

public class MyApp extends Application {

    static final String TAG = "MyApp";
    static final String SP_RATED = "rated";
    static final String SP_CRASHED = "crashed";

    private Thread.UncaughtExceptionHandler defaultUEH;
    private Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = (thread, ex) -> {

        SharedPreferences.Editor sp_edit = getSp(this).edit();
        if(getSp(this).getBoolean(SP_CRASHED, false)){ // should fix back to back crash
            sp_edit.clear().apply();
        }
        sp_edit.putBoolean(SP_CRASHED, true).apply();

        defaultUEH.uncaughtException(thread, ex);
    };


    @Override
    public void onCreate() {
        super.onCreate();

        defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);

        if(getSp(getApplicationContext()).getBoolean("first", true)){ //called also after crash - ok
            getApplicationContext().openOrCreateDatabase(DbHelper.DB_NAME, MODE_PRIVATE, null)
                    .execSQL("CREATE TABLE IF NOT EXISTS "+ DbHelper.TABLE_NAME +" (id INTEGER primary key, name TEXT, color INTEGER, updated INTEGER)");

            getSp(getApplicationContext()).edit().putBoolean("first", false).apply();
        }

    }


    static SharedPreferences getSp(Context context){
        return context.getSharedPreferences("default_preferences", Activity.MODE_PRIVATE);
    }

    public static boolean isRTL() {
        final int directionality = Character.getDirectionality(Locale.getDefault().getDisplayName().charAt(0));
        return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }


    public static int dpToPx(Context context, int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources().getDisplayMetrics());
    }

    public static int pxToDp(int px, Context context){
        return px / (context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static void hideKeyboardFrom(Context context, View view) {
        if(view == null){
            return;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }



}
