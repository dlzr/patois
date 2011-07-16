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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import java.io.File;


public class DatabaseRestorer extends FilePicker {
    private final static String TAG = "DatabaseRestorer";

    private File mInputFile;
    private File mOutputFile;

    public DatabaseRestorer(int dialogIdBase) {
        super(Database.getDefaultBackupFile().getPath(),
              dialogIdBase,
              R.string.restore_database,
              R.layout.restore_database_dialog,
              R.string.restore,
              R.string.internal_file_exists,
              R.string.restoring_database);

        mInputFile = null;
        mOutputFile = null;
    }

    @Override
    protected void prepareTask() {
        mInputFile = new File(getFileName());
        if (!mInputFile.exists()) {
            String message = String.format(getStringRes(R.string.external_file_missing),
                                           getFileName());
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

            showFilePickerDialog();
            return;
        }

        mOutputFile = Database.getDatabaseFile(getActivity());
        if (mOutputFile.exists()) {
            showOverwriteConfirmationDialog();
            return;
        }

        startTask();
    }

    @Override
    protected PersistentTask getTask() {
        return new CopyFileTask(getActivity(), mInputFile, mOutputFile) {
            protected void onStart() {
                showProgressDialog();
            }

            protected void onFinish(boolean successful) {
                dismissProgressDialog();

                int messageId = successful ? R.string.restore_successful : R.string.restore_failed;
                Toast.makeText(getActivity(), messageId, Toast.LENGTH_SHORT).show();

                finishTask();
            }
        };
    }
}
