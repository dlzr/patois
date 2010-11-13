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
    public void setName(String name) { mName = name.trim(); }

    private long mNumWords;
    public long getNumWords() { return mNumWords; }

    public Language(long id, String code, String name, long numWords) {
        mId = id;
        mCode = code;
        mName = name;
        mNumWords = numWords;
    }

    public Language() {
        this(-1, "", "", 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Language))
            return false;
        Language that = (Language) o;
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
        return (mCode.length() == 0) && (mName.length() == 0);
    }

    // Required for the Serializable interface.
    static final long serialVersionUID = -8997431011778280454L;
}
