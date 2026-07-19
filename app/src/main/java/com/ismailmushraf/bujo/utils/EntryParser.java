package com.ismailmushraf.bujo.utils;

import com.ismailmushraf.bujo.models.Entry;

public class EntryParser {

    public static Entry parse(String input) {
        Entry entry = new Entry();
        String signifier = "-"; // Default
        String content = input.trim();
        String projectTag = null;

        // A project tag consumes the remainder of the input. This allows natural
        // names such as "# Project One" and "#Project-One" without splitting
        // the second word into a journal entry.
        int tagStart = content.indexOf('#');
        if (tagStart >= 0) {
            String tagCandidate = content.substring(tagStart + 1).trim();
            if (!tagCandidate.isEmpty()) {
                projectTag = tagCandidate;
                content = content.substring(0, tagStart).trim();
            }
        }

        // 2. Identify signifier at the very start
        if (content.startsWith("*")) {
            signifier = "*";
            content = content.substring(1).trim();
        } else if (content.startsWith("-")) {
            signifier = "-";
            content = content.substring(1).trim();
        } else if (content.startsWith("o")) {
            // Check if it's 'o ' or just 'o' to avoid catching words starting with o
            if (content.startsWith("o ") || content.length() == 1) {
                signifier = "o";
                content = content.substring(1).trim();
            }
        }

        entry.setSignifier(signifier);
        entry.setContent(content);
        entry.setProjectTag(projectTag);
        
        return entry;
    }
}
