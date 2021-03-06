/*
 * Copyright 2010 David Lazăr
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

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FilterQueryProvider;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class BrowseWordsActivity extends ListActivity {
    private final static String TAG = "BrowseWordsActivity";

    private Database mDb;
    private Cursor mCursor;
    private SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new Database(this);
        mCursor = mDb.getBrowseWordsCursor(mDb.getActiveLanguage(), "");
        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.browse_words_list_item,
                mCursor,
                new String[] {
                    Database.BROWSE_WORDS_NAME_COLUMN,
                    Database.BROWSE_WORDS_TRANSLATIONS_COLUMN,
                    // Never used: WordViewBinder accesses the cursor directly.
                    Database.BROWSE_WORDS_DUMMY_SCORE_COLUMN,
                },
                new int[] {
                    R.id.name,
                    R.id.translations,
                    R.id.score,
                });
        mAdapter.setViewBinder(new WordViewBinder());
        mAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                return mDb.getBrowseWordsCursor(
                        mDb.getActiveLanguage(),
                        (constraint != null) ? constraint.toString() : "");
            }
        });
        setListAdapter(mAdapter);

        ListView listView = getListView();
        listView.setFastScrollEnabled(true);
        listView.setTextFilterEnabled(true);
        registerForContextMenu(listView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        startEditWordActivity(id);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.browse_words_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.reset_score: {
                mDb.resetPracticeInfoById(info.id);
                mCursor.requery();
                return true;
            }
            case R.id.edit_word: {
                startEditWordActivity(info.id);
                return true;
            }
            case R.id.delete_word: {
                mDb.deleteWordById(info.id);
                mCursor.requery();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.browse_words_activity_menu, menu);
	return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Show the active sort order as selected.
        int id = R.id.sort_by_name;
        switch (mDb.getSortOrder()) {
            case Database.SORT_ORDER_BY_NAME:
                id = R.id.sort_by_name;
                break;
            case Database.SORT_ORDER_BY_SCORE:
                id = R.id.sort_by_score;
                break;
            case Database.SORT_ORDER_NEWEST_FIRST:
                id = R.id.sort_newest_first;
                break;
            case Database.SORT_ORDER_OLDEST_FIRST:
                id = R.id.sort_oldest_first;
                break;
        }
        MenuItem item = menu.findItem(id);
        item.setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_by_name: {
                setSortOrder(Database.SORT_ORDER_BY_NAME);
                return true;
            }
            case R.id.sort_by_score: {
                setSortOrder(Database.SORT_ORDER_BY_SCORE);
                return true;
            }
            case R.id.sort_newest_first: {
                setSortOrder(Database.SORT_ORDER_NEWEST_FIRST);
                return true;
            }
            case R.id.sort_oldest_first: {
                setSortOrder(Database.SORT_ORDER_OLDEST_FIRST);
                return true;
            }
            case R.id.add_words: {
                startEditWordActivity(-1);
                return true;
            }
        }
        return false;
    }

    private void setSortOrder(int order) {
        mDb.setSortOrder(order);
        mCursor = mDb.getBrowseWordsCursor(mDb.getActiveLanguage(), "");
        mAdapter.changeCursor(mCursor);
    }

    private void startEditWordActivity(long id) {
        Intent intent = new Intent();
        intent.setClass(this, EditWordActivity.class);

        if (id != -1) {
            intent.setAction(Intent.ACTION_EDIT);
            intent.putExtra(EditWordActivity.EXTRA_WORD_ID, id);
        } else {
            intent.setAction(Intent.ACTION_INSERT);
        }

        startActivity(intent);
    }

    // Class to use with SimpleCursorAdapter for displaying the word entries.
    // Note that this inner class is NOT static, since it uses resources from
    // the outer activity.
    private class WordViewBinder implements SimpleCursorAdapter.ViewBinder {
        private ScoreRenderer mScoreRenderer;

        public WordViewBinder() {
            mScoreRenderer = new ScoreRenderer(BrowseWordsActivity.this,
                                               R.style.browse_score_good,
                                               R.style.browse_score_average,
                                               R.style.browse_score_bad);
        }

        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            switch (view.getId()) {
                case R.id.name:
                case R.id.translations: {
                    TextView v = (TextView) view;
                    String text = cursor.getString(columnIndex);
                    v.setText(applyWordMarkup(text));
                    return true;
                }
                case R.id.score: {
                    TextView v = (TextView) view;
                    v.setText(mScoreRenderer.renderScores(
                            cursor.getInt(Database.BROWSE_WORDS_LEVEL_FROM_COLUMN_ID),
                            cursor.getInt(Database.BROWSE_WORDS_NEXT_PRACTICE_FROM_COLUMN_ID),
                            cursor.getInt(Database.BROWSE_WORDS_LEVEL_TO_COLUMN_ID),
                            cursor.getInt(Database.BROWSE_WORDS_NEXT_PRACTICE_TO_COLUMN_ID)));
                    return true;
                }
            }
            return false;
        }

        // This function implements a simple mark-up language for putting
        // rich-text into the BrowseWordsActivity list.  We implement this
        // by hand because we want to use SQLite for generating the content
        // of the two text boxes in the list item (it would be
        // significantly more complex to generate the list of translations
        // outside of SQLite, since we would have to issue sub-queries for
        // each word).  See the SELECT statement in
        // Database.getBrowseWordsCursor() for how details.
        //
        // However, we still want getBrowseWordsCursor() to signal the
        // presence of special items such as the language code tag or words
        // without translations.  As such, this simple markup language was
        // born.
        //
        // All characters in the input text are copied as-is to the output
        // buffer, with the exception of the escape sequences documented
        // next.  An escape sequence is a two-character sequence, of which
        // the first is '.' (dot).  The following escape sequences are
        // defined:
        //
        //      '..'    - will output a single dot
        //      '.c'    - start LANGUAGE CODE formatting
        //      '.C'    - stop LANGUAGE CODE formatting
        //      '.u'    - start UNTRANSLATED WORD formatting
        //      '.U'    - stop UNTRANSLATED WORD formatting
        //      '.0'    - insert NO TRANSLATIONS text
        //
        // All other escape sequences will be ignored.
        //
        private Spannable applyWordMarkup(String text) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            int languageCodeStart = 0;
            int untranslatedStart = 0;

            int textLength = text.length();
            for (int i = 0; i < textLength; i++) {
                char c = text.charAt(i);
                if (c == '.') {
                    if (++i < textLength) {
                        c = text.charAt(i);
                        if (c == '.') {
                            ssb.append(c);
                        } else if (c == 'c') {
                            languageCodeStart = ssb.length();
                        } else if (c == 'C') {
                            ssb.setSpan(new TextAppearanceSpan(BrowseWordsActivity.this,
                                                               R.style.language_code_tag),
                                        languageCodeStart, ssb.length(), 0);
                            languageCodeStart = ssb.length();
                        } else if (c == 'u') {
                            untranslatedStart = ssb.length();
                        } else if (c == 'U') {
                            ssb.setSpan(new TextAppearanceSpan(BrowseWordsActivity.this,
                                                               R.style.untranslated_word),
                                        untranslatedStart, ssb.length(), 0);
                            untranslatedStart = ssb.length();
                        } else if (c == '0') {
                            Resources res = getResources();
                            ssb.append(res.getString(R.string.no_translations));
                        }
                    }
                } else
                    ssb.append(c);
            }

            return ssb;
        }
    }
}
