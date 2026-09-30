package com.ismailmushraf.bujo.utils;

import com.ismailmushraf.bujo.models.Entry;

public class EntryParser {

    public static Entry parse(String input) {
        Entry entry = new Entry();
        String content = input.trim();
        String projectTag = null;

        // 1. Identify project tag
        int tagStart = content.indexOf('#');
        if (tagStart >= 0) {
            String tagCandidate = content.substring(tagStart + 1).trim();
            if (!tagCandidate.isEmpty()) {
                projectTag = tagCandidate;
                content = content.substring(0, tagStart).trim();
            }
        }

        // An optional leading asterisk remains harmless for existing input habits.
        if (content.startsWith("*")) {
            content = content.substring(1).trim();
        }

        entry.setContent(content);
        entry.setProjectTag(projectTag);
        
        return entry;
    }
}
