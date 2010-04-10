package ro.undef.patois;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import java.io.File;
import java.io.IOException;

public class ExportTask extends AsyncTask<Void, Void, Void> {
    private final static String TAG = "ExportTask";

    public static String getDefaultFileName() {
        return normalizeFileName(new File(Environment.getExternalStorageDirectory(),
                                            Database.DATABASE_NAME));
    }

    private static String normalizeFileName(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    private File mFile;

    public ExportTask(String fileName) {
        mFile = new File(fileName);
    }

    public boolean fileExists() {
        return mFile.exists();
    }

    public String getFileName() {
        return normalizeFileName(mFile);
    }

    @Override
    protected Void doInBackground(Void... unused) {
        // TODO: Implement me.
        return null;
    }
}
