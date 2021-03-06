/*
 * Copyright 2010 David Lazăr
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
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class IntroActivity extends Activity {
    private final static String TAG = "IntroActivity";

    private static final int DATABASE_RESTORER_DIALOG_BASE = 100;

    private DatabaseRestorer mDbRestorer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbRestorer = (DatabaseRestorer) getLastNonConfigurationInstance();
        if (mDbRestorer == null)
            mDbRestorer = new DatabaseRestorer(DATABASE_RESTORER_DIALOG_BASE);
        mDbRestorer.attach(this);

        setupViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mDbRestorer.detach();
        return mDbRestorer;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return mDbRestorer.onCreateDialog(id);
    }

    private void setupViews() {
        setContentView(R.layout.intro_activity);

        Button button = (Button) findViewById(R.id.start_tutorial);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO: Open the on-line tutorial in the browser.
            }
        });

        button = (Button) findViewById(R.id.restore_database);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDbRestorer.start();
            }
        });

        button = (Button) findViewById(R.id.start_patois);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                forwardToActivity(MainActivity.class);
            }
        });
    }

    private void forwardToActivity(Class<?> cls) {
        Intent intent = new Intent();
        intent.setClass(this, cls);
        startActivity(intent);
        finish();
    }
}
