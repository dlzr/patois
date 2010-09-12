package ro.undef.patois;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class CopyFileTask extends AsyncTask<Void, Void, Boolean> {
    private final static String TAG = "CopyFileTask";

    public static String getDefaultFileName() {
        return normalizeFileName(new File(Environment.getExternalStorageDirectory(),
                                            Database.DATABASE_NAME));
    }

    private static String normalizeFileName(File file) {
        try {
            return file.getCanonicalPath();
        } catch (java.io.IOException e) {
            return file.getAbsolutePath();
        }
    }

    private MainActivity mActivity;  // Should only be accessed from the UI thread.
    private File mInputFile;
    private File mOutputFile;
    private boolean mSuspended;
    private boolean mFinished;
    private boolean mSuccessful;
    private Database.Lock mDbLock;

    // The following methods should only be called from the UI thread.

    public CopyFileTask(MainActivity activity, String fileName) {
        mActivity = activity;
        mInputFile = Database.getDatabaseFile(activity);
        mOutputFile = new File(fileName);
        mSuspended = false;
        mFinished = false;
        mSuccessful = false;
        mDbLock = null;
    }

    public boolean fileExists() {
        return mOutputFile.exists();
    }

    public String getFileName() {
        return normalizeFileName(mOutputFile);
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
            mActivity.onFinishExport(mSuccessful);
        }
    }

    public void onDestroy() {
        if (!mSuspended)
            cancel(false);
    }

    @Override
    protected void onPreExecute() {
        mDbLock = new Database.Lock(mInputFile.getPath());
        mActivity.onStartExport();
    }

    @Override
    protected void onPostExecute(Boolean successful) {
        mDbLock.release();
        mDbLock = null;
        mFinished = true;
        mSuccessful = successful;
        if (mActivity != null)
            mActivity.onFinishExport(mSuccessful);
    }

    // What follows is/can be called from the background thread.

    @Override
    protected Boolean doInBackground(Void... unused) {
        try {
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
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (java.io.IOException e) {
            return false;
        }

        return true;
    }
}
