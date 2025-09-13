package com.duck.moodflix.movie.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AgeRatingDecider {
    private AgeRatingDecider() {}

    // 숫자 18/19(+옵션)만 매칭하고 숫자 경계 보장 (예: 2019, 119 같은 오탐 방지)
    private static final Pattern ADULT_NUMBER_PATTERN =
            Pattern.compile("(?<!\\d)(18|19)\\+?(?!\\d)");

    /** certification 문자열로 성인(18/19+) 여부를 판정 */
    public static boolean isAdultCert(String cert) {
        if (cert == null) return false;

        final String raw = cert.trim();
        if (raw.isEmpty()) return false;

        // 공백 제거 버전(한글 표현에 공백이 섞여도 감지되도록)
        final String compact = raw.replaceAll("\\s+", "");
        final String lc = raw.toLowerCase(Locale.ROOT);
        final String lcCompact = compact.toLowerCase(Locale.ROOT);

        // 한국 표현: 청소년관람불가/청불/제한상영/19금
        if (lcCompact.contains("청소년관람불가")) return true;
        if (lc.contains("청불")) return true;
        if (lc.contains("제한상영")) return true;
        if (lc.contains("19금")) return true;
        if (lc.contains("19세")) return true; // 예: "19세 이상 관람가"

        // 대표 국제 표현: NC-17, X, R-18/R18, FSK 18
        if (lc.contains("nc-17")) return true;
        if (lc.equals("x")) return true;
        if (lc.contains("r-18") || lc.contains("r18")) return true;
        if (lc.contains("fsk 18") || lc.contains("fsk-18")) return true;

        // 숫자 18/19(+옵션)만 성인 처리 (경계 보장)
        if (ADULT_NUMBER_PATTERN.matcher(lc).find()) return true;

        return false;
    }
}
