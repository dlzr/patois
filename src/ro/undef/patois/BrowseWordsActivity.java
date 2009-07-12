package ro.undef.patois;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

public class BrowseWordsActivity extends ListActivity {
    private final static String TAG = "BrowseWordsActivity";

    private PatoisDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new PatoisDatabase(this);

        setListAdapter(new SimpleCursorAdapter(
                this,
                R.layout.browse_words_list_item,
                mDb.getBrowseWordsCursor(mDb.getActiveLanguage()),
                new String[] {
                    PatoisDatabase.WORD_NAME_COLUMN,
                    PatoisDatabase.WORD_TRANSLATIONS_COLUMN,
                },
                new int[] {
                    R.id.name,
                    R.id.translations,
                }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
    }
}
