package ro.undef.patois;

import java.util.ArrayList;
import java.util.Random;

public class Trainer {
    private final static String TAG = "Trainer";

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
    public Word selectWord() throws EmptyException {
        int numWords = mScores.size();

        if (numWords == 0 || mTotalScore == 0)
            throw new EmptyException();

        // This is an implementation of the rejection sampling algorithm for
        // generating random samples with a particular probablility
        // distribution.  For details, see:
        //     http://en.wikipedia.org/wiki/Rejection_sampling

        // numAttempts is twice the expected number of attempts to get an
        // accepted word.  We use this to make sure we always terminate, even
        // if the scores distribution is really "adverse" (i.e., the
        // "unconditional acceptance probability" is very low).
        int numAttempts = (int) (2L * mMaxScore * numWords / mTotalScore + 1);

        // bestWs is the highest scoring word we've seen in the attempts to
        // select a word.  It will only be used as a last resort, in case no
        // word can be selected according to the probability distribution of
        // the scores.
        Word.Score bestWs = null;

        while (numAttempts --> 0) {
            Word.Score ws = mScores.get(mRandom.nextInt(numWords));
            int randomScore = mRandom.nextInt(mMaxScore);

            if (randomScore < ws.score)
                return mDb.getWord(ws.id);

            if (bestWs == null || bestWs.score < ws.score)
                bestWs = ws;
        }

        return mDb.getWord(bestWs.id);
    }

    public void updateWordScore(Word word, int direction, boolean successful) {
        int score = word.getScore();
        if (successful) {
            if (score > 1)
                score--;
        } else {
            score = (int) (1.2 * score + 1);
        }
        word.setScore(score);

        mDb.insertPracticeLogEntry(word, direction, successful);
        mDb.updateWord(word);
    }

    // Exception thrown when an attempt is made to select a word from an empty trainer.
    public static class EmptyException extends Exception {
    }
}
