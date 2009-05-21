package ro.undef.patois;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;


/**
 * Activity for editting the list of languages.
 */
public class EditLanguagesActivity extends Activity implements View.OnClickListener {
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
        mLayout = (LinearLayout) findViewById(R.id.list);

        mDb = new PatoisDatabase(this);
        mDb.open();

        if (savedInstanceState != null) {
            loadLanguagesFromBundle(savedInstanceState);
        } else {
            loadLanguagesFromDatabase(mDb);
        }
        buildViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        for (LanguageEntry language : mLanguages) {
            language.syncFromView();
        }

        outState.putParcelableArrayList("languages", mLanguages);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                doSaveAction();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_language: {
                addNewLanguage();
                break;
            }
            case R.id.done: {
                doSaveAction();
                break;
            }
            case R.id.revert: {
                doRevertAction();
                break;
            }
        }
    }

    private void loadLanguagesFromDatabase(PatoisDatabase db) {
        ArrayList<LanguageEntry> languages = new ArrayList<LanguageEntry>();
        Cursor cursor = db.getLanguages();

        while (cursor.moveToNext()) {
            languages.add(new LanguageEntry(cursor));
        }

        mLanguages = languages;
    }

    private void loadLanguagesFromBundle(Bundle savedInstanceState) {
        mLanguages = savedInstanceState.getParcelableArrayList("languages");
    }

    private void addNewLanguage() {
        LanguageEntry language = new LanguageEntry();
        mLanguages.add(language);
        mLayout.addView(language.buildView(mInflater, mLayout));
    }

    private void buildViews() {
        LayoutInflater inflater = mInflater;
        LinearLayout layout = mLayout;
        layout.removeAllViews();

        for (LanguageEntry language : mLanguages) {
            if (!language.isDeleted())
                layout.addView(language.buildView(inflater, layout));
        }

        View view = findViewById(R.id.add_language);
        view.setOnClickListener(this);
        view = findViewById(R.id.done);
        view.setOnClickListener(this);
        view = findViewById(R.id.revert);
        view.setOnClickListener(this);
    }

    private void doSaveAction() {
        for (LanguageEntry language : mLanguages) {
            language.saveToDatabase(mDb);
        }
        finish();
    }

    private void doRevertAction() {
        finish();
    }

    private static class LanguageEntry implements Parcelable, View.OnClickListener {
        // These fields are saved in the parcel.

        /**
         * ID number of the the language row in the database.
         */
        private long mId;

        /**
         * Short code for the language (e.g., "EN" for "English").
         */
        private String mCode;

        /**
         * Language name (e.g., "English").
         */
        private String mName;

        /**
         * True if the data in the LanguageEntry is different from what's
         * stored in the database.
         */
        private boolean mModified;

        /**
         * True if the user pressed the 'delete' button on this LanguageEntry.
         */
        private boolean mDeleted;
        public boolean isDeleted() {
            return mDeleted;
        }

        /**
         * Position of the beginning of the selection in the "code" EditText
         * (-1 if there is no selection).
         */
        private int mCodeSelectionStart;

        /**
         * Position of the end of the selection in the "code" EditText
         * (-1 if there is no selection).
         */
        private int mCodeSelectionEnd;

        /**
         * Position of the beginning of the selection in the "name" EditText
         * (-1 if there is no selection).
         */
        private int mNameSelectionStart;

        /**
         * Position of the end of the selection in the "name" EditText
         * (-1 if there is no selection).
         */
        private int mNameSelectionEnd;

        // These fields are NOT saved in the parcel.
        private View mView;
        private EditText mCodeEditText;
        private EditText mNameEditText;

        public LanguageEntry(long id, String code, String name) {
            mId = id;
            mCode = code;
            mName = name;
            mModified = false;
            mDeleted = false;
            mCodeSelectionStart = -1;
            mCodeSelectionStart = -1;
            mNameSelectionEnd = -1;
            mNameSelectionEnd = -1;
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
            mView = view;

            mCodeEditText = (EditText) view.findViewById(R.id.language_code);
            mNameEditText = (EditText) view.findViewById(R.id.language_name);

            mCodeEditText.setText(mCode);
            mNameEditText.setText(mName);

            if (mCodeSelectionStart != -1 && mCodeSelectionEnd != -1) {
                mCodeEditText.requestFocus();
                mCodeEditText.setSelection(mCodeSelectionStart, mCodeSelectionEnd);
            }
            if (mNameSelectionStart != -1 && mNameSelectionEnd != -1) {
                mNameEditText.requestFocus();
                mNameEditText.setSelection(mNameSelectionStart, mNameSelectionEnd);
            }

            View deleteButton = view.findViewById(R.id.delete_language);
            deleteButton.setOnClickListener(this);

            return view;
        }

        public void syncFromView() {
            String new_code = mCodeEditText.getText().toString();
            String new_name = mNameEditText.getText().toString();

            if (new_code != mCode || new_name != mName)
                mModified = true;

            mCode = new_code;
            mName = new_name;

            if (mCodeEditText.hasFocus()) {
                mCodeSelectionStart = mCodeEditText.getSelectionStart();
                mCodeSelectionEnd = mCodeEditText.getSelectionEnd();
            } else {
                mCodeSelectionStart = -1;
                mCodeSelectionEnd = -1;
            }
            if (mNameEditText.hasFocus()) {
                mNameSelectionStart = mNameEditText.getSelectionStart();
                mNameSelectionEnd = mNameEditText.getSelectionEnd();
            } else {
                mNameSelectionStart = -1;
                mNameSelectionEnd = -1;
            }
        }

        private void markAsDeleted() {
            // TODO: Count the number of words in this language, and if not
            // zero, ask the user to confirm the deletion.
            LinearLayout layout = (LinearLayout) mView.getParent();
            layout.removeView(mView);
            mDeleted = true;
        }

        public void saveToDatabase(PatoisDatabase db) {
            syncFromView();

            if (mId == -1 && !mDeleted) {
                mId = db.insertLanguage(mCode, mName);
            } else if (mModified && !mDeleted) {
                db.updateLanguage(mId, mCode, mName);
            } else if (mDeleted) {
                db.deleteLanguage(mId);
            }
        }

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.delete_language: {
                    markAsDeleted();
                    break;
                }
            }
        }

        // The Parcelable interface implementation.

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeLong(mId);
            out.writeString(mCode);
            out.writeString(mName);
            out.writeInt(mModified ? 1 : 0);
            out.writeInt(mDeleted ? 1 : 0);
            out.writeInt(mCodeSelectionStart);
            out.writeInt(mCodeSelectionEnd);
            out.writeInt(mNameSelectionStart);
            out.writeInt(mNameSelectionEnd);
        }

        public static final Parcelable.Creator CREATOR
                = new Parcelable.Creator() {

            public LanguageEntry createFromParcel(Parcel in) {
                LanguageEntry language = new LanguageEntry(in.readLong(),
                                                           in.readString(),
                                                           in.readString());
                language.mModified = in.readInt() == 1;
                language.mDeleted = in.readInt() == 1;

                language.mCodeSelectionStart = in.readInt();
                language.mCodeSelectionEnd = in.readInt();
                language.mNameSelectionStart = in.readInt();
                language.mNameSelectionEnd = in.readInt();

                return language;
            }

            public LanguageEntry[] newArray(int size) {
                return new LanguageEntry[size];
            }
        };
    }
}
