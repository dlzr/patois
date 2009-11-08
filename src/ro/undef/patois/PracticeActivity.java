package ro.undef.patois;

import android.app.Activity;
import android.os.Bundle;

public class PracticeActivity extends Activity {
    private final static String TAG = "PracticeActivity";

    private PatoisDatabase mDb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new PatoisDatabase(this);

        setContentView(R.layout.practice_activity);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
    }
}
