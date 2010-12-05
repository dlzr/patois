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


public class DatabaseExporter implements CopyFileTask.Listener {
    private final static String TAG = "DatabaseExporter";

    private final int EXPORT_DATABASE_DIALOG;
    private final int CONFIRM_OVERWRITE_DIALOG;
    private final int EXPORT_DATABASE_PROGRESS;

    private Activity mActivity;
    private File mInputFile;
    private File mOutputFile;

    public DatabaseExporter(int dialogIdBase) {
        EXPORT_DATABASE_DIALOG = dialogIdBase;
        CONFIRM_OVERWRITE_DIALOG = dialogIdBase + 1;
        EXPORT_DATABASE_PROGRESS = dialogIdBase + 2;

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
        mActivity.showDialog(EXPORT_DATABASE_DIALOG);
    }

    public Dialog onCreateDialog(int id) {
        if (id == EXPORT_DATABASE_DIALOG) {
            View view = mActivity.getLayoutInflater().inflate(R.layout.export_database_dialog, null);
            final EditText fileNameEditText = (EditText) view.findViewById(R.id.file_name);
            fileNameEditText.setText(Database.getDefaultExportFile().getPath());

            return new AlertDialog.Builder(mActivity)
                .setTitle(R.string.export_database)
                .setView(view)
                .setPositiveButton(R.string.export, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        prepareExport(fileNameEditText.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        } else if (id == CONFIRM_OVERWRITE_DIALOG) {
            return new AlertDialog.Builder(mActivity)
                .setTitle(R.string.confirm_overwrite)
                .setMessage(String.format(
                        mActivity.getResources().getString(R.string.external_file_exists),
                        mOutputFile.getPath()))
                .setPositiveButton(R.string.yes,
                                   new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startExport();
                    }
                })
                .setNegativeButton(R.string.no,
                                   new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.showDialog(EXPORT_DATABASE_DIALOG);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mActivity.showDialog(EXPORT_DATABASE_DIALOG);
                    }
                })
                .create();
        } else if (id == EXPORT_DATABASE_PROGRESS) {
            ProgressDialog dialog = new ProgressDialog(mActivity);
            dialog.setCancelable(false);
            dialog.setMessage(
                    String.format(mActivity.getResources().getString(R.string.exporting_database),
                                  mOutputFile.getPath()));
            return dialog;
        }
        return null;
    }

    private void prepareExport(String outputFileName) {
        mInputFile = Database.getDatabaseFile(mActivity);
        if (!mInputFile.exists())
            throw new RuntimeException("Cannot export database: missing database file.");

        mOutputFile = new File(outputFileName);
        if (mOutputFile.exists()) {
            mActivity.showDialog(CONFIRM_OVERWRITE_DIALOG);
            return;
        }

        startExport();
    }

    private void startExport() {
        new CopyFileTask(mInputFile, mOutputFile, this,
                         new Database.Lock(mInputFile.getPath())).execute();
    }

    public void onStartCopy() {
        mActivity.showDialog(EXPORT_DATABASE_PROGRESS);
    }

    public void onFinishCopy(boolean successful) {
        mActivity.dismissDialog(EXPORT_DATABASE_PROGRESS);

        int messageId = successful ? R.string.export_successful : R.string.export_failed;
        Toast.makeText(mActivity, messageId, Toast.LENGTH_SHORT).show();
    }
}
