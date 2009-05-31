package ro.undef.patois;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class PatoisMainActivity extends Activity {
    private final static String TAG = "PatoisMainActivity";

    private PatoisDatabase mDb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new PatoisDatabase(this);

        setContentView(R.layout.main_activity);
        updateLabels();

        View view = findViewById(R.id.main_title);
        view.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(R.id.select_language);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
    }

    private void updateLabels() {
        Resources res = getResources();

        String language = mDb.getActiveLanguageName();
        if (language == null)
            language = res.getString(R.string.foreign);

        TextView mainTitle = (TextView) findViewById(R.id.main_title);
        mainTitle.setText(language);

        Button button = (Button) findViewById(R.id.from_foreign);
        button.setText(String.format(res.getString(R.string.from_foreign), language));

        button = (Button) findViewById(R.id.to_foreign);
        button.setText(String.format(res.getString(R.string.to_foreign), language));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.main_activity_menu, menu);
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_language: {
                showDialog(R.id.select_language);
                return true;
            }
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case R.id.select_language:
                return buildSelectLanguageDialog();
        }
        return null;
    }

    private Dialog buildSelectLanguageDialog() {
        final Cursor cursor = mDb.getLanguages();
        return new AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setCursor(cursor, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    cursor.moveToPosition(which);
                    activateLanguage(cursor.getLong(PatoisDatabase.LANGUAGE_ID_COLUMN));
                }
            }, "name")
            .setNeutralButton(R.string.edit_languages,
                              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startEditLanguagesActivity();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create();
    }

    private void activateLanguage(long id) {
        mDb.setActiveLanguage(id);
        updateLabels();
    }

    private void startEditLanguagesActivity() {
        Intent intent = new Intent();
        intent.setClass(PatoisMainActivity.this, EditLanguagesActivity.class);
        startActivityForResult(intent, R.id.select_language);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case R.id.select_language:
                updateLabels();
                break;
        }
    }
}
