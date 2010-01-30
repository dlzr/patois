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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";

    private Database mDb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new Database(this);

        setContentView(R.layout.main_activity);
        updateLabels();

        View view = findViewById(R.id.main_title);
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(R.id.select_language);
            }
        });

        Button button = (Button) findViewById(R.id.browse_words);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startBrowseWordsActivity();
            }
        });

        button = (Button) findViewById(R.id.add_words);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startEditWordActivity();
            }
        });

        button = (Button) findViewById(R.id.from_foreign);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPracticeActivity(PracticeActivity.ACTION_TRANSLATE_FROM_FOREIGN);
            }
        });

        button = (Button) findViewById(R.id.to_foreign);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPracticeActivity(PracticeActivity.ACTION_TRANSLATE_TO_FOREIGN);
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
        boolean enabled = true;

        Language language = mDb.getActiveLanguage();
        if (language == null) {
            language = new Language(-1, "XX", res.getString(R.string.foreign), 0);
            enabled = false;
        }

        TextView mainTitle = (TextView) findViewById(R.id.main_title);
        if (enabled) {
            mainTitle.setText(language.getName());
        } else {
            mainTitle.setText(R.string.select_language);
        }

        Button button = (Button) findViewById(R.id.browse_words);
        button.setEnabled(enabled);

        button = (Button) findViewById(R.id.add_words);
        button.setEnabled(enabled);

        button = (Button) findViewById(R.id.from_foreign);
        button.setText(String.format(res.getString(R.string.from_foreign),
                                     language.getName()));
        button.setEnabled(enabled);

        button = (Button) findViewById(R.id.to_foreign);
        button.setText(String.format(res.getString(R.string.to_foreign),
                                     language.getName()));
        button.setEnabled(enabled);
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
                final Cursor cursor = mDb.getLanguagesCursor();
                return new AlertDialog.Builder(this)
                    .setTitle(R.string.select_language)
                    .setCursor(cursor, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            cursor.moveToPosition(which);
                            activateLanguageId(cursor.getLong(Database.LANGUAGES_ID_COLUMN));
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
        return null;
    }

    private void activateLanguageId(long id) {
        mDb.setActiveLanguageId(id);
        updateLabels();
    }

    private void startEditLanguagesActivity() {
        Intent intent = new Intent();
        intent.setClass(this, EditLanguagesActivity.class);
        startActivityForResult(intent, R.id.select_language);
    }

    private void startBrowseWordsActivity() {
        Intent intent = new Intent();
        intent.setClass(this, BrowseWordsActivity.class);
        startActivity(intent);
    }

    private void startEditWordActivity() {
        Intent intent = new Intent();
        intent.setClass(this, EditWordActivity.class);
        intent.setAction(Intent.ACTION_INSERT);
        startActivity(intent);
    }

    private void startPracticeActivity(String action) {
        Intent intent = new Intent();
        intent.setClass(this, PracticeActivity.class);
        intent.setAction(action);
        startActivity(intent);
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
