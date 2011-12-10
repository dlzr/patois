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

import android.database.Cursor;
import android.os.Environment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import ro.undef.csv.CSVWriter;


public class WordsExporter extends FilePicker {
    private final static String TAG = "WordsExporter";

    private File mOutputFile;

    public WordsExporter(int dialogIdBase) {
        super(getDefaultCsvFileName(),
              dialogIdBase,
              R.string.export_words_to_csv,
              R.layout.export_words_dialog,
              R.string.export,
              R.string.external_file_exists,
              R.string.exporting_words,
              R.string.export_successful,
              R.string.export_failed);

        mOutputFile = null;
    }

    private static String getDefaultCsvFileName() {
        return new File(Environment.getExternalStorageDirectory(),
                        CSVFormat.DEFAULT_FILE).getPath();
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
                mDb = new Database(getActivity());
            }

            protected void onFinishImmediate(boolean successful) {
                mDb.close();
            }

            protected void onFinish(boolean successful) {
                finishTask(successful);
            }

            protected Boolean doInBackground(Void... unused) {
                try {
                    doExport();
                } catch (IOException e) {
                    return false;
                } catch (IllegalArgumentException e) {
                    return false;
                }

                return true;
            }

            private void doExport() throws IOException {
                File tempFile = File.createTempFile(mOutputFile.getName() + ".", ".tmp",
                                                    mOutputFile.getParentFile());
                Writer out = new BufferedWriter(new FileWriter((tempFile)));
                try {
                    CSVWriter csvOut = new CSVWriter(out);
                    writeHeader(csvOut);
                    writeLanguages(csvOut);
                    writeWords(csvOut);
                } finally {
                    out.close();
                }

                tempFile.renameTo(mOutputFile);
            }

            private void writeHeader(CSVWriter csvOut) throws IOException {
                String[] fields = new String[3];
                fields[0] = CSVFormat.VERSION_TAG;
                fields[1] = "Patois";
                fields[2] = CSVFormat.VERSION;
                csvOut.writeRow(fields);
                csvOut.writeRow(new String[0]);
            }

            private void writeLanguages(CSVWriter csvOut) throws IOException {
                String[] fields = new String[3];
                fields[0] = CSVFormat.LANGUAGE_TAG;

                Cursor cursor = mDb.getExportLanguagesCursor();
                try {
                    while (cursor.moveToNext()) {
                        fields[1] = cursor.getString(Database.LANGUAGES_CODE_COLUMN);
                        fields[2] = cursor.getString(Database.LANGUAGES_NAME_COLUMN);
                        csvOut.writeRow(fields);
                    }
                } finally {
                    cursor.close();
                }

                csvOut.writeRow(new String[0]);
            }

            private void writeWords(CSVWriter csvOut) throws IOException {
                Cursor cursor = mDb.getExportWordsCursor();
                try {
                    while (cursor.moveToNext()) {
                        writeWord(cursor.getLong(0), csvOut);
                    }
                } finally {
                    cursor.close();
                }
            }

            private void writeWord(long wordId, CSVWriter csvOut) throws IOException {
                Word word = mDb.getWord(wordId);
                ArrayList<Word> translations = mDb.getTranslations(word);

                String[] fields = new String[1 + 2 + 2 * translations.size()];
                int i = 0;
                fields[i++] = CSVFormat.WORD_TAG;
                fields[i++] = word.getLanguage().getCode();
                fields[i++] = word.getName();
                for (Word translation : translations) {
                    fields[i++] = translation.getLanguage().getCode();
                    fields[i++] = translation.getName();
                }

                csvOut.writeRow(fields);
            }
        };
    }
}
