package ro.undef.patois;

import java.io.Serializable;


public class Language implements Serializable {
    private long mId;
    public long getId() { return mId; }
    public String getIdString() { return Long.toString(mId); }
    public void setId(long id) { mId = id; }

    private String mCode;
    public String getCode() { return mCode; }
    public void setCode(String code) { mCode = code; }

    private String mName;
    public String getName() { return mName; }
    public void setName(String name) { mName = name; }

    public Language(long id, String code, String name) {
        mId = id;
        mCode = code;
        mName = name;
    }

    public Language() {
        this(-1, "", "");
    }

    public boolean notInDatabase() {
        return mId == -1;
    }

    // Required for the Serializable interface.
    static final long serialVersionUID = -8997431011778280454L;
}
