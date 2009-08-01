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

    public Word(long id, String name, Language language) {
        mId = id;
        mName = name;
        mLanguage = language;
    }

    public Word(Language language) {
        this(-1, "", language);
    }

    public boolean isInDatabase() {
        return mId != -1;
    }

    public boolean isEmpty() {
        return mName.length() == 0;
    }

    // Required for the Serializable interface.
    static final long serialVersionUID = -4666237898176840116L;
}
