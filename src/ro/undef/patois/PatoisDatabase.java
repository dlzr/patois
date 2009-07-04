package ro.undef.patois;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
    private static final String PREFERENCES_NAME = "patois.prefs";

    private final Activity mActivity;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private SharedPreferences mPrefs;

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
        mActivity = activity;
        mDbHelper = new DatabaseHelper(mActivity);
        mDb = mDbHelper.getWritableDatabase();
        mPrefs = mActivity.getSharedPreferences(PREFERENCES_NAME, 0);
    }

    public void close() {
        mDbHelper.close();
    }


    public static final int LANGUAGE_ID_COLUMN = 0;
    public static final int LANGUAGE_CODE_COLUMN = 1;
    public static final int LANGUAGE_NAME_COLUMN = 2;

    public Cursor getLanguagesCursor() {
        Cursor cursor = mDb.query("languages", new String[] { "_id", "code", "name" },
                null, null, null, null, null);
        mActivity.startManagingCursor(cursor);

        return cursor;
    }

    public ArrayList<Language> getLanguages() {
        ArrayList<Language> languages = new ArrayList<Language>();

        Cursor cursor = mDb.query("languages", new String[] { "_id", "code", "name" },
                null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                languages.add(new Language(cursor.getLong(0),
                                           cursor.getString(1),
                                           cursor.getString(2)));
            }
        } finally {
            cursor.close();
        }

        return languages;
    }

    public Language getLanguage(long id) {
        // TODO: Cache languages in memory.

        Cursor cursor = mDb.query("languages", new String[] { "_id", "code", "name" },
                                  "_id = ?", new String[] { Long.toString(id) },
                                  null, null, null);
        try {
            if (cursor.getCount() != 1)
                return null;
            cursor.moveToFirst();
            return new Language(cursor.getLong(0),
                                cursor.getString(1),
                                cursor.getString(2));
        } finally {
            cursor.close();
        }
    }

    public boolean insertLanguage(Language language) {
        ContentValues values = new ContentValues();
        values.put("code", language.getCode());
        values.put("name", language.getName());

        long id = mDb.insert("languages", null, values);
        language.setId(id);

        return id != -1;
    }

    public boolean updateLanguage(Language language) {
        ContentValues values = new ContentValues();
        values.put("code", language.getCode());
        values.put("name", language.getName());

        return mDb.update("languages", values, "_id = ?",
                          new String[] { language.getIdString() }) == 1;
    }

    public boolean deleteLanguage(Language language) {
        return mDb.delete("languages", "_id = ?",
                          new String[] { language.getIdString() }) == 1;
    }

    private static final String ACTIVE_LANGUAGE_PREF = "active_language";

    public Language getActiveLanguage() {
        long id = mPrefs.getLong(ACTIVE_LANGUAGE_PREF, -1);
        if (id == -1)
            return null;

        return getLanguage(id);
    }

    public void setActiveLanguageId(long id) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putLong(ACTIVE_LANGUAGE_PREF, id);
        editor.commit();
    }


    public Word getWord(long id) {
        // TODO: Implement me.
        return null;
    }

    public ArrayList<Word> getTranslations(Word word) {
        // TODO: Implement me.
        return null;
    }

    public boolean insertWord(Word word) {
        // TODO: Implement me.
        return true;
    }

    public boolean updateWord(Word word) {
        // TODO: Implement me.
        return true;
    }

    public boolean deleteWord(Word word) {
        // TODO: Implement me.
        return true;
    }
}
