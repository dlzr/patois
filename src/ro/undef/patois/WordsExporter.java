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

import android.os.Environment;
import android.widget.Toast;
import java.io.File;


public class WordsExporter extends FilePicker {
    private final static String TAG = "WordsExporter";

    private final static String DEFAULT_CSV_FILE = "patois-words.csv";

    private File mOutputFile;

    public WordsExporter(int dialogIdBase) {
        super(getDefaultCsvFileName(),
              dialogIdBase,
              R.string.export_words_to_csv,
              R.layout.export_words_dialog,
              R.string.export,
              R.string.external_file_exists,
              R.string.exporting_words);

        mOutputFile = null;
    }

    private static String getDefaultCsvFileName() {
        return new File(Environment.getExternalStorageDirectory(), DEFAULT_CSV_FILE).getPath();
    }

    @Override
    protected void prepareTask() {
        mOutputFile = new File(getFileName());
        if (mOutputFile.exists()) {
            showOverwriteConfirmationDialog();
            return;
        }

        startTask();
    }

    @Override
    protected PersistentTask getTask() {
        return new PersistentTask(getActivity()) {
            private Database mDb;

            protected void onStart() {
                showProgressDialog();
                mDb = new Database(getActivity());
            }

            protected void onFinishImmediate(boolean successful) {
                mDb.close();
            }

            protected void onFinish(boolean successful) {
                dismissProgressDialog();

                int messageId = successful ? R.string.export_successful : R.string.export_failed;
                Toast.makeText(getActivity(), messageId, Toast.LENGTH_SHORT).show();

                finishTask();
            }

            protected Boolean doInBackground(Void... unused) {
                // TODO: Export the words.
                return true;
            }
        };
    }
}
