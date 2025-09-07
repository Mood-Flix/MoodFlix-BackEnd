package com.duck.moodflix.movie.util;

import org.springframework.stereotype.Component;

@Component
public class ImageUrlResolver {

    private static final String ROOT = "https://image.tmdb.org/t/p";

    public String w185(String path) { return ROOT + "/w185" + path; }
    public String w342(String path) { return ROOT + "/w342" + path; }
    public String w500(String path) { return ROOT + "/w500" + path; }
    public String w780(String path) { return ROOT + "/w780" + path; }

    /** TMDb avatarPath('/abc.jpg') vs gravatar('/https://...') 대응 */
    public String avatar(String avatarPath) {
        if (avatarPath == null || avatarPath.isBlank()) return null;
        String p = avatarPath.trim();
        if (p.startsWith("/https://") || p.startsWith("/http://")) {
            return p.substring(1);
        }
        if (p.startsWith("/")) {
            return w185(p);
        }
        return p;
    }
}
