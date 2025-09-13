package com.duck.moodflix.movie.util;

import java.util.Locale;

public final class AgeRatingDecider {
    private AgeRatingDecider() {}

    /** certification 문자열로 성인(18/19+) 여부를 판정 */
    public static boolean isAdultCert(String cert) {
        if (cert == null) return false;
        String c = cert.trim();
        String u = c.toUpperCase(Locale.ROOT);

        // 한국: 청소년관람불가/청불/19
        if (u.contains("청소년") || u.contains("청불")) return true;

        // 대표 표현들
        if (u.contains("NC-17") || u.equals("X") || u.contains("R-18") || u.contains("FSK 18")) return true;

        // 숫자 16~19 (단독 또는 +, -, 공백과 함께)
        if (u.matches(".*(^|[^0-9])(16|17|18|19)(\\+)?([^0-9]|$).*")) return true;

        return false;
    }
}
