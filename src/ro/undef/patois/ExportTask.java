package ro.undef.patois;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
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

    private MainActivity mActivity;  // Should only be accessed from the UI thread.
    private File mFile;
    private boolean mFinished;

    public ExportTask(MainActivity activity, String fileName) {
        mActivity = activity;
        mFile = new File(fileName);
        mFinished = false;
    }

    public boolean fileExists() {
        return mFile.exists();
    }

    public String getFileName() {
        return normalizeFileName(mFile);
    }

    public void setActivity(MainActivity activity) {
        mActivity = activity;
        if (mActivity != null && mFinished) {
            // We're being resumed after a configuration change, and the
            // background thread finished while we were disconnected from the
            // activity.  As such, the current activity still has the progress
            // dialog open, and we need to dismiss it.
            mActivity.onFinishExport();
        }
    }

    @Override
    protected void onPreExecute() {
        mActivity.onStartExport();
    }

    @Override
    protected Void doInBackground(Void... unused) {
        // TODO: Replace with the real implementation.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException x) {}
        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        mFinished = true;
        if (mActivity != null)
            mActivity.onFinishExport();
    }
}
