/*
 * Copyright 2010 David Lazar
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

// This class adds nextLong(n) to java.util.Random.

public class Random extends java.util.Random {

    // The implementation is heavily based on nextInt(n) from java.util.Random.
    public long nextLong(long n) {
        if (n > 0) {
            long bits, val;
            do {
                bits = nextLong() >> 1; // We only need positive longs.
                val = bits % n;
            } while (bits - val + (n - 1) < 0);
            return val;
        }
        throw new IllegalArgumentException();
    }
}
