package com.example.azentio.floodgate.utils;


public final class MaskingUtils {

    private static final int  VISIBLE_TAIL = 4;
    private static final char MASK_CHAR    = 'X';

    private MaskingUtils() {}

    /** Masks a bank account number, leaving only the last 4 characters visible. */
    public static String maskAccount(String account) {
        return mask(account);
    }

    /** Masks a PAN, leaving only the last 4 characters visible. */
    public static String maskPan(String pan) {
        return mask(pan);
    }

    private static String mask(String value) {
        if (value == null) return null;
        int len = value.length();
        if (len <= VISIBLE_TAIL) return "X".repeat(len);
        int maskLen = len - VISIBLE_TAIL;
        return MASK_CHAR + "X".repeat(maskLen - 1) + value.substring(maskLen);
    }
}

