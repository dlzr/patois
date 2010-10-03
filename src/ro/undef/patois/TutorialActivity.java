package ro.undef.patois;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class TutorialActivity extends Activity {
    private final static String TAG = "TutorialActivity";

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
        setContentView(R.layout.tutorial_activity);

        Button button = (Button) findViewById(R.id.next);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO: Implement me.
            }
        });
    }
}
