package ro.undef.patois;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;


public class PatoisDatabase {
    private final static String TAG = "PatoisDatabase";

    private static final String DATABASE_NAME = "patois.db";
    private static final int DATABASE_VERSION = 1;
    private static final String PREFERENCES_NAME = "patois.prefs";

    private final Activity mActivity;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private SharedPreferences mPrefs;
    private TreeMap<Long, Language> mLanguagesCache;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private Context mCtx;

        DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
            mCtx = ctx;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            for (String statement : readStatementsFromAsset("sql/patois.sql"))
                db.execSQL(statement);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // When we'll have a new version of the database, implement this
            // method.
        }

        private ArrayList<String> readStatementsFromAsset(String fileName) {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(mCtx.getAssets().open(fileName)), 4096);

                try {
                    String eol = System.getProperty("line.separator");
                    ArrayList<String> schema = new ArrayList<String>();
                    StringBuffer statement = new StringBuffer("");
                    String line;

                    while ((line = in.readLine()) != null) {
                        // Ignore comments.
                        if (line.startsWith("--"))
                            continue;

                        // Empty lines terminate statements.
                        if (line.trim().length() == 0) {
                            if (statement.length() != 0)
                                schema.add(statement.toString());
                            statement.setLength(0);
                            continue;
                        }

                        statement.append(line);
                        statement.append(eol);  // readLine() strips the EOL characters.
                    }
                    if (statement.length() != 0)
                        schema.add(statement.toString());

                    return schema;
                } finally {
                    in.close();
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException("Could not read asset file: " + fileName, e);
            }
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


    public static final int LANGUAGES_ID_COLUMN = 0;
    public static final int LANGUAGES_CODE_COLUMN = 1;
    public static final int LANGUAGES_NAME_COLUMN = 2;

    public Cursor getLanguagesCursor() {
        Cursor cursor = mDb.query("languages", new String[] { "_id", "code", "name" },
                                  null, null, null, null, null);
        mActivity.startManagingCursor(cursor);

        return cursor;
    }

    public ArrayList<Language> getLanguages() {
        ArrayList<Language> languages = new ArrayList<Language>();

        Cursor cursor = mDb.query("languages", new String[] { "_id", "code", "name", "num_words" },
                                  null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                Language language = new Language(cursor.getLong(0), cursor.getString(1),
                                                 cursor.getString(2), cursor.getLong(3));
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

        Cursor cursor = mDb.query("languages", new String[] { "code", "name", "num_words" },
                                  "_id = ?", new String[] { Long.toString(id) },
                                  null, null, null);
        try {
            if (cursor.getCount() != 1)
                return null;

            cursor.moveToFirst();
            language = new Language(id, cursor.getString(0), cursor.getString(1),
                                    cursor.getLong(2));
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


    public static final String BROWSE_WORDS_NAME_COLUMN = "name";
    public static final String BROWSE_WORDS_TRANSLATIONS_COLUMN = "translations";

    public Cursor getBrowseWordsCursor(Language language) {
        // TODO: Fix this query to also include words without translations.
        Cursor cursor = mDb.rawQuery(
                "SELECT " +
                "    t.word_id1 AS _id, " +
                "    w1.name AS name, " +
                "    group_concat( " +
                "      w2.name || ' (' || l.code || ') ', " +
                "      ', ') AS translations " +
                "  FROM " +
                "    translations AS t, " +
                "    words AS w1, " +
                "    words AS w2, " +
                "    languages AS l " +
                "  WHERE " +
                "    w1.language_id = ? AND " +
                "    t.word_id1 = w1._id AND " +
                "    t.word_id2 = w2._id AND " +
                "    w2.language_id = l._id " +
                "  GROUP BY (t.word_id1)",
                new String[] { language.getIdString() });
        mActivity.startManagingCursor(cursor);

        return cursor;
    }

    public static final String WORDS_NAME_COLUMN = "name";
    public static final int WORDS_NAME_COLUMN_ID = 1;

    public Cursor getWordsCursor(Language language, String filter) {
        Cursor cursor = mDb.query("words", new String[] { "_id", "name" },
                                  "(language_id = ?) AND (name LIKE ?)",
                                  new String[] {
                                      language.getIdString(),
                                      "%" + filter + "%",
                                  },
                                  null, null, null);
        mActivity.startManagingCursor(cursor);

        return cursor;
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
        return mDb.delete("words", "_id = ?",
                          new String[] { word.getIdString() }) == 1;
    }

    public ArrayList<Word> getTranslations(Word word) {
        ArrayList<Word> translations = new ArrayList<Word>();

        Cursor cursor = mDb.query("translations", new String[] { "word_id2" },
                                  "word_id1 = ?", new String[] { word.getIdString() },
                                  null, null, null);
        try {
            while (cursor.moveToNext())
                translations.add(getWord(cursor.getLong(0)));
        } finally {
            cursor.close();
        }

        return translations;
    }

    public void insertTranslation(Word word1, Word word2) {
        ContentValues values = new ContentValues();

        values.put("word_id1", word1.getId());
        values.put("word_id2", word2.getId());
        mDb.insert("translations", null, values);

        values.clear();
        values.put("word_id1", word2.getId());
        values.put("word_id2", word1.getId());
        mDb.insert("translations", null, values);
    }

    public void deleteTranslation(Word word1, Word word2) {
        mDb.delete("translations",
                   "(word_id1 = ? AND word_id2 = ?) OR (word_id1 = ? AND word_id2 = ?)",
                   new String[] {
                       word1.getIdString(),
                       word2.getIdString(),
                       word2.getIdString(),
                       word1.getIdString(),
                   });
    }
}
