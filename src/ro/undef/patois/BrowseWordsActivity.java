package ro.undef.patois;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class BrowseWordsActivity extends ListActivity {
    private final static String TAG = "BrowseWordsActivity";

    private Database mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new Database(this);

        setListAdapter(new SimpleCursorAdapter(
                this,
                R.layout.browse_words_list_item,
                mDb.getBrowseWordsCursor(mDb.getActiveLanguage()),
                new String[] {
                    Database.BROWSE_WORDS_NAME_COLUMN,
                    Database.BROWSE_WORDS_TRANSLATIONS_COLUMN,
                },
                new int[] {
                    R.id.name,
                    R.id.translations,
                }) {

            @Override
            public void setViewText(TextView v, String text) {
                v.setText(applyWordMarkup(text));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Bundle extras = new Bundle();
        extras.putLong("word_id", id);

        Intent intent = new Intent();
        intent.setClass(this, EditWordActivity.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.putExtras(extras);

        startActivity(intent);
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
