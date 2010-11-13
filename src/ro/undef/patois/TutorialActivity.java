/*
 * Copyright 2010 David Lazar
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
