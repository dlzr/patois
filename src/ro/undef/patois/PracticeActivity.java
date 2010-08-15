package ro.undef.patois;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class PracticeActivity extends Activity {
    private final static String TAG = "PracticeActivity";

    private final static int STATE_QUESTION = 0;
    private final static int STATE_ANSWER = 1;

    private Database mDb;
    private Trainer mTrainer;

    private LayoutInflater mInflater;
    private ScoreRenderer mScoreRenderer;
    private TextView mScoreView;
    private ViewGroup mWordPanel;
    private ViewGroup mQuestionButtons;
    private ViewGroup mAnswerButtons;

    // These fields are saved across restarts.
    private Trainer.Direction mDirection;
    private int mState;
    private Word mWord;
    private ArrayList<Word> mTranslations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mDb = new Database(this);
            mTrainer = new Trainer(mDb);

            if (savedInstanceState != null) {
                loadStateFromBundle(savedInstanceState);
            } else {
                String action = getIntent().getAction();
                resetState(Trainer.Direction.fromAction(action));
            }

            setupViews();
        } catch (Trainer.EmptyException ex) {
            Toast.makeText(this, R.string.no_words_for_practice, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.close();
    }

    private void resetState(Trainer.Direction direction) throws Trainer.EmptyException {
        mDirection = direction;
        mState = STATE_QUESTION;
        loadWord(mTrainer.selectWord(mDb.getActiveLanguage(), mDirection));
    }

    private void loadWord(long wordId) {
        mWord = mDb.getWord(wordId);
        mTranslations = mDb.getTranslations(mWord);
    }

    @SuppressWarnings("unchecked")
    private void loadStateFromBundle(Bundle savedInstanceState) {
        mDirection = Trainer.Direction.fromValue(savedInstanceState.getInt("direction"));
        mState = savedInstanceState.getInt("state");
        mWord = (Word) savedInstanceState.getSerializable("word");
        mTranslations = (ArrayList<Word>) savedInstanceState.getSerializable("translations");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("direction", mDirection.getValue());
        outState.putInt("state", mState);
        outState.putSerializable("word", mWord);
        outState.putSerializable("translations", mTranslations);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.practice_activity_menu, menu);
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_word: {
                Bundle extras = new Bundle();
                extras.putLong("word_id", mWord.getId());

                Intent intent = new Intent();
                intent.setClass(this, EditWordActivity.class);
                intent.setAction(Intent.ACTION_EDIT);
                intent.putExtras(extras);

                startActivityForResult(intent, R.id.edit_word);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case R.id.edit_word:
                loadWord(mWord.getId());
                updateViews();
                break;
        }
    }


    private void setupViews() {
        setContentView(R.layout.practice_activity);

        mInflater = getLayoutInflater();

        mScoreRenderer = new ScoreRenderer(this,
                                           R.style.practice_score_good,
                                           R.style.practice_score_average,
                                           R.style.practice_score_bad);
        mScoreView = (TextView) findViewById(R.id.score);
        mWordPanel = (ViewGroup) findViewById(R.id.word_panel);
        mQuestionButtons = (ViewGroup) findViewById(R.id.question_side);
        mAnswerButtons = (ViewGroup) findViewById(R.id.answer_side);

        findViewById(R.id.show).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAnswer();
            }
        });

        findViewById(R.id.yes).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveStatsAndRestart(true);
            }
        });
        findViewById(R.id.no).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveStatsAndRestart(false);
            }
        });

        updateViews();
    }

    private void updateViews() {
        mScoreView.setText(mScoreRenderer.renderScore(mDb.getPracticeInfo(mWord, mDirection)));

        mWordPanel.removeAllViews();

        if (mState == STATE_ANSWER || mDirection == Trainer.Direction.FROM_FOREIGN)
            mWordPanel.addView(buildWordView(mWord, true), 0);

        if (mState == STATE_ANSWER || mDirection == Trainer.Direction.TO_FOREIGN) {
            for (Word word : mTranslations)
                mWordPanel.addView(buildWordView(word, false));
        }

        if (mState == STATE_QUESTION) {
            mQuestionButtons.setVisibility(View.VISIBLE);
            mAnswerButtons.setVisibility(View.GONE);
        } else if (mState == STATE_ANSWER) {
            mQuestionButtons.setVisibility(View.GONE);
            mAnswerButtons.setVisibility(View.VISIBLE);
        }
    }

    private View buildWordView(Word word, boolean isMainWord) {
        View view = mInflater.inflate(R.layout.practice_word_entry, mWordPanel, false);

        TextView nameView = (TextView) view.findViewById(R.id.name);
        nameView.setText(word.getName());

        TextView languageView = (TextView) view.findViewById(R.id.language);
        languageView.setText(String.format("(%s)", word.getLanguage().getCode()));

        if (isMainWord) {
            nameView.setTextAppearance(this, R.style.practice_main_word_name);
            languageView.setTextAppearance(this, R.style.practice_main_word_language);
        }

        return view;
    }

    private void showAnswer() {
        mState = STATE_ANSWER;

        // TODO: Should use animations when showing the answers.
        if (mDirection == Trainer.Direction.FROM_FOREIGN) {
            for (Word word : mTranslations)
                mWordPanel.addView(buildWordView(word, false));
        } else if (mDirection == Trainer.Direction.TO_FOREIGN) {
            mWordPanel.addView(buildWordView(mWord, true), 0);
        }

        mQuestionButtons.setVisibility(View.GONE);
        mAnswerButtons.setVisibility(View.VISIBLE);
    }

    private void saveStatsAndRestart(boolean knewAnswer) {
        mTrainer.updatePracticeInfo(mWord, mDirection, knewAnswer);

        // TODO: Should use animations when restarting.
        try {
            resetState(mDirection);
            updateViews();
        } catch (Trainer.EmptyException ex) {
            Toast.makeText(this, R.string.no_words_for_practice, Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
