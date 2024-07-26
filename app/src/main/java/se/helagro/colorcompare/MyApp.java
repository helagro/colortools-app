package se.helagro.colorcompare;

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

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        try( android.database.sqlite.SQLiteDatabase db = context.openOrCreateDatabase(DbHelper.DB_NAME, MODE_PRIVATE, null)) {
            if(getSp(getApplicationContext()).getBoolean("first", true)){ // called also after crash - ok
                db.execSQL("CREATE TABLE IF NOT EXISTS "+ DbHelper.TABLE_NAME +" (id INTEGER primary key, name TEXT, color INTEGER, updated INTEGER)");
                getSp(getApplicationContext()).edit().putBoolean("first", false).apply();
            }
        } catch (Exception e){
            e.printStackTrace();
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

    public static int dpToPx(final Context context, final int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources().getDisplayMetrics());
    }

    public static int pxToDp(final int px, final Context context){
        return px / (context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static void hideKeyboardFrom(final Context context, final View view) {
        if (view == null) return;

        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
