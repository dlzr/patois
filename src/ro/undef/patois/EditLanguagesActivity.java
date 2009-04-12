package ro.undef.patois;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;


/**
 * Activity for editting the list of languages.
 */
public class EditLanguagesActivity extends Activity {

    private LinearLayout mLayout;
    private LayoutInflater mInflater;

    private PatoisDatabase mDb;
    private ArrayList<Language> mLanguages;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInflater = getLayoutInflater();
        setContentView(R.layout.edit_languages);

        mDb = new PatoisDatabase(this);
        mDb.open();

        mLanguages = mDb.getLanguages();

        mLayout = (LinearLayout) findViewById(R.id.list);
        buildViews();
    }

    private void buildViews() {
        final LinearLayout layout = mLayout;
        layout.removeAllViews();

        for (Language language : mLanguages) {
            layout.addView(buildViewForLanguage(language));
        }

        // Add an empty language entry for new languages.
        layout.addView(buildViewForLanguage(null));
    }

    private View buildViewForLanguage(final Language language) {
        View view = mInflater.inflate(R.layout.edit_language_entry, mLayout, false);

        if (language != null) {
            EditText code_text = (EditText) view.findViewById(R.id.language_code);
            code_text.setText(language.code);
            EditText name_text = (EditText) view.findViewById(R.id.language_name);
            name_text.setText(language.name);
        }

        return view;
    }
}
