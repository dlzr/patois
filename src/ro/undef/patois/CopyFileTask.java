package ro.undef.patois;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class CopyFileTask extends AsyncTask<Void, Void, Boolean> {
    private final static String TAG = "CopyFileTask";

    // The calling activity needs to implement this interface to be notified
    // about the various stages of the copy process.  All methods in this
    // interface are called from the UI thread.
    public static interface Listener {
        // Called before the copying begins.
        public void onStartCopy();

        // Called after the copying is finished.
        public void onFinishCopy(boolean successful);
    }

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

    private Listener mListener;  // Should only be accessed from the UI thread.
    private File mInputFile;
    private File mOutputFile;
    private boolean mDetached;
    private boolean mFinished;
    private boolean mSuccessful;
    private Database.Lock mDbLock;

    // The following methods should only be called from the UI thread.

    public CopyFileTask(File inputFile, File outputFile, Listener listener) {
        mListener = listener;
        mInputFile = inputFile;
        mOutputFile = outputFile;
        mDetached = false;
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

    public void detachFromListener() {
        mDetached = true;
        mListener = null;
    }

    public void attachToListener(Listener listener) {
        mDetached = false;
        mListener = listener;
        if (mFinished) {
            // We're being resumed after a configuration change, and the
            // background thread finished while we were detached from the
            // listening activity.  As such, the new activity still has the
            // progress dialog open, and we need to dismiss it.
            mListener.onFinishCopy(mSuccessful);
        }
    }

    public void onDestroy() {
        if (!mDetached)
            cancel(false);
    }

    @Override
    protected void onPreExecute() {
        mDbLock = new Database.Lock(mInputFile.getPath());
        mListener.onStartCopy();
    }

    @Override
    protected void onPostExecute(Boolean successful) {
        mDbLock.release();
        mDbLock = null;
        mFinished = true;
        mSuccessful = successful;
        if (mListener != null)
            mListener.onFinishCopy(mSuccessful);
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
