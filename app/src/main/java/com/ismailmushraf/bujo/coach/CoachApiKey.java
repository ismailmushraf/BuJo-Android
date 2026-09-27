package com.ismailmushraf.bujo.coach;

/** Clipboard cleanup, not provider-specific authentication validation. Never log keys. */
public final class CoachApiKey {
    private CoachApiKey() {}
    public static String normalize(String raw) {
        if(raw==null) return "";
        String value=raw.replace("\u200B","").replace("\uFEFF","").replace("\u2060","");
        int start=0,end=value.length();
        while(start<end && boundarySpace(value.charAt(start))) start++;
        while(end>start && boundarySpace(value.charAt(end-1))) end--;
        return value.substring(start,end);
    }
    private static boolean boundarySpace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }
    public static boolean valid(String key) {
        // Accept header-safe opaque tokens; Google, not a guessed regex, validates the key.
        if(key.length()>4096) return false;
        for(int i=0;i<key.length();i++) if(key.charAt(i)<33 || key.charAt(i)>126) return false;
        return true;
    }
}
