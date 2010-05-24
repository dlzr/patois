package ro.undef.patois;

import java.util.ArrayList;
import java.util.Random;

public class Trainer {
    private final static String TAG = "Trainer";

    // The version of the training algorithm.
    public final static int VERSION = 0;

    private Database mDb;
    private Random mRandom;

    public Trainer(Database db) {
        mDb = db;
        mRandom = new Random();
    }

    // Returns a random word for practice.  The probablility of a word being
    // selected is directly proportional with its weight.
    public Word selectWord(Language language, Direction direction) throws EmptyException {
        ArrayList<Weight> weights = mDb.getWordWeights(language, direction);
        int numWeights = weights.size();
        int maxWeight = -1;
        int totalWeight = 0;
        for (Weight w : weights) {
            totalWeight += w.weight;
            if (w.weight > maxWeight)
                maxWeight = w.weight;
        }

        if (numWeights == 0 || totalWeight == 0)
            throw new EmptyException();

        // This is an implementation of the rejection sampling algorithm for
        // generating random samples with a particular probablility
        // distribution.  For details, see:
        //     http://en.wikipedia.org/wiki/Rejection_sampling

        // numAttempts is twice the expected number of attempts to get an
        // accepted word.  We use this to make sure we always terminate, even
        // if the weights distribution is really "adverse" (i.e., the
        // "unconditional acceptance probability" is very low).
        int numAttempts = (int) (2L * maxWeight * numWeights / totalWeight + 1);

        // best is the highest scoring word we've seen in the attempts to
        // select a word.  It will only be used as a last resort, in case no
        // word can be selected according to the probability distribution of
        // the weights.
        Weight best = null;

        while (numAttempts --> 0) {
            Weight picked = weights.get(mRandom.nextInt(numWeights));
            int randomWeight = mRandom.nextInt(maxWeight);

            if (randomWeight < picked.weight)
                return mDb.getWord(picked.wordId);

            if (best == null || best.weight < picked.weight)
                best = picked;
        }

        return mDb.getWord(best.wordId);
    }

    public void updatePracticeInfo(Word word, Direction direction, boolean successful) {
        PracticeInfo info = mDb.getPracticeInfo(word, direction);

        if (successful) {
            info.next_practice = scheduleNextPractice(System.currentTimeMillis() / 1000,
                                                      info.level);
            info.level++;
        } else {
            info.level = 0;
        }

        mDb.insertPracticeLogEntry(VERSION, word, direction, successful);
        mDb.updatePracticeInfo(word, info);
    }

    public static long scheduleNextPractice(long now_timestamp, int level) {
        return now_timestamp + getInterval(level);
    }

    // The minimum time in seconds between two practices of the same word.
    private static final long[] PRACTICE_INTERVALS = {
        32656,     // level 0 -- 9 hours
        86400,     // level 1 -- 24 hours
        228592,    // level 2 -- 2.6 days
        604800,    // level 3 -- 7 days
        1600150,   // level 4 -- 18 days
        4233600,   // level 5 -- 49 days
        11201052,  // level 6 -- 129 days
        29635200,  // level 7 -- 343 days
    };

    private static long getInterval(int level) {
        if (level < 0)
            level = 0;
        if (level >= PRACTICE_INTERVALS.length)
            level = PRACTICE_INTERVALS.length - 1;
        return PRACTICE_INTERVALS[level];
    }

    public static enum Direction {
        FROM_FOREIGN(0, "ro.undef.patois.intent.action.TRANSLATE_FROM_FOREIGN"),
        TO_FOREIGN(1, "ro.undef.patois.intent.action.TRANSLATE_TO_FOREIGN");

        private final int mValue;
        private final String mAction;

        Direction(int value, String action) {
            mValue = value;
            mAction = action;
        }

        public int getValue() { return mValue; }
        public String getValueString() { return Integer.toString(mValue); }
        public String getAction() { return mAction; }

        public static Direction fromValue(int value) {
            for (Direction d : Direction.values()) {
                if (d.getValue() == value)
                    return d;
            }
            throw new RuntimeException("Invalid direction value: " + value);
        }

        public static Direction fromAction(String action) {
            for (Direction d : Direction.values()) {
                if (action.equals(d.getAction()))
                    return d;
            }
            throw new RuntimeException("Invalid direction action: " + action);
        }
    };

    // Lightweight class for storing just a word ID and a weight.
    public static class Weight {
        public long wordId;
        public int weight;

        public Weight(long wordId, int weight) {
            this.wordId = wordId;
            this.weight = weight;
        }
    }

    public static class PracticeInfo {
        public Direction direction;
        public int level;
        public long next_practice;

        public PracticeInfo(Direction direction, int level, long next_practice) {
            this.direction = direction;
            this.level = level;
            this.next_practice = next_practice;
        }
    }

    // Exception thrown when an attempt is made to select a word from an empty trainer.
    public static class EmptyException extends Exception {
    }
}
