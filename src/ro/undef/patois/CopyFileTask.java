package ro.undef.patois;

import android.os.AsyncTask;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CopyFileTask extends AsyncTask<Void, Void, Boolean> {
    private final static String TAG = "CopyFileTask";

    // The calling activity needs to implement this interface to be notified
    // about the various stages of the copy process.  All methods in this
    // interface are called from the UI thread.
    public interface Listener {
        // Called before the copying begins.
        public void onStartCopy();

        // Called after the copying is finished.
        public void onFinishCopy(boolean successful);
    }

    // If locking/unlocking operations need to be done around the copy
    // operation, the caller should provide a Lock implementation.  This
    // interface is provided separately from the Listener one above because the
    // Lock operations are done regardless of the attached state.
    public interface Lock {
        public void acquire();
        public void release();
    }

    public static class EmptyLock implements Lock {
        public void acquire() {}
        public void release() {}
    }

    private File mInputFile;
    private File mOutputFile;
    private Listener mListener;  // Should only be accessed from the UI thread.
    private Lock mLock;  // Should only be accessed from the UI thread.
    private boolean mFinished;
    private boolean mSuccessful;

    // The following methods should only be called from the UI thread.

    public CopyFileTask(File inputFile, File outputFile, Listener listener, Lock lock) {
        mInputFile = inputFile;
        mOutputFile = outputFile;
        mListener = listener;
        mLock = lock;
        mFinished = false;
        mSuccessful = false;
    }

    public String getInputFileName() {
        return mInputFile.getPath();
    }

    public String getOutputFileName() {
        return mOutputFile.getPath();
    }

    public boolean inputFileExists() {
        return mInputFile.exists();
    }

    public boolean outputFileExists() {
        return mOutputFile.exists();
    }

    public void detachFromListener() {
        mListener = null;
    }

    public void attachToListener(Listener listener) {
        mListener = listener;
        if (mFinished) {
            // We're being resumed after a configuration change, and the
            // background thread finished while we were detached from the
            // listening activity.  As such, the new activity still has the
            // progress dialog open, and we need to dismiss it.
            mListener.onFinishCopy(mSuccessful);
        }
    }

    @Override
    protected void onPreExecute() {
        mLock.acquire();
        if (mListener != null)
            mListener.onStartCopy();
    }

    @Override
    protected void onPostExecute(Boolean successful) {
        mLock.release();
        mLock = null;
        mFinished = true;
        mSuccessful = successful;
        if (mListener != null)
            mListener.onFinishCopy(mSuccessful);
    }

    // What follows is/can be called from the background thread.

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
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
