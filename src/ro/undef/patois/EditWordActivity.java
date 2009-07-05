package ro.undef.patois;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.io.Serializable;
import java.util.ArrayList;


public class EditWordActivity extends Activity {
    private final static String TAG = "EditWordActivity";

    private PatoisDatabase mDb;

    private LinearLayout mTranslationsLayout;
    private LayoutInflater mInflater;
    private View mAddButton;
    private View mDoneButton;
    private View mNewWordButton;
    private View mCancelButton;

    // These fields are saved accross restarts.
    private WordEntry mMainWordEntry;
    private ArrayList<TranslationEntry> mTranslationEntries;
    private boolean mAddButtonHasFocus;
    private boolean mDoneButtonHasFocus;
    private boolean mNewWordButtonHasFocus;
    private boolean mCancelButtonHasFocus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new PatoisDatabase(this);

        Intent intent = getIntent();
        if (savedInstanceState != null) {
            loadStateFromBundle(savedInstanceState);
        } else if (intent.getAction() == Intent.ACTION_EDIT) {
            loadStateFromDatabase(intent.getExtras().getLong("word_id"));
        } else {
            resetState();
        }

        setContentView(R.layout.edit_word);
        setupViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
    }

    private void resetState() {
        mMainWordEntry = new WordEntry(mDb.getActiveLanguage());

        mTranslationEntries = new ArrayList<TranslationEntry>();
        mTranslationEntries.add(new TranslationEntry(pickTranslationLanguage()));

        mAddButtonHasFocus = false;
        mDoneButtonHasFocus = false;
        mNewWordButtonHasFocus = false;
        mCancelButtonHasFocus = false;
    }

    private void loadStateFromDatabase(long wordId) {
        Word mainWord = mDb.getWord(wordId);
        mMainWordEntry = new WordEntry(mainWord);

        ArrayList<TranslationEntry> entries = new ArrayList<TranslationEntry>();
        for (Word word : mDb.getTranslations(mainWord))
            entries.add(new TranslationEntry(word));
        mTranslationEntries = entries;

        mAddButtonHasFocus = false;
        mDoneButtonHasFocus = false;
        mNewWordButtonHasFocus = false;
        mCancelButtonHasFocus = false;
    }

    private void saveStateToDatabase() {
        mMainWordEntry.saveToDatabase(mDb);

        for (TranslationEntry entry : mTranslationEntries)
            entry.saveToDatabase(mDb);
    }

    @SuppressWarnings("unchecked")
    private void loadStateFromBundle(Bundle savedInstanceState) {
        mMainWordEntry = (WordEntry) savedInstanceState.getSerializable("main_word");
        mTranslationEntries = (ArrayList<TranslationEntry>)
            savedInstanceState.getSerializable("translations");
        mAddButtonHasFocus = savedInstanceState.getBoolean("add_translation");
        mDoneButtonHasFocus = savedInstanceState.getBoolean("done");
        mNewWordButtonHasFocus = savedInstanceState.getBoolean("new_word");
        mCancelButtonHasFocus = savedInstanceState.getBoolean("cancel");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mMainWordEntry.syncFromView();
        outState.putSerializable("main_word", mMainWordEntry);

        for (TranslationEntry entry : mTranslationEntries)
            entry.syncFromView();
        outState.putSerializable("translations", mTranslationEntries);

        outState.putBoolean("add_translation", mAddButton.hasFocus());
        outState.putBoolean("done", mDoneButton.hasFocus());
        outState.putBoolean("new_word", mNewWordButton.hasFocus());
        outState.putBoolean("cancel", mCancelButton.hasFocus());
    }

    private void setupViews() {
        mMainWordEntry.setupView(findViewById(R.id.main_word));

        LayoutInflater inflater = mInflater = getLayoutInflater();
        LinearLayout layout = mTranslationsLayout = (LinearLayout) findViewById(R.id.translations);
        layout.removeAllViews();
        for (TranslationEntry entry : mTranslationEntries)
            entry.addViewToList(layout, inflater);

        mAddButton = findViewById(R.id.add_translation);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addNewTranslation();
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

        mNewWordButton = findViewById(R.id.new_word);
        mNewWordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveStateToDatabase();
                resetState();
                setupViews();
            }
        });
        if (mNewWordButtonHasFocus)
            mNewWordButton.requestFocus();

        mCancelButton = findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        if (mCancelButtonHasFocus)
            mCancelButton.requestFocus();
    }

    private void addNewTranslation() {
        TranslationEntry entry = new TranslationEntry(pickTranslationLanguage());
        mTranslationEntries.add(entry);
        entry.addViewToList(mTranslationsLayout, mInflater);
    }

    private Language pickTranslationLanguage() {
        // TODO: Choose the language with most words that is not already
        // present in mTranslationEntries.
        return mDb.getActiveLanguage();
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


    private static class WordEntry implements Serializable {
        protected Word mWord;
        protected boolean mModified;
        protected int mNameSelectionStart;
        protected int mNameSelectionEnd;
        protected boolean mLanguageButtonHasFocus;

        transient protected Button mLanguageButton;
        transient protected EditText mNameEditText;

        public WordEntry(Word word) {
            mWord = word;
            mModified = false;
            mNameSelectionStart = -1;
            mNameSelectionEnd = -1;
            mLanguageButtonHasFocus = false;
        }

        public WordEntry(Language language) {
            this(new Word(language));
        }

        public void setupView(View view) {
            mLanguageButton = (Button) view.findViewById(R.id.language);
            mLanguageButton.setText(mWord.getLanguage().getCode());
            mLanguageButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // TODO: Show language selector.
                }
            });
            if (mLanguageButtonHasFocus)
                mLanguageButton.requestFocus();

            mNameEditText = (EditText) view.findViewById(R.id.name);
            mNameEditText.setText(mWord.getName());
            if (mNameSelectionStart != -1 && mNameSelectionEnd != -1) {
                mNameEditText.requestFocus();
                mNameEditText.setSelection(mNameSelectionStart, mNameSelectionEnd);
            }
        }

        public void syncFromView() {
            String new_name = mNameEditText.getText().toString();

            if (new_name != mWord.getName())
                mModified = true;

            mWord.setName(new_name);

            if (mNameEditText.hasFocus()) {
                mNameSelectionStart = mNameEditText.getSelectionStart();
                mNameSelectionEnd = mNameEditText.getSelectionEnd();
            } else {
                mNameSelectionStart = -1;
                mNameSelectionEnd = -1;
            }
            mLanguageButtonHasFocus = mLanguageButton.hasFocus();
        }

        public void saveToDatabase(PatoisDatabase db) {
            syncFromView();

            if (mWord.notInDatabase()) {
                if (mWord.getName().length() != 0)
                    db.insertWord(mWord);
            } else if (mModified) {
                if (mWord.getName().length() != 0)
                    db.updateWord(mWord);
                else
                    db.deleteWord(mWord);
            }
        }

        // Required for the Serializable interface.
        static final long serialVersionUID = 506921538917961504L;
    }


    private static class TranslationEntry extends WordEntry {
        private boolean mDeleted;
        private boolean mDeleteButtonHasFocus;

        transient private View mView;
        transient private View mDeleteButton;

        public TranslationEntry(Word word) {
            super(word);
            mDeleted = false;
            mDeleteButtonHasFocus = false;
        }

        public TranslationEntry(Language language) {
            this(new Word(language));
        }

        public void addViewToList(LinearLayout parent, LayoutInflater inflater) {
            if (mDeleted)
                return;

            View view = inflater.inflate(R.layout.edit_word_entry, parent, false);
            setupView(view);
            parent.addView(view);
        }

        public void setupView(View view) {
            super.setupView(view);

            mView = view;

            mDeleteButton = view.findViewById(R.id.delete);
            mDeleteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    markAsDeleted();
                }
            });
            if (mDeleteButtonHasFocus)
                mDeleteButton.requestFocus();
        }

        public void syncFromView() {
            if (mDeleted)
                return;

            super.syncFromView();

            mDeleteButtonHasFocus = mDeleteButton.hasFocus();
        }

        private void markAsDeleted() {
            LinearLayout parent = (LinearLayout) mView.getParent();
            parent.removeView(mView);
            mDeleted = true;
        }

        public void saveToDatabase(PatoisDatabase db) {
            // TODO: Update "translations" table in the database.
            if (mDeleted) {
                db.deleteWord(mWord);
            } else {
                super.saveToDatabase(db);
            }
        }

        // Required for the Serializable interface.
        static final long serialVersionUID = -4401068042075884608L;
    }
}
