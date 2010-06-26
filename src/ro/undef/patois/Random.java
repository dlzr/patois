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
