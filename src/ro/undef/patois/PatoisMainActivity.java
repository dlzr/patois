package ro.undef.patois;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View;

// XXX: Temporary imports
import android.widget.Toast;


public class PatoisMainActivity extends Activity {
    private final static String TAG = "PatoisMainActivity";

    private PatoisDatabase mDb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        View view = findViewById(R.id.main_title);
        view.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doSelectLanguageAction();
            }
        });

        mDb = new PatoisDatabase(this);
        mDb.open();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.main_activity_menu, menu);
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_language: {
                doSelectLanguageAction();
                return true;
            }
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case R.id.select_language:
                return buildSelectLanguageDialog();
        }
        return null;
    }

    private void doSelectLanguageAction() {
        showDialog(R.id.select_language);
    }

    private Dialog buildSelectLanguageDialog() {
        return new AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setCursor(mDb.getLanguages(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(PatoisMainActivity.this,
                                   "Selected language " + which + ".",
                                   Toast.LENGTH_SHORT).show();
                    // TODO: Save the selected language to "properties"
                    // and update the title of the main window.
                }
            }, "name")
            .setNeutralButton(R.string.edit_languages,
                              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startEditLanguagesActivity();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create();
    }

    private void startEditLanguagesActivity() {
        Intent intent = new Intent();
        intent.setClass(PatoisMainActivity.this, EditLanguagesActivity.class);
        startActivity(intent);
    }
}
