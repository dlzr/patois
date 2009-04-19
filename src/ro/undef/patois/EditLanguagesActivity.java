package ro.undef.patois;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;


/**
 * Activity for editting the list of languages.
 */
public class EditLanguagesActivity extends Activity {

    private LinearLayout mLayout;
    private LayoutInflater mInflater;

    private PatoisDatabase mDb;
    private ArrayList<LanguageEntry> mLanguages;

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);

        mInflater = getLayoutInflater();
        setContentView(R.layout.edit_languages);

        mDb = new PatoisDatabase(this);
        mDb.open();

        mLanguages = getLanguagesFromDB(mDb);

        mLayout = (LinearLayout) findViewById(R.id.list);
        buildViews();
    }

    private ArrayList<LanguageEntry> getLanguagesFromDB(PatoisDatabase db) {
        Cursor cursor = db.getLanguages();
        ArrayList<LanguageEntry> languages = new ArrayList<LanguageEntry>();

        while (cursor.moveToNext()) {
            languages.add(new LanguageEntry(cursor));
        }

        return languages;
    }

    private void buildViews() {
        LinearLayout layout = mLayout;
        layout.removeAllViews();

        for (LanguageEntry language : mLanguages) {
            layout.addView(language.buildView(layout));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO: Save the edits made by the user to outState.
    }

    private class LanguageEntry {
        // These fields are saved in the parcel.
        public String code;
        public String name;
        public Integer id;

        // These fields are NOT saved in the parcel.
        public EditText codeEditText;
        public EditText nameEditText;

        public LanguageEntry() {
            code = "";
            name = "";
        }

        public LanguageEntry(int id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        public LanguageEntry(Cursor cursor) {
            this(cursor.getInt(PatoisDatabase.LANGUAGE_ID_COLUMN),
                 cursor.getString(PatoisDatabase.LANGUAGE_CODE_COLUMN),
                 cursor.getString(PatoisDatabase.LANGUAGE_NAME_COLUMN));
        }

        public View buildView(LinearLayout parent) {
            View view = mInflater.inflate(R.layout.edit_language_entry, parent, false);

            codeEditText = (EditText) view.findViewById(R.id.language_code);
            nameEditText = (EditText) view.findViewById(R.id.language_name);

            codeEditText.setText(code);
            nameEditText.setText(name);

            view.setTag(this);

            return view;
        }
    }
}
