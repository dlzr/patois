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

    private static final int SELECT_LANGUAGE_DIALOG = 1;
    private static final int DATABASE_EXPORTER_DIALOG_BASE = 100;

    private Database mDb;
    private DatabaseExporter mDbExporter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new Database(this);

        mDbExporter = (DatabaseExporter) getLastNonConfigurationInstance();
        if (mDbExporter == null) {
            mDbExporter = new DatabaseExporter(DATABASE_EXPORTER_DIALOG_BASE);

            // We only check for an empty database when the activity is first
            // started.  If we're being restarted because of a configuration
            // change, mDbExporter will be non-null at this point.
            if (mDb.getLanguagesCursor().getCount() == 0)
                startEditLanguagesActivity();
        }
        mDbExporter.attach(this);

        setupViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
        // TODO: Maybe we should support canceling the export operation.
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mDbExporter.detach();
        return mDbExporter;
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
                showDialog(SELECT_LANGUAGE_DIALOG);
                return true;
            }
            case R.id.export_database: {
                mDbExporter.start();
                return true;
            }
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = mDbExporter.onCreateDialog(id);
        if (dialog != null)
            return dialog;

        switch (id) {
            case SELECT_LANGUAGE_DIALOG: {
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
        }
        return null;
    }

    private void setupViews() {
        setContentView(R.layout.main_activity);
        updateLabels();

        View view = findViewById(R.id.main_title);
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(SELECT_LANGUAGE_DIALOG);
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
                startPracticeActivity(Trainer.Direction.FROM_FOREIGN);
            }
        });

        button = (Button) findViewById(R.id.to_foreign);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPracticeActivity(Trainer.Direction.TO_FOREIGN);
            }
        });
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

    private void startPracticeActivity(Trainer.Direction direction) {
        Intent intent = new Intent();
        intent.setClass(this, PracticeActivity.class);
        intent.setAction(direction.getAction());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case R.id.select_language:
                mDb.clearLanguagesCache();
                updateLabels();
                break;
        }
    }
}
