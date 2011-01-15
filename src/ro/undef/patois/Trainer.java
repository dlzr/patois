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

import java.util.ArrayList;

public class Trainer {
    private final static String TAG = "Trainer";

    // The version of the training algorithm.
    // See patois.sql for already used version numbers.  In particular,
    // version 1 has been reserved for manual score resets.
    public final static int TRAINER_VERSION = 0;
    public final static int MANUAL_SCORE_RESET_VERSION = 1;

    private Database mDb;
    private Random mRandom;

    public Trainer(Database db) {
        mDb = db;
        mRandom = new Random();
    }

    // Returns a random word ID for practice.  The probablility of a word being
    // selected is directly proportional with its weight.
    public long selectWord(Language language, Direction direction) throws EmptyException {
        ArrayList<Weight> weights = mDb.getWordWeights(language, direction);
        int numWeights = weights.size();
        long maxWeight = -1;
        long totalWeight = 0;
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
            long randomWeight = mRandom.nextLong(maxWeight);

            if (randomWeight < picked.weight)
                return picked.wordId;

            if (best == null || best.weight < picked.weight)
                best = picked;
        }

        return best.wordId;
    }

    public void updatePracticeInfo(Word word, Direction direction, boolean successful) {
        PracticeInfo info = mDb.getPracticeInfo(word, direction);

        if (successful) {
            info.nextPractice = scheduleNextPractice(System.currentTimeMillis() / 1000,
                                                     info.level);
            info.level++;
        } else {
            info.level = 0;
        }

        mDb.updatePracticeInfo(word, info, TRAINER_VERSION, successful);
    }

    public static long scheduleNextPractice(long now, int level) {
        return now + getInterval(level);
    }

    // The minimum time in seconds between two practices of the same word.
    private static final long[] PRACTICE_INTERVALS = {
          6 * 60 * 60,        // level 0 -- 6 hours
          2 * 24 * 60 * 60,   // level 1 -- 2 days
          7 * 24 * 60 * 60,   // level 2 -- 7 days
         30 * 24 * 60 * 60,   // level 3 -- 30 days
        180 * 24 * 60 * 60,   // level 4 -- 180 days
    };
    private static final int MAX_LEVEL = PRACTICE_INTERVALS.length - 1;

    private static int clamp(int level) {
        if (level < 0)
            level = 0;
        if (level > MAX_LEVEL)
            level = MAX_LEVEL;
        return level;
    }

    private static long getInterval(int level) {
        return PRACTICE_INTERVALS[clamp(level)];
    }

    public static int getNumStars(int level, int maxStars) {
        return (clamp(level) + 1) * maxStars / (MAX_LEVEL + 1);
    }

    public static enum Direction {
        FROM_FOREIGN(0, "_from", "ro.undef.patois.intent.action.TRANSLATE_FROM_FOREIGN"),
        TO_FOREIGN(1, "_to", "ro.undef.patois.intent.action.TRANSLATE_TO_FOREIGN");

        private final int mValue;
        private final String mSuffix;
        private final String mAction;

        Direction(int value, String suffix, String action) {
            mValue = value;
            mSuffix = suffix;
            mAction = action;
        }

        public int getValue() { return mValue; }
        public String getSuffix() { return mSuffix; }
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
        public long weight;

        public Weight(long wordId, long weight) {
            this.wordId = wordId;
            this.weight = weight;
        }
    }

    public static class PracticeInfo {
        public Direction direction;
        public int level;
        public long nextPractice;

        public PracticeInfo(Direction direction, int level, long nextPractice) {
            this.direction = direction;
            this.level = level;
            this.nextPractice = nextPractice;
        }
    }

    // Exception thrown when an attempt is made to select a word from an empty trainer.
    public static class EmptyException extends Exception {
    }
}
