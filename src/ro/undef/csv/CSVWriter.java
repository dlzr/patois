/*
 * Copyright 2011 David Lazăr
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

package ro.undef.csv;

import java.io.IOException;
import java.io.Writer;


public class CSVWriter {
    private Writer mOut;

    public CSVWriter(Writer out) {
        mOut = out;
    }

    public void writeRow(String[] cells) throws IOException {
        boolean firstCell = true;
        for (String cell : cells) {
            if (!firstCell)
                mOut.write(",");
            mOut.write(escapeCell(cell));
            firstCell = false;
        }
        mOut.write("\r\n");
    }

    private String escapeCell(String cell) {
        if (!hasSpecialChars(cell))
            return cell;

        return escapeQuotes(cell);
    }

    private boolean hasSpecialChars(String str) {
        final String specialChars = ",\"\n\r";
        for (int i = 0; i < str.length(); i++) {
            if (specialChars.indexOf(str.charAt(i)) != -1)
                return true;
        }
        return false;
    }

    private String escapeQuotes(String str) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            out.append(c);
            if (c == '"')
                out.append(c);
        }
        out.append('"');
        return out.toString();
    }
}
