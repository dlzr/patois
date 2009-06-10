package ro.undef.patois;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.io.Serializable;
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
    private ArrayList<LanguageEntry> mLanguageEntries;
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
        ArrayList<LanguageEntry> entries = new ArrayList<LanguageEntry>();

        for (Language language : db.getLanguages()) {
            entries.add(new LanguageEntry(language));
        }

        mLanguageEntries = entries;
    }

    @SuppressWarnings("unchecked")
    private void loadStateFromBundle(Bundle savedInstanceState) {
        mLanguageEntries = (ArrayList<LanguageEntry>)
            savedInstanceState.getSerializable("languages");
        mAddButtonHasFocus = savedInstanceState.getBoolean("addButtonHasFocus");
        mDoneButtonHasFocus = savedInstanceState.getBoolean("doneButtonHasFocus");
        mCancelButtonHasFocus = savedInstanceState.getBoolean("cancelButtonHasFocus");
    }

    private void buildViews() {
        LayoutInflater inflater = mInflater;
        LinearLayout layout = mLayout;
        layout.removeAllViews();

        for (LanguageEntry entry : mLanguageEntries) {
            if (!entry.isDeleted())
                layout.addView(entry.buildView(inflater, layout));
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
        LanguageEntry entry = new LanguageEntry();
        mLanguageEntries.add(entry);
        mLayout.addView(entry.buildView(mInflater, mLayout));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        for (LanguageEntry entry : mLanguageEntries) {
            entry.syncFromView();
        }

        outState.putSerializable("languages", mLanguageEntries);
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
        for (LanguageEntry entry : mLanguageEntries) {
            entry.saveToDatabase(mDb);
        }
        finish();
    }

    private void doRevertAction() {
        finish();
    }

    private static class LanguageEntry implements Serializable {
        private Language mLanguage;
        private boolean mModified;
        private boolean mDeleted;
        private int mCodeSelectionStart;
        private int mCodeSelectionEnd;
        private int mNameSelectionStart;
        private int mNameSelectionEnd;
        private boolean mDeleteButtonHasFocus;

        transient private View mView;
        transient private EditText mCodeEditText;
        transient private EditText mNameEditText;
        transient private View mDeleteButton;

        public LanguageEntry(Language language) {
            mLanguage = language;
            mModified = false;
            mDeleted = false;
            mCodeSelectionStart = -1;
            mCodeSelectionStart = -1;
            mNameSelectionEnd = -1;
            mNameSelectionEnd = -1;
            mDeleteButtonHasFocus = false;
        }

        public LanguageEntry() {
            this(new Language());
        }

        public View buildView(LayoutInflater inflater, LinearLayout parent) {
            View view = inflater.inflate(R.layout.edit_language_entry, parent, false);
            mView = view;

            mCodeEditText = (EditText) view.findViewById(R.id.language_code);
            mCodeEditText.setText(mLanguage.getCode());
            if (mCodeSelectionStart != -1 && mCodeSelectionEnd != -1) {
                mCodeEditText.requestFocus();
                mCodeEditText.setSelection(mCodeSelectionStart, mCodeSelectionEnd);
            }

            mNameEditText = (EditText) view.findViewById(R.id.language_name);
            mNameEditText.setText(mLanguage.getName());
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

            if (new_code != mLanguage.getCode() || new_name != mLanguage.getName())
                mModified = true;

            mLanguage.setCode(new_code);
            mLanguage.setName(new_name);

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

            if (mLanguage.notInDatabase() && !mDeleted) {
                db.insertLanguage(mLanguage);
            } else if (mModified && !mDeleted) {
                db.updateLanguage(mLanguage);
            } else if (mDeleted) {
                db.deleteLanguage(mLanguage);
            }
        }

        // Required for the Serializable interface.
        static final long serialVersionUID = 972042590665827295L;
    }
}
