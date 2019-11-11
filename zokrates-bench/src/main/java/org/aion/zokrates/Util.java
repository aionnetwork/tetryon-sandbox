package org.aion.zokrates;

import java.util.Arrays;

public class Util {
    public static String trimHexPrefix(String s) {
        if (s != null && s.toLowerCase().startsWith("0x"))
            return s.substring(2);

        return s;
    }

    public static byte[] trimTrailingZeros(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }
}
