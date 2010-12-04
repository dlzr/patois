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
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import java.io.File;


public class DatabaseImporter implements CopyFileTask.Listener {
    private final static String TAG = "DatabaseImporter";

    private final int IMPORT_DATABASE_DIALOG;
    private final int CONFIRM_OVERWRITE_DIALOG;
    private final int IMPORT_DATABASE_PROGRESS;

    private Activity mActivity;
    private CopyFileTask mCopyFileTask;

    public DatabaseImporter(int dialogIdBase) {
        IMPORT_DATABASE_DIALOG = dialogIdBase;
        CONFIRM_OVERWRITE_DIALOG = dialogIdBase + 1;
        IMPORT_DATABASE_PROGRESS = dialogIdBase + 2;

        mActivity = null;
        mCopyFileTask = null;
    }

    public void attachToActivity(Activity activity) {
        mActivity = activity;
    }

    public void detachFromActivity() {
        mActivity = null;
    }

    public void start() {
        mActivity.showDialog(IMPORT_DATABASE_DIALOG);
    }

    public Dialog onCreateDialog(int id) {
        if (id == IMPORT_DATABASE_DIALOG) {
            View view = mActivity.getLayoutInflater().inflate(R.layout.import_database_dialog, null);
            final EditText fileNameEditText = (EditText) view.findViewById(R.id.file_name);
            fileNameEditText.setText(Database.getDefaultExportFile().getPath());

            return new AlertDialog.Builder(mActivity)
                .setTitle(R.string.import_database)
                .setView(view)
                .setPositiveButton(R.string.import_,
                                   new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mCopyFileTask = new CopyFileTask(
                                new File(fileNameEditText.getText().toString()),
                                Database.getDatabaseFile(mActivity),
                                DatabaseImporter.this,
                                new CopyFileTask.EmptyLock());

                        if (!mCopyFileTask.inputFileExists()) {
                            showErrorAndRestartImport(String.format(
                                    mActivity.getResources().getString(R.string.external_file_missing),
                                    mCopyFileTask.getInputFileName()));
                        } else if (mCopyFileTask.outputFileExists()) {
                            mActivity.showDialog(CONFIRM_OVERWRITE_DIALOG);
                        } else {
                            mCopyFileTask.execute();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        } else if (id == CONFIRM_OVERWRITE_DIALOG) {
            return new AlertDialog.Builder(mActivity)
                .setTitle(R.string.confirm_overwrite)
                .setMessage(mActivity.getResources().getString(R.string.internal_file_exists))
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
                        mActivity.removeDialog(CONFIRM_OVERWRITE_DIALOG);
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
        } else if (id == IMPORT_DATABASE_PROGRESS) {
            ProgressDialog dialog = new ProgressDialog(mActivity);
            // TODO: Maybe we should support canceling the import operation.
            dialog.setCancelable(false);
            dialog.setMessage(
                    String.format(mActivity.getResources().getString(R.string.importing_database),
                                  mCopyFileTask.getInputFileName()));
            return dialog;
        }
        return null;
    }

    private void showErrorAndRestartImport(String message) {
        mActivity.removeDialog(IMPORT_DATABASE_DIALOG);
        Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
        mCopyFileTask = null;
        mActivity.showDialog(IMPORT_DATABASE_DIALOG);
    }

    private void cancelConfirmOverwriteDialog() {
        // We don't want the activity to cache this dialog.
        // See above for details.
        mActivity.removeDialog(CONFIRM_OVERWRITE_DIALOG);
        mCopyFileTask = null;
    }

    public void onStartCopy() {
        mActivity.showDialog(IMPORT_DATABASE_PROGRESS);
    }

    public void onFinishCopy(boolean successful) {
        // We use removeDialog() instead of dismissDialog() here to stop the
        // activity from caching the IMPORT_DATABASE_PROGRESS dialog.  See
        // comment in onCreateDialog(CONFIRM_OVERWRITE_DIALOG) why we don't
        // want that.
        mActivity.removeDialog(IMPORT_DATABASE_PROGRESS);
        mCopyFileTask = null;

        int messageId = successful ? R.string.import_successful : R.string.import_failed;
        Toast.makeText(mActivity, messageId, Toast.LENGTH_SHORT).show();
    }
}
