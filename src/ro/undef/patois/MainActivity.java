package ro.undef.patois;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";

    private static final int SELECT_LANGUAGE_DIALOG = 1;
    private static final int EXPORT_DATABASE_DIALOG = 2;
    private static final int CONFIRM_OVERWRITE_DIALOG = 3;
    private static final int EXPORT_DATABASE_PROGRESS = 4;

    private static class NonConfigurationInstance {
        private boolean mSuspended;
        public Database db;
        // Note that exportTask is null most of the times.  The only time it
        // points to a valid object is between the user selecting "Export
        // database" from the menu, and the export operation finishing.  As
        // such, whenever accessing exportTask, make sure it's not null.
        public ExportTask exportTask;

        public NonConfigurationInstance(MainActivity activity) {
            mSuspended = false;
            db = new Database(activity);
            exportTask = null;
        }

        public void suspend() {
            mSuspended = true;
            setExportTaskActivity(null);
        }

        public void resume(MainActivity activity) {
            db.changeActivity(activity);
            setExportTaskActivity(activity);
            mSuspended = false;
        }

        public void onDestroy() {
            if (!mSuspended) {
                db.close();
                cancelExportTask();
            }
        }

        private void setExportTaskActivity(MainActivity activity) {
            if (exportTask != null)
                exportTask.setActivity(activity);
        }

        private void cancelExportTask() {
            if (exportTask != null)
                exportTask.cancel(false);
        }
    }

    private NonConfigurationInstance mInstance;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInstance = (NonConfigurationInstance) getLastNonConfigurationInstance();
        if (mInstance != null) {
            mInstance.resume(this);
        } else {
            mInstance = new NonConfigurationInstance(this);
        }

        setupViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mInstance.onDestroy();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mInstance.suspend();
        return mInstance;
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
                showDialog(EXPORT_DATABASE_DIALOG);
                return true;
            }
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case SELECT_LANGUAGE_DIALOG: {
                final Cursor cursor = mInstance.db.getLanguagesCursor();
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
            case EXPORT_DATABASE_DIALOG: {
                View view = getLayoutInflater().inflate(R.layout.export_database_dialog, null);
                final EditText fileNameEditText = (EditText) view.findViewById(R.id.file_name);
                fileNameEditText.setText(ExportTask.getDefaultFileName());

                return new AlertDialog.Builder(this)
                    .setTitle(R.string.export_database)
                    .setView(view)
                    .setPositiveButton(R.string.export,
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mInstance.exportTask = new ExportTask(
                                    MainActivity.this, fileNameEditText.getText().toString());
                            if (mInstance.exportTask.fileExists()) {
                                showDialog(CONFIRM_OVERWRITE_DIALOG);
                            } else {
                                mInstance.exportTask.execute();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            }
            case CONFIRM_OVERWRITE_DIALOG: {
                return new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_overwrite)
                    .setMessage(String.format(getResources().getString(R.string.file_exists),
                                              mInstance.exportTask.getFileName()))
                    .setPositiveButton(R.string.yes,
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // We don't want the activity to cache this dialog
                            // because, if it did, it would call
                            // onCreateDialog() when resuming from
                            // configuration changes, and mInstance.exportTask
                            // could be null at that point.
                            removeDialog(CONFIRM_OVERWRITE_DIALOG);
                            mInstance.exportTask.execute();
                        }
                    })
                    .setNegativeButton(R.string.no,
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // We don't want the activity to cache this dialog.
                            // See above for details.
                            removeDialog(CONFIRM_OVERWRITE_DIALOG);
                            mInstance.exportTask = null;
                            showDialog(EXPORT_DATABASE_DIALOG);
                        }
                    })
                    .create();
            }
            case EXPORT_DATABASE_PROGRESS: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage(
                        String.format(getResources().getString(R.string.exporting_database),
                                      mInstance.exportTask.getFileName()));
                return dialog;
            }
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case R.id.select_language:
                mInstance.db.clearLanguagesCache();
                updateLabels();
                break;
        }
    }

    public void onStartExport() {
        showDialog(EXPORT_DATABASE_PROGRESS);
    }

    public void onFinishExport() {
        // We use removeDialog() instead of dismissDialog() here to stop the
        // activity from caching the EXPORT_DATABASE_PROGRESS dialog.  See
        // comment in onCreateDialog(CONFIRM_OVERWRITE_DIALOG) why we don't
        // want that.
        removeDialog(EXPORT_DATABASE_PROGRESS);
        mInstance.exportTask = null;
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

    private void updateLabels() {
        Resources res = getResources();
        boolean enabled = true;

        Language language = mInstance.db.getActiveLanguage();
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
        mInstance.db.setActiveLanguageId(id);
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
}
