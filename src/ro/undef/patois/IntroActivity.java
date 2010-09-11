package ro.undef.patois;

import android.app.Activity;
import android.os.Bundle;


public class IntroActivity extends Activity {
    private final static String TAG = "IntroActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setupViews() {
        setContentView(R.layout.intro_activity);
    }
}
