package ro.undef.patois;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
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
                    PatoisDatabase.BROWSE_WORDS_NAME_COLUMN,
                    PatoisDatabase.BROWSE_WORDS_TRANSLATIONS_COLUMN,
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
}
