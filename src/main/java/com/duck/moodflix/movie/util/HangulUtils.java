package com.duck.moodflix.movie.util;

public final class HangulUtils {
    private HangulUtils(){}

    private static final char HANGUL_BASE = 0xAC00, HANGUL_END = 0xD7A3;
    private static final int JUNG = 21, JONG = 28;
    private static final char[] CHO = {'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'};

    public static String toChoseongKey(String s) {
        if (s == null || s.isBlank()) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            if (ch >= HANGUL_BASE && ch <= HANGUL_END) {
                int idx = ch - HANGUL_BASE;
                int ci = idx / (JUNG * JONG);
                sb.append(CHO[ci]);
            } else if (Character.isLetterOrDigit(ch)) {
                sb.append(Character.toLowerCase(ch));
            }
        }
        return sb.toString();
    }

    public static boolean isChoseongQuery(String q) {
        return q != null && !q.isBlank() && q.trim().matches("^[ㄱ-ㅎ]+$");
    }
}

