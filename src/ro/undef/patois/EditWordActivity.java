package ro.undef.patois;

import android.app.Activity;
import android.os.Bundle;


public class EditWordActivity extends Activity {
    private final static String TAG = "EditWordActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_word);
    }
}
