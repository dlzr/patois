package ro.undef.patois;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;

/**
 * Activity for editting the list of languages.
 */
public class EditLanguagesActivity extends Activity {

    private LayoutInflater mInflater;
    private PatoisDatabase mDb;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInflater = getLayoutInflater();

        mDb = new PatoisDatabase(this);
        mDb.open();

        setContentView(R.layout.edit_languages);
    }
}
