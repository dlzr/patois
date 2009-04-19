package ro.undef.patois;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;


/**
 * Activity for editting the list of languages.
 */
public class EditLanguagesActivity extends Activity {
    private final static String TAG = "EditLanguagesActivity";

    private LinearLayout mLayout;
    private LayoutInflater mInflater;

    private PatoisDatabase mDb;
    private ArrayList<LanguageEntry> mLanguages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInflater = getLayoutInflater();
        setContentView(R.layout.edit_languages);

        mDb = new PatoisDatabase(this);
        mDb.open();

        if (savedInstanceState != null) {
            getLanguagesFromBundle(savedInstanceState);
        } else {
            getLanguagesFromDB(mDb);
        }

        mLayout = (LinearLayout) findViewById(R.id.list);
        buildViews();
    }

    private void getLanguagesFromDB(PatoisDatabase db) {
        ArrayList<LanguageEntry> languages = new ArrayList<LanguageEntry>();
        Cursor cursor = db.getLanguages();

        while (cursor.moveToNext()) {
            languages.add(new LanguageEntry(cursor));
        }

        mLanguages = languages;
    }

    private void getLanguagesFromBundle(Bundle savedInstanceState) {
        mLanguages = savedInstanceState.getParcelableArrayList("languages");
    }

    private void buildViews() {
        LayoutInflater inflater = mInflater;
        LinearLayout layout = mLayout;
        layout.removeAllViews();

        for (LanguageEntry language : mLanguages) {
            layout.addView(language.buildView(inflater, layout));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        for (LanguageEntry language : mLanguages) {
            language.syncFromView();
        }

        outState.putParcelableArrayList("languages", mLanguages);
    }

    private static class LanguageEntry implements Parcelable {
        // These fields are saved in the parcel.
        public int id;
        public String code;
        public String name;
        public boolean modified;

        // These fields are NOT saved in the parcel.
        private EditText mCodeEditText;
        private EditText mNameEditText;

        public LanguageEntry(int id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.modified = false;
        }

        public LanguageEntry() {
            this(-1, "", "");
        }

        public LanguageEntry(Cursor cursor) {
            this(cursor.getInt(PatoisDatabase.LANGUAGE_ID_COLUMN),
                 cursor.getString(PatoisDatabase.LANGUAGE_CODE_COLUMN),
                 cursor.getString(PatoisDatabase.LANGUAGE_NAME_COLUMN));
        }

        public View buildView(LayoutInflater inflater, LinearLayout parent) {
            View view = inflater.inflate(R.layout.edit_language_entry, parent, false);

            mCodeEditText = (EditText) view.findViewById(R.id.language_code);
            mNameEditText = (EditText) view.findViewById(R.id.language_name);

            mCodeEditText.setText(code);
            mNameEditText.setText(name);

            view.setTag(this);

            return view;
        }

        public void syncFromView() {
            String new_code = mCodeEditText.getText().toString();
            String new_name = mNameEditText.getText().toString();

            if (new_code != code || new_name != name)
                modified = true;

            code = new_code;
            name = new_name;
        }

        // The Parcelable interface implementation.

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(id);
            out.writeString(code);
            out.writeString(name);
            out.writeInt(modified ? 1 : 0);
        }

        public static final Parcelable.Creator CREATOR
                = new Parcelable.Creator() {

            public LanguageEntry createFromParcel(Parcel in) {
                LanguageEntry language = new LanguageEntry(in.readInt(),
                                                           in.readString(),
                                                           in.readString());
                language.modified = in.readInt() == 1;

                return language;
            }

            public LanguageEntry[] newArray(int size) {
                return new LanguageEntry[size];
            }
        };
    }
}
