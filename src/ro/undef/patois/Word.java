package ro.undef.patois;

import java.io.Serializable;


public class Word implements Serializable {
    private long mId;
    public long getId() { return mId; }
    public String getIdString() { return Long.toString(mId); }
    public void setId(long id) { mId = id; }

    private String mName;
    public String getName() { return mName; }
    public void setName(String name) { mName = name.trim(); }

    private Language mLanguage;
    public Language getLanguage() { return mLanguage; }
    public void setLanguage(Language language) { mLanguage = language; }

    private int mScore;
    public int getScore() { return mScore; }
    public void setScore(int score) { mScore = score; }

    public static final int DEFAULT_SCORE = 10;

    public Word(long id, String name, Language language, int score) {
        mId = id;
        mName = name;
        mLanguage = language;
        mScore = score;
    }

    public Word(Language language) {
        this(-1, "", language, DEFAULT_SCORE);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Word))
            return false;
        Word that = (Word) o;
        return this.mId == that.mId;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (int) (mId ^ (mId >>> 32));
        return result ;
    }

    public boolean isInDatabase() {
        return mId != -1;
    }

    public boolean isEmpty() {
        return mName.length() == 0;
    }

    // Light-weight class for storing just a word ID and a score.
    public static class Score {
        public long id;
        public int score;

        public Score(long id, int score) {
            this.id = id;
            this.score = score;
        }
    }

    // Required for the Serializable interface.
    static final long serialVersionUID = -4666237898176840116L;
}
