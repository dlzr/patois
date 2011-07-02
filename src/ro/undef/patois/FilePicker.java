/*
 * Copyright 2011 David Lazăr
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


public abstract class FilePicker {
    private final static String TAG = "FilePicker";

    // Dialog IDs.
    private final int FILE_PICKER_DIALOG;
    private final int OVERWRITE_CONFIRMATION_DIALOG;
    private final int ACTION_PROGRESS_DIALOG;

    // Resource IDs for various parts of the dialogs.
    private final int FILE_PICKER_TITLE;
    private final int FILE_PICKER_LAYOUT;
    private final int ACTION_BUTTON_NAME;
    private final int OVERWRITE_CONFIRMATION_MESSAGE;
    private final int ACTION_PROGRESS_MESSAGE;

    private String mFileName;
    private Activity mActivity;
    private PersistentTask mTask;

    public FilePicker(String defaultFileName,
                      int dialogIdBase,
                      int filePickerTitle,
                      int filePickerLayout,
                      int actionButtonName,
                      int overwriteConfirmationMessage,
                      int actionProgressMessage) {
        FILE_PICKER_DIALOG = dialogIdBase;
        OVERWRITE_CONFIRMATION_DIALOG = dialogIdBase + 1;
        ACTION_PROGRESS_DIALOG = dialogIdBase + 2;

        FILE_PICKER_TITLE = filePickerTitle;
        FILE_PICKER_LAYOUT = filePickerLayout;
        ACTION_BUTTON_NAME = actionButtonName;
        OVERWRITE_CONFIRMATION_MESSAGE = overwriteConfirmationMessage;
        ACTION_PROGRESS_MESSAGE = actionProgressMessage;

        mFileName = defaultFileName;
        mActivity = null;
        mTask = null;
    }

    public void attach(Activity activity) {
        mActivity = activity;
        if (mTask != null)
            mTask.attach(activity);
    }

    public void detach() {
        if (mTask != null)
            mTask.detach();
        mActivity = null;
    }

    public void start() {
        showFilePickerDialog();
    }

    public Dialog onCreateDialog(int id) {
        if (id == FILE_PICKER_DIALOG) {
            View view = mActivity.getLayoutInflater().inflate(FILE_PICKER_LAYOUT, null);
            final EditText fileNameEditText = (EditText) view.findViewById(R.id.file_name);
            fileNameEditText.setText(mFileName);

            return new AlertDialog.Builder(mActivity)
                .setTitle(FILE_PICKER_TITLE)
                .setView(view)
                .setPositiveButton(ACTION_BUTTON_NAME, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mFileName = fileNameEditText.getText().toString();
                        prepareTask();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        }
        
        if (id == OVERWRITE_CONFIRMATION_DIALOG) {
            return new AlertDialog.Builder(mActivity)
                .setTitle(R.string.confirm_overwrite)
                .setMessage(String.format(getStringRes(OVERWRITE_CONFIRMATION_MESSAGE), mFileName))
                .setPositiveButton(R.string.yes,
                                   new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startTask();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create();
        }
        
        if (id == ACTION_PROGRESS_DIALOG) {
            ProgressDialog dialog = new ProgressDialog(mActivity);
            dialog.setCancelable(false);
            dialog.setMessage(
                    String.format(getStringRes(ACTION_PROGRESS_MESSAGE), mFileName));
            return dialog;
        }

        return null;
    }

    protected void showFilePickerDialog() {
        mActivity.removeDialog(FILE_PICKER_DIALOG);
        mActivity.showDialog(FILE_PICKER_DIALOG);
    }

    protected void showOverwriteConfirmationDialog() {
        mActivity.showDialog(OVERWRITE_CONFIRMATION_DIALOG);
    }

    protected void showProgressDialog() {
        mActivity.showDialog(ACTION_PROGRESS_DIALOG);
    }

    protected void dismissProgressDialog() {
        mActivity.dismissDialog(ACTION_PROGRESS_DIALOG);
    }


    protected Activity getActivity() {
        return mActivity;
    }

    protected String getFileName() {
        return mFileName;
    }

    protected String getStringRes(int id) {
        return mActivity.getResources().getString(id);
    }

    protected void startTask() {
        mTask = getTask();
        mTask.execute();
    }

    protected void finishTask() {
        mTask = null;
    }

    // Methods for subclasses to override.

    protected void prepareTask() {
        // By default, we just start the operation.
        startTask();
    }

    protected abstract PersistentTask getTask();
}
