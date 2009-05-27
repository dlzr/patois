package ro.undef.patois;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;


public class PatoisDatabase {

    private static final String DATABASE_NAME = "patois.db";
    private static final int DATABASE_VERSION = 1;
    private static final String[] DATABASE_SCHEMA = new String[] {
        "CREATE TABLE languages ( " +
        "    _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "    code TEXT NOT NULL, " +
        "    name TEXT NOT NULL " +
        ");",

        "CREATE TABLE words ( " +
        "    _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "    name TEXT NOT NULL, " +
        "    language_id INTEGER NOT NULL " +
        ");",

        "CREATE TABLE translations ( " +
        "    word_id1 INTEGER NOT NULL, " +
        "    word_id2 INTEGER NOT NULL " +
        ");",
    };

    private final Activity mActivity;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            for (String statement : DATABASE_SCHEMA) {
                db.execSQL(statement);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO: When we'll have a new version of the database, implement
            // this method.
        }
    }

    PatoisDatabase(Activity activity) {
        this.mActivity = activity;
    }

    public void open() throws SQLException {
        mDbHelper = new DatabaseHelper(mActivity);
        mDb = mDbHelper.getWritableDatabase();
    }

    public void close() {
        mDbHelper.close();
    }

    public static final int LANGUAGE_ID_COLUMN = 0;
    public static final int LANGUAGE_CODE_COLUMN = 1;
    public static final int LANGUAGE_NAME_COLUMN = 2;

    public Cursor getLanguages() {
        Cursor cursor = mDb.query("languages", new String[] { "_id", "code", "name" },
                null, null, null, null, null);
        mActivity.startManagingCursor(cursor);

        return cursor;
    }

    public long insertLanguage(String code, String name) {
        ContentValues values = new ContentValues();
        values.put("code", code);
        values.put("name", name);

        return mDb.insert("languages", null, values);
    }

    public boolean updateLanguage(long id, String code, String name) {
        ContentValues values = new ContentValues();
        values.put("code", code);
        values.put("name", name);

        return mDb.update("languages", values, "_id = ?",
                          new String[] { Long.toString(id) }) == 1;
    }

    public boolean deleteLanguage(long id) {
        return mDb.delete("languages", "_id = ?",
                          new String[] { Long.toString(id) }) == 1;
    }
}
