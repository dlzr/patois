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


public class EditLanguagesActivity extends Activity {
    private final static String TAG = "EditLanguagesActivity";

    private PatoisDatabase mDb;

    private LinearLayout mLayout;
    private LayoutInflater mInflater;
    private View mAddButton;
    private View mDoneButton;
    private View mCancelButton;

    // These fields are saved accross restarts.
    private ArrayList<LanguageEntry> mLanguages;
    private boolean mAddButtonHasFocus;
    private boolean mDoneButtonHasFocus;
    private boolean mCancelButtonHasFocus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_languages);
        mLayout = (LinearLayout) findViewById(R.id.list);
        mInflater = getLayoutInflater();

        mDb = new PatoisDatabase(this);

        if (savedInstanceState != null) {
            loadStateFromBundle(savedInstanceState);
        } else {
            loadStateFromDatabase(mDb);
        }
        buildViews();
    }

    private void loadStateFromDatabase(PatoisDatabase db) {
        ArrayList<LanguageEntry> languages = new ArrayList<LanguageEntry>();
        Cursor cursor = db.getLanguages();

        while (cursor.moveToNext()) {
            languages.add(new LanguageEntry(cursor));
        }

        mLanguages = languages;
    }

    private void loadStateFromBundle(Bundle savedInstanceState) {
        mLanguages = savedInstanceState.getParcelableArrayList("languages");
        mAddButtonHasFocus = savedInstanceState.getBoolean("addButtonHasFocus");
        mDoneButtonHasFocus = savedInstanceState.getBoolean("doneButtonHasFocus");
        mCancelButtonHasFocus = savedInstanceState.getBoolean("cancelButtonHasFocus");
    }

    private void buildViews() {
        LayoutInflater inflater = mInflater;
        LinearLayout layout = mLayout;
        layout.removeAllViews();

        for (LanguageEntry language : mLanguages) {
            if (!language.isDeleted())
                layout.addView(language.buildView(inflater, layout));
        }

        mAddButton = findViewById(R.id.add_language);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addNewLanguage();
            }
        });
        if (mAddButtonHasFocus)
            mAddButton.requestFocus();

        mDoneButton = findViewById(R.id.done);
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSaveAction();
            }
        });
        if (mDoneButtonHasFocus)
            mDoneButton.requestFocus();

        mCancelButton = findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doRevertAction();
            }
        });
        if (mCancelButtonHasFocus)
            mCancelButton.requestFocus();
    }

    private void addNewLanguage() {
        LanguageEntry language = new LanguageEntry();
        mLanguages.add(language);
        mLayout.addView(language.buildView(mInflater, mLayout));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        for (LanguageEntry language : mLanguages) {
            language.syncFromView();
        }

        outState.putParcelableArrayList("languages", mLanguages);
        outState.putBoolean("addButtonHasFocus", mAddButton.hasFocus());
        outState.putBoolean("doneButtonHasFocus", mDoneButton.hasFocus());
        outState.putBoolean("cancelButtonHasFocus", mCancelButton.hasFocus());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
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

    private void doSaveAction() {
        for (LanguageEntry language : mLanguages) {
            language.saveToDatabase(mDb);
        }
        finish();
    }

    private void doRevertAction() {
        finish();
    }

    private static class LanguageEntry implements Parcelable {
        // These fields are saved in the parcel.
        private long mId;
        private String mCode;
        private String mName;

        private boolean mModified;
        private boolean mDeleted;
        private int mCodeSelectionStart;
        private int mCodeSelectionEnd;
        private int mNameSelectionStart;
        private int mNameSelectionEnd;
        private boolean mDeleteButtonHasFocus;

        // These fields are NOT saved in the parcel.
        private View mView;
        private EditText mCodeEditText;
        private EditText mNameEditText;
        private View mDeleteButton;

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
            mDeleteButtonHasFocus = false;
        }

        public LanguageEntry() {
            this(-1, "", "");
        }

        public LanguageEntry(Cursor cursor) {
            this(cursor.getLong(PatoisDatabase.LANGUAGE_ID_COLUMN),
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

            mDeleteButton = view.findViewById(R.id.delete_language);
            mDeleteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    markAsDeleted();
                }
            });
            if (mDeleteButtonHasFocus)
                mDeleteButton.requestFocus();

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
            mDeleteButtonHasFocus = mDeleteButton.hasFocus();
        }

        private void markAsDeleted() {
            // TODO: Count the number of words in this language, and if not
            // zero, ask the user to confirm the deletion.
            LinearLayout layout = (LinearLayout) mView.getParent();
            layout.removeView(mView);
            mDeleted = true;
        }

        public boolean isDeleted() {
            return mDeleted;
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
            out.writeInt(mDeleteButtonHasFocus ? 1 : 0);
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
                language.mDeleteButtonHasFocus = in.readInt() == 1;

                return language;
            }

            public LanguageEntry[] newArray(int size) {
                return new LanguageEntry[size];
            }
        };
    }
}
