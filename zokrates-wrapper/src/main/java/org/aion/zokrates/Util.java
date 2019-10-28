package org.aion.zokrates;

public class Util {
    @SuppressWarnings("WeakerAccess")
    public static String sanitizeHex(String s) {
        if (s != null && s.toLowerCase().startsWith("0x"))
            return s.substring(2);

        return s;
    }
}
