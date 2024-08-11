package se.helagro.colorcompare;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;

public class DbHelper {
    final static String DB_NAME = "colors.db";
    final static String TABLE_NAME = "colors";

    private final static String TAG = "DbHelper";
    private static DbHelper ourInstance;
    private SQLiteDatabase db;

    private DbHelper(final Context context) {
        db = context.openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
    }

    public static DbHelper getInstance(final Context context) {
        if (ourInstance == null)
            ourInstance = new DbHelper(context);
        return ourInstance;
    }

    ArrayList<Color> getColors() {
        final ArrayList<Color> colors = new ArrayList<>();
        final Cursor myCursor = db.rawQuery(
                "select name, color, id from " + TABLE_NAME + " ORDER BY updated DESC;",
                null
        );
        while (myCursor.moveToNext()) {
            colors.add(new Color(
                    myCursor.getString(0),
                    myCursor.getInt(1),
                    myCursor.getLong(2)
            ));
        }
        myCursor.close();

        return colors;
    }

    public void updateColor(final Color color) {
        final ContentValues cv = new ContentValues();
        cv.put("name", color.name);
        cv.put("color", color.color);
        cv.put("updated", Calendar.getInstance().getTimeInMillis());

        if (db.update(TABLE_NAME, cv, "id=" + color.id, null) == 0)
            color.id = db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void delColor(final Long id) {
        db.delete(TABLE_NAME, "id=" + id, null);
    }

    void close() {
        db.close();
        db = null;
        ourInstance = null;
    }
}
