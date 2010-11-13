/*
 * Copyright 2010 David Lazar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ro.undef.patois;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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

    private static final int CONFIRM_DELETE_DIALOG = 1;

    private Database mDb;

    private LinearLayout mLanguagesLayout;
    private LayoutInflater mInflater;
    private View mAddButton;
    private View mDoneButton;
    private View mCancelButton;
    private LanguageEntry mEntryToDelete;

    // These fields are saved across restarts.
    private ArrayList<LanguageEntry> mLanguageEntries;
    private boolean mAddButtonHasFocus;
    private boolean mDoneButtonHasFocus;
    private boolean mCancelButtonHasFocus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new Database(this);

        if (savedInstanceState != null) {
            loadStateFromBundle(savedInstanceState);
        } else {
            loadStateFromDatabase();
        }

        setupViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
    }

    private void loadStateFromDatabase() {
        ArrayList<LanguageEntry> entries = new ArrayList<LanguageEntry>();
        for (Language language : mDb.getLanguages())
            entries.add(new LanguageEntry(language));
        mLanguageEntries = entries;

        mEntryToDelete = null;

        mAddButtonHasFocus = false;
        mDoneButtonHasFocus = false;
        mCancelButtonHasFocus = false;
    }

    private void saveStateToDatabase() {
        for (LanguageEntry entry : mLanguageEntries) {
            entry.saveToDatabase(mDb);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadStateFromBundle(Bundle savedInstanceState) {
        mLanguageEntries = (ArrayList<LanguageEntry>)
            savedInstanceState.getSerializable("languages");

        mEntryToDelete = null;

        mAddButtonHasFocus = savedInstanceState.getBoolean("add_language");
        mDoneButtonHasFocus = savedInstanceState.getBoolean("done");
        mCancelButtonHasFocus = savedInstanceState.getBoolean("cancel");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        for (LanguageEntry entry : mLanguageEntries)
            entry.syncFromView();
        outState.putSerializable("languages", mLanguageEntries);

        outState.putBoolean("add_language", mAddButton.hasFocus());
        outState.putBoolean("done", mDoneButton.hasFocus());
        outState.putBoolean("cancel", mCancelButton.hasFocus());
    }

    private void setupViews() {
        setContentView(R.layout.edit_languages);

        LayoutInflater inflater = mInflater = getLayoutInflater();
        LinearLayout layout = mLanguagesLayout = (LinearLayout) findViewById(R.id.languages);
        layout.removeAllViews();
        for (LanguageEntry entry : mLanguageEntries) {
            entry.addViewToList(this, layout, inflater);
            if (entry.hasConfirmDeleteDialogOpen())
                mEntryToDelete = entry;
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
                saveStateToDatabase();
                finish();
            }
        });
        if (mDoneButtonHasFocus)
            mDoneButton.requestFocus();

        mCancelButton = findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        if (mCancelButtonHasFocus)
            mCancelButton.requestFocus();
    }

    private void addNewLanguage() {
        LanguageEntry entry = new LanguageEntry();
        mLanguageEntries.add(entry);
        entry.addViewToList(this, mLanguagesLayout, mInflater);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                saveStateToDatabase();
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showConfirmDeleteDialog(LanguageEntry entryToDelete) {
        mEntryToDelete = entryToDelete;
        showDialog(CONFIRM_DELETE_DIALOG);
    }

    private void cancelConfirmDeleteDialog() {
        // We don't want the activity to cache this dialog.
        // See above for details.
        removeDialog(CONFIRM_DELETE_DIALOG);
        mEntryToDelete.cancelDelete();
        mEntryToDelete = null;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CONFIRM_DELETE_DIALOG: {
                return new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_delete)
                    .setMessage(String.format(getResources().getString(R.string.language_not_empty),
                                              mEntryToDelete.getLanguage().getNumWords(),
                                              mEntryToDelete.getLanguage().getName()))
                    .setPositiveButton(R.string.yes,
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // We don't want the activity to cache this dialog
                            // so we call removeDialog() explicitly.  If this
                            // dialog were to be cached, the activity would
                            // call onCreateDialog() when resuming from
                            // configuration changes, and mEntryToDelete could
                            // be null at that point, leading to a
                            // NullPointerException in the setup code above.
                            removeDialog(CONFIRM_DELETE_DIALOG);
                            mEntryToDelete.markAsDeleted();
                            mEntryToDelete = null;
                        }
                    })
                    .setNegativeButton(R.string.no,
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            cancelConfirmDeleteDialog();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            cancelConfirmDeleteDialog();
                        }
                    })
                    .create();
            }
        }
        return null;
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
        private boolean mHasConfirmDeleteDialogOpen;

        transient private EditLanguagesActivity mActivity;
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
            mHasConfirmDeleteDialogOpen = false;
        }

        public LanguageEntry() {
            this(new Language());
        }

        public Language getLanguage() {
            return mLanguage;
        }

        public boolean hasConfirmDeleteDialogOpen() {
            return mHasConfirmDeleteDialogOpen;
        }

        public void addViewToList(final EditLanguagesActivity activity,
                                  LinearLayout parent, LayoutInflater inflater) {
            if (mDeleted)
                return;

            View view = inflater.inflate(R.layout.edit_language_entry, parent, false);
            setupView(activity, view);
            parent.addView(view);
        }

        private void setupView(final EditLanguagesActivity activity, View view) {
            mActivity = activity;
            mView = view;

            mCodeEditText = (EditText) view.findViewById(R.id.code);
            mCodeEditText.setText(mLanguage.getCode());
            if (mCodeSelectionStart != -1 && mCodeSelectionEnd != -1) {
                mCodeEditText.requestFocus();
                mCodeEditText.setSelection(mCodeSelectionStart, mCodeSelectionEnd);
            }

            mNameEditText = (EditText) view.findViewById(R.id.name);
            mNameEditText.setText(mLanguage.getName());
            if (mNameSelectionStart != -1 && mNameSelectionEnd != -1) {
                mNameEditText.requestFocus();
                mNameEditText.setSelection(mNameSelectionStart, mNameSelectionEnd);
            }

            mDeleteButton = view.findViewById(R.id.delete);
            mDeleteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mLanguage.getNumWords() > 0) {
                        mHasConfirmDeleteDialogOpen = true;
                        mActivity.showConfirmDeleteDialog(LanguageEntry.this);
                    } else {
                        markAsDeleted();
                    }
                }
            });
            if (mDeleteButtonHasFocus)
                mDeleteButton.requestFocus();
        }

        public void syncFromView() {
            if (mDeleted)
                return;

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

        public void markAsDeleted() {
            LinearLayout parent = (LinearLayout) mView.getParent();
            parent.removeView(mView);
            mDeleted = true;
            mHasConfirmDeleteDialogOpen = false;
        }

        public void cancelDelete() {
            mHasConfirmDeleteDialogOpen = false;
        }

        public void saveToDatabase(Database db) {
            syncFromView();

            if (!mDeleted) {
                if (mLanguage.isInDatabase()) {
                    if (mModified)
                        db.updateLanguage(mLanguage);
                } else {
                    if (!mLanguage.isEmpty())
                        db.insertLanguage(mLanguage);
                }
            } else {
                if (mLanguage.isInDatabase())
                    db.deleteLanguage(mLanguage);
            }
        }

        // Required for the Serializable interface.
        static final long serialVersionUID = 972042590665827295L;
    }
}
