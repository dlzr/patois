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
import android.os.AsyncTask;

public abstract class PersistentTask<Params, Result> extends AsyncTask<Params, Void, Result> {
    private final static String TAG = "PersistentTask";

    private Activity mActivity = null;
    private boolean mOnFinishPending = false;
    private Result mResult = null;

    public PersistentTask(Activity activity) {
        mActivity = activity;
    }

    public void detach() {
        mActivity = null;
    }

    public void attach(Activity activity) {
        mActivity = activity;
        if (mOnFinishPending) {
            // We're being resumed after a configuration change, and the
            // background thread finished while we were detached from the
            // listening activity.  As such, the new activity still has the
            // progress dialog open, and we need to dismiss it.
            mOnFinishPending = false;
            onFinish(mResult);
        }
    }

    protected Activity getActivity() {
        return mActivity;
    }

    @Override
    protected void onPreExecute() {
        onStart();
    }

    @Override
    protected void onPostExecute(Result result) {
        onFinishImmediate(result);
        if (mActivity != null) {
            onFinish(result);
        } else {
            mResult = result;
            mOnFinishPending = true;
        }
    }

    // Use onStart() instead of onPreExecute().
    protected void onStart() {
    }

    // onFinishImmediate() will be called as soon as the background processing
    // is finished, regardless of the connected state.  You can use this for
    // cleanup tasks that don't depend on the owning activity and must be done
    // even if we never get re-attached to an activity.
    protected void onFinishImmediate(Result result) {
    }

    // Use onFinish() instead of onPostExecute().  onFinish() will only be
    // called when attached to an activity.
    protected void onFinish(Result result) {
    }
}
