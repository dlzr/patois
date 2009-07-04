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
import java.util.TreeMap;


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
    private TreeMap<Long, Language> mLanguagesCache;

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
        mLanguagesCache = new TreeMap<Long, Language>();
    }

    public void close() {
        mDbHelper.close();
        mLanguagesCache.clear();
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
                Language language = new Language(cursor.getLong(0),
                                                 cursor.getString(1),
                                                 cursor.getString(2));
                mLanguagesCache.put(language.getId(), language);
                languages.add(language);
            }
        } finally {
            cursor.close();
        }

        return languages;
    }

    public Language getLanguage(long id) {
        Language language = mLanguagesCache.get(id);
        if (language != null)
            return language;

        Cursor cursor = mDb.query("languages", new String[] { "code", "name" },
                                  "_id = ?", new String[] { Long.toString(id) },
                                  null, null, null);
        try {
            if (cursor.getCount() != 1)
                return null;

            cursor.moveToFirst();
            language = new Language(id, cursor.getString(0), cursor.getString(1));
            mLanguagesCache.put(id, language);

            return language;
        } finally {
            cursor.close();
        }
    }

    public boolean insertLanguage(Language language) {
        mLanguagesCache.put(language.getId(), language);

        ContentValues values = new ContentValues();
        values.put("code", language.getCode());
        values.put("name", language.getName());

        long id = mDb.insert("languages", null, values);
        language.setId(id);

        return id != -1;
    }

    public boolean updateLanguage(Language language) {
        mLanguagesCache.put(language.getId(), language);

        ContentValues values = new ContentValues();
        values.put("code", language.getCode());
        values.put("name", language.getName());

        return mDb.update("languages", values, "_id = ?",
                          new String[] { language.getIdString() }) == 1;
    }

    public boolean deleteLanguage(Language language) {
        mLanguagesCache.remove(language.getId());

        // TODO: Add triggers that delete all the words in the language being
        // deleted.  Presumably, the user has already confirmed that this is
        // what he wants.

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
        Cursor cursor = mDb.query("words", new String[] { "name", "language_id" },
                                  "_id = ?", new String[] { Long.toString(id) },
                                  null, null, null);
        try {
            if (cursor.getCount() != 1)
                return null;

            cursor.moveToFirst();
            return new Word(id, cursor.getString(0), getLanguage(cursor.getLong(1)));
        } finally {
            cursor.close();
        }
    }

    public ArrayList<Word> getTranslations(Word word) {
        ArrayList<Word> translations = new ArrayList<Word>();

        Cursor cursor = mDb.query("translations", new String[] { "word_id2" },
                                  "word_id1 = ?", new String[] { word.getIdString() },
                                  null, null, null);
        try {
            while (cursor.moveToNext()) {
                translations.add(getWord(cursor.getLong(0)));
            }
        } finally {
            cursor.close();
        }

        return translations;
    }

    public boolean insertWord(Word word) {
        ContentValues values = new ContentValues();
        values.put("name", word.getName());
        values.put("language_id", word.getLanguage().getId());

        long id = mDb.insert("words", null, values);
        word.setId(id);

        return id != -1;
    }

    public boolean updateWord(Word word) {
        ContentValues values = new ContentValues();
        values.put("name", word.getName());
        values.put("language_id", word.getLanguage().getId());

        return mDb.update("words", values, "_id = ?",
                          new String[] { word.getIdString() }) == 1;
    }

    public boolean deleteWord(Word word) {
        // TODO: Add triggers that delete the translations referring to the
        // word being deleted.
        return mDb.delete("words", "_id = ?",
                          new String[] { word.getIdString() }) == 1;
    }
}
