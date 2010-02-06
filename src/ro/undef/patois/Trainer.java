package ro.undef.patois;

import java.util.ArrayList;
import java.util.Random;

public class Trainer {

    private Database mDb;
    private Random mRandom;
    private ArrayList<Word.Score> mScores;
    private int mMaxScore;
    private int mTotalScore;

    public Trainer(Database db, Language language) {
        mDb = db;
        mRandom = new Random();
        mScores = db.getWordScores(language);
        mMaxScore = -1;
        mTotalScore = 0;
        for (Word.Score ws : mScores) {
            mTotalScore += ws.score;
            if (ws.score > mMaxScore)
                mMaxScore = ws.score;
        }
    }

    // Returns a random word for practice.  The probablility of a word being
    // selected is directly proportional with its score.
    public Word selectWord() {
        int numWords = mScores.size();

        if (numWords == 0 || mTotalScore == 0)
            return null;

        // This is an implementation of the rejection sampling algorithm for
        // generating random samples with a particular probablility
        // distribution.  For details, see:
        //     http://en.wikipedia.org/wiki/Rejection_sampling

        // numAttempts the twice the expected number of attempts to get an
        // accepted word.  We use this to make sure we always terminate, even
        // if the scores distribution is really "adverse" (i.e., the
        // "unconditional acceptance probability" is very low).
        int numAttempts = (int)(2L * mMaxScore * numWords / mTotalScore);

        while (numAttempts --> 0) {
            Word.Score ws = mScores.get(mRandom.nextInt(numWords));
            int randomScore = mRandom.nextInt(mMaxScore);

            if (randomScore < ws.score)
                return mDb.getWord(ws.id);
        }

        return null;
    }

    public void updateWordScore(Word word, int direction, boolean success) {
        // TODO: implement me.
    }
}
