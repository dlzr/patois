package ro.undef.patois;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class PracticeActivity extends Activity {
    private final static String TAG = "PracticeActivity";

    public final static String ACTION_TRANSLATE_FROM_FOREIGN =
        "ro.undef.patois.intent.action.TRANSLATE_FROM_FOREIGN";
    public final static String ACTION_TRANSLATE_TO_FOREIGN =
        "ro.undef.patois.intent.action.TRANSLATE_TO_FOREIGN";

    private final static int STATE_QUESTION = 0;
    private final static int STATE_ANSWER = 1;

    private final static int DIRECTION_FROM_FOREIGN = 0;
    private final static int DIRECTION_TO_FOREIGN = 1;

    private Database mDb;
    private Trainer mTrainer;

    private LayoutInflater mInflater;
    private ViewGroup mWordPanel;
    private ViewGroup mQuestionButtons;
    private ViewGroup mAnswerButtons;

    // These fields are saved across restarts.
    private int mDirection;
    private int mState;
    private Word mWord;
    private ArrayList<Word> mTranslations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mDb = new Database(this);
            mTrainer = new Trainer(mDb, mDb.getActiveLanguage());

            if (savedInstanceState != null) {
                loadStateFromBundle(savedInstanceState);
            } else {
                String action = getIntent().getAction();
                resetState(action.equals(ACTION_TRANSLATE_FROM_FOREIGN) ?
                           DIRECTION_FROM_FOREIGN : DIRECTION_TO_FOREIGN);
            }

            setContentView(R.layout.practice_activity);
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

    private void resetState(int direction) throws Trainer.EmptyException {
        mDirection = direction;
        mState = STATE_QUESTION;
        mWord = mTrainer.selectWord();
        mTranslations = mDb.getTranslations(mWord);
    }

    @SuppressWarnings("unchecked")
    private void loadStateFromBundle(Bundle savedInstanceState) {
        mDirection = savedInstanceState.getInt("direction");
        mState = savedInstanceState.getInt("state");
        mWord = (Word) savedInstanceState.getSerializable("word");
        mTranslations = (ArrayList<Word>) savedInstanceState.getSerializable("translations");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("direction", mDirection);
        outState.putInt("state", mState);
        outState.putSerializable("word", mWord);
        outState.putSerializable("translations", mTranslations);
    }

    private void setupViews() {
        mInflater = getLayoutInflater();
        mWordPanel = (ViewGroup) findViewById(R.id.word_panel);

        if (mState == STATE_ANSWER || mDirection == DIRECTION_FROM_FOREIGN)
            mWordPanel.addView(buildWordView(mWord, true), 0);

        if (mState == STATE_ANSWER || mDirection == DIRECTION_TO_FOREIGN) {
            for (Word word : mTranslations)
                mWordPanel.addView(buildWordView(word, false));
        }

        mQuestionButtons = (ViewGroup) findViewById(R.id.question_side);
        mAnswerButtons = (ViewGroup) findViewById(R.id.answer_side);
        if (mState == STATE_QUESTION) {
            mQuestionButtons.setVisibility(View.VISIBLE);
            mAnswerButtons.setVisibility(View.GONE);
        } else if (mState == STATE_ANSWER) {
            mQuestionButtons.setVisibility(View.GONE);
            mAnswerButtons.setVisibility(View.VISIBLE);
        }

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
        if (mDirection == DIRECTION_FROM_FOREIGN) {
            for (Word word : mTranslations)
                mWordPanel.addView(buildWordView(word, false));
        } else if (mDirection == DIRECTION_TO_FOREIGN) {
            mWordPanel.addView(buildWordView(mWord, true), 0);
        }

        mQuestionButtons.setVisibility(View.GONE);
        mAnswerButtons.setVisibility(View.VISIBLE);
    }

    private void saveStatsAndRestart(boolean knewAnswer) {
        mTrainer.updateWordScore(mWord, mDirection, knewAnswer);

        Intent intent = new Intent();
        intent.setClass(PracticeActivity.this, PracticeActivity.class);
        intent.setAction((mDirection == DIRECTION_FROM_FOREIGN) ?
                         ACTION_TRANSLATE_FROM_FOREIGN :
                         ACTION_TRANSLATE_TO_FOREIGN);
        startActivity(intent);
        finish();
    }
}
