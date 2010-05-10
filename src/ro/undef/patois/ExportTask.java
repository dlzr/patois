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
    private boolean mSuspended;
    private boolean mFinished;
    private Database.Lock mDbLock;

    // The following methods should only be called from the UI thread.

    public ExportTask(MainActivity activity, String fileName) {
        mActivity = activity;
        mFile = new File(fileName);
        mSuspended = false;
        mFinished = false;
        mDbLock = null;
    }

    public boolean fileExists() {
        return mFile.exists();
    }

    public String getFileName() {
        return normalizeFileName(mFile);
    }

    public void suspend() {
        mSuspended = true;
        mActivity = null;
    }

    public void resume(MainActivity activity) {
        mSuspended = false;
        mActivity = activity;
        if (mFinished) {
            // We're being resumed after a configuration change, and the
            // background thread finished while we were disconnected from the
            // activity.  As such, the current activity still has the progress
            // dialog open, and we need to dismiss it.
            mActivity.onFinishExport();
        }
    }

    public void onDestroy() {
        if (!mSuspended)
            cancel(false);
    }

    @Override
    protected void onPreExecute() {
        mDbLock = new Database.Lock(mActivity);
        mActivity.onStartExport();
    }

    @Override
    protected void onPostExecute(Void unused) {
        mDbLock.release();
        mDbLock = null;
        mFinished = true;
        if (mActivity != null)
            mActivity.onFinishExport();
    }

    // What follows is/can be called from the background thread.

    @Override
    protected Void doInBackground(Void... unused) {
        // TODO: Do something with FileInputStream and FileOutputStream.
        return null;
    }
}
