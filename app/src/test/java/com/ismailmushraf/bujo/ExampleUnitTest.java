package com.ismailmushraf.bujo;

import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.utils.EntryParser;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void parser_accepts_spaced_project_names() {
        Entry entry = EntryParser.parse("# Project One");

        assertEquals("Project One", entry.getProjectTag());
        assertEquals("", entry.getContent());
    }

    @Test
    public void parser_accepts_hyphenated_project_names() {
        Entry entry = EntryParser.parse("* Plan release #Project-One");

        assertEquals("Project-One", entry.getProjectTag());
        assertEquals("Plan release", entry.getContent());
        assertEquals("*", entry.getSignifier());
    }
}
