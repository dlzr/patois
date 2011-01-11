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
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CopyFileTask extends PersistentTask<Void, Boolean> {
    private final static String TAG = "CopyFileTask";

    private File mInputFile;
    private File mOutputFile;

    public CopyFileTask(Activity activity, File inputFile, File outputFile) {
        super(activity);

        mInputFile = inputFile;
        mOutputFile = outputFile;
    }

    // What follows is called from the background thread.

    @Override
    protected Boolean doInBackground(Void... unused) {
        try {
            if (!validateOutputPath())
                return false;

            copyFile();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private boolean validateOutputPath() {
        File parent = mOutputFile.getParentFile();
        if (parent == null)
            return true;

        if (parent.exists())
            return true;

        return parent.mkdirs();
    }

    private void copyFile() throws IOException {
        FileInputStream in = new FileInputStream(mInputFile);
        try {
            FileOutputStream out = new FileOutputStream(mOutputFile);
            try {
                byte buffer[] = new byte[32768];
                int count = 0;

                while ((count = in.read(buffer)) >= 0) {
                    if (count > 0)
                        out.write(buffer, 0, count);
                }

                out.getFD().sync();
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
