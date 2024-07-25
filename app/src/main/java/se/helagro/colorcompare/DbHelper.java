package se.helagro.colorcompare;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;

import static android.content.Context.MODE_PRIVATE;

class DbHelper {
    final static String DB_NAME = "colors.db";
    final static String TABLE_NAME = "colors";

    private final static String TAG = "DbHelper";
    private static DbHelper ourInstance;
    private SQLiteDatabase db;

    static DbHelper getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new DbHelper(context);
        }
        return ourInstance;
    }

    private DbHelper(Context context) {
        db = context.openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
    }

    ArrayList<Color> getColors() {
        ArrayList<Color> colors = new ArrayList<>();
        Cursor myCursor = db.rawQuery("select name, color, id from " + TABLE_NAME + " ORDER BY updated DESC;", null);
        while (myCursor.moveToNext()) {
            Color color = new Color(
                    myCursor.getString(0),
                    myCursor.getInt(1),
                    myCursor.getLong(2)
            );
            colors.add(color);
        }
        myCursor.close();

        return colors;
    }

    void updateColor(Color color) {
        final ContentValues cv = new ContentValues();
        cv.put("name", color.name);
        cv.put("color", color.color);
        cv.put("updated", Calendar.getInstance().getTimeInMillis());


        if (db.update(TABLE_NAME, cv, "id=" + color.id, null) == 0) {
            color.id = db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    void delColor(Long id) {
        db.delete(TABLE_NAME, "id=" + id, null);
    }

    void close() {
        db.close();
        db = null;
        ourInstance = null;
    }
}
