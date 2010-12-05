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
    private File mInputFile;
    private File mOutputFile;

    public DatabaseImporter(int dialogIdBase) {
        IMPORT_DATABASE_DIALOG = dialogIdBase;
        CONFIRM_OVERWRITE_DIALOG = dialogIdBase + 1;
        IMPORT_DATABASE_PROGRESS = dialogIdBase + 2;

        mActivity = null;
        mInputFile = null;
        mOutputFile = null;
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
                .setPositiveButton(R.string.import_, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        prepareImport(fileNameEditText.getText().toString());
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
                        startImport();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create();
        } else if (id == IMPORT_DATABASE_PROGRESS) {
            ProgressDialog dialog = new ProgressDialog(mActivity);
            dialog.setCancelable(false);
            dialog.setMessage(
                    String.format(mActivity.getResources().getString(R.string.importing_database),
                                  mInputFile.getPath()));
            return dialog;
        }
        return null;
    }

    private void prepareImport(String inputFileName) {
        mInputFile = new File(inputFileName);
        if (!mInputFile.exists()) {
            String message = String.format(
                    mActivity.getResources().getString(R.string.external_file_missing),
                    inputFileName);
            Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();

            mActivity.removeDialog(IMPORT_DATABASE_DIALOG);
            mActivity.showDialog(IMPORT_DATABASE_DIALOG);
            return;
        }

        mOutputFile = Database.getDatabaseFile(mActivity);
        if (mOutputFile.exists()) {
            mActivity.showDialog(CONFIRM_OVERWRITE_DIALOG);
            return;
        }

        startImport();
    }

    private void startImport() {
        new CopyFileTask(mInputFile, mOutputFile, this, new CopyFileTask.EmptyLock()).execute();
    }

    public void onStartCopy() {
        mActivity.showDialog(IMPORT_DATABASE_PROGRESS);
    }

    public void onFinishCopy(boolean successful) {
        mActivity.dismissDialog(IMPORT_DATABASE_PROGRESS);

        int messageId = successful ? R.string.import_successful : R.string.import_failed;
        Toast.makeText(mActivity, messageId, Toast.LENGTH_SHORT).show();
    }
}
