package com.ismailmushraf.bujo.utils;

import com.ismailmushraf.bujo.models.Entry;

public class EntryParser {

    public static Entry parse(String input) {
        Entry entry = new Entry();
        String signifier = "*"; // Default to Task
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

        // 2. Identify signifier only for tasks if explicitly typed, but default is now task.
        // We remove support for '-' and 'o' parsing here as they are no longer handled via the text box.
        if (content.startsWith("*")) {
            content = content.substring(1).trim();
        }

        entry.setSignifier(signifier);
        entry.setContent(content);
        entry.setProjectTag(projectTag);
        
        return entry;
    }
}
