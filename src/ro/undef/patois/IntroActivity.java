/*
 * Copyright 2010 David Lazar
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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.File;


public class IntroActivity extends Activity implements CopyFileTask.Listener {
    private final static String TAG = "IntroActivity";

    private static final int IMPORT_DATABASE_DIALOG = 1;
    private static final int CONFIRM_OVERWRITE_DIALOG = 2;
    private static final int IMPORT_DATABASE_PROGRESS = 3;

    // Note that mCopyFileTask is null most of the times.  The only time it
    // points to a valid object is between the user clicking the "Import
    // database" button, and the import operation finishing.
    // As such, whenever accessing mCopyFileTask, make sure it's not null.
    private CopyFileTask mCopyFileTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCopyFileTask = (CopyFileTask) getLastNonConfigurationInstance();
        if (mCopyFileTask != null)
            mCopyFileTask.attachToListener(this);

        setupViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO: Maybe we should support canceling the import operation.
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mCopyFileTask != null)
            mCopyFileTask.detachFromListener();
        return mCopyFileTask;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case IMPORT_DATABASE_DIALOG: {
                View view = getLayoutInflater().inflate(R.layout.import_database_dialog, null);
                final EditText fileNameEditText = (EditText) view.findViewById(R.id.file_name);
                fileNameEditText.setText(Database.getDefaultExportFile().getPath());

                return new AlertDialog.Builder(this)
                    .setTitle(R.string.import_database)
                    .setView(view)
                    .setPositiveButton(R.string.import_,
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mCopyFileTask = new CopyFileTask(
                                    new File(fileNameEditText.getText().toString()),
                                    Database.getDatabaseFile(IntroActivity.this),
                                    IntroActivity.this,
                                    new CopyFileTask.EmptyLock());

                            if (!mCopyFileTask.inputFileExists()) {
                                showErrorAndRestartImport(String.format(
                                        getResources().getString(R.string.external_file_missing),
                                        mCopyFileTask.getInputFileName()));
                            } else if (mCopyFileTask.outputFileExists()) {
                                showDialog(CONFIRM_OVERWRITE_DIALOG);
                            } else {
                                mCopyFileTask.execute();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            }
            case CONFIRM_OVERWRITE_DIALOG: {
                return new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_overwrite)
                    .setMessage(getResources().getString(R.string.internal_file_exists))
                    .setPositiveButton(R.string.yes,
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // We don't want the activity to cache this dialog
                            // so we call removeDialog() explicitly.  If this
                            // dialog were to be cached, the activity would
                            // call onCreateDialog() when resuming from
                            // configuration changes, and mCopyFileTask could
                            // be null at that point, leading to a
                            // NullPointerException in the setup code above.
                            removeDialog(CONFIRM_OVERWRITE_DIALOG);
                            mCopyFileTask.execute();
                        }
                    })
                    .setNegativeButton(R.string.no,
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            cancelConfirmOverwriteDialog();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            cancelConfirmOverwriteDialog();
                        }
                    })
                    .create();
            }
            case IMPORT_DATABASE_PROGRESS: {
                ProgressDialog dialog = new ProgressDialog(this);
                // TODO: Maybe we should support canceling the import operation.
                dialog.setCancelable(false);
                dialog.setMessage(
                        String.format(getResources().getString(R.string.importing_database),
                                      mCopyFileTask.getInputFileName()));
                return dialog;
            }
        }
        return null;
    }

    private void showErrorAndRestartImport(String message) {
        removeDialog(IMPORT_DATABASE_DIALOG);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        mCopyFileTask = null;
        showDialog(IMPORT_DATABASE_DIALOG);
    }

    private void cancelConfirmOverwriteDialog() {
        // We don't want the activity to cache this dialog.
        // See above for details.
        removeDialog(CONFIRM_OVERWRITE_DIALOG);
        mCopyFileTask = null;
    }

    public void onStartCopy() {
        showDialog(IMPORT_DATABASE_PROGRESS);
    }

    public void onFinishCopy(boolean successful) {
        // We use removeDialog() instead of dismissDialog() here to stop the
        // activity from caching the IMPORT_DATABASE_PROGRESS dialog.  See
        // comment in onCreateDialog(CONFIRM_OVERWRITE_DIALOG) why we don't
        // want that.
        removeDialog(IMPORT_DATABASE_PROGRESS);
        mCopyFileTask = null;

        int messageId = successful ? R.string.import_successful : R.string.import_failed;
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();

        forwardToActivity(MainActivity.class);
    }

    private void setupViews() {
        setContentView(R.layout.intro_activity);

        Button button = (Button) findViewById(R.id.start_tutorial);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO: Open the on-line tutorial in the browser.
            }
        });

        button = (Button) findViewById(R.id.import_database);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(IMPORT_DATABASE_DIALOG);
            }
        });

        button = (Button) findViewById(R.id.start_patois);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                forwardToActivity(MainActivity.class);
            }
        });
    }

    private void forwardToActivity(Class<?> cls) {
        Intent intent = new Intent();
        intent.setClass(this, cls);
        startActivity(intent);
        finish();
    }
}
