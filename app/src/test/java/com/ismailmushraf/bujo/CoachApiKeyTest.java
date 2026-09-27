package com.ismailmushraf.bujo;
import com.ismailmushraf.bujo.coach.CoachApiKey;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoachApiKeyTest {
    @Test public void removesClipboardArtefactsWithoutChangingToken() {
        assertEquals("example_Key-123",CoachApiKey.normalize("\u00a0\uFEFFexample_\u200BKey-123\u2060\n"));
    }
    @Test public void permitsOpaquePrintableTokensInsteadOfGuessingGoogleFormat() {
        assertTrue(CoachApiKey.valid("example.token:with+punctuation/="));
        assertTrue(CoachApiKey.valid("example_Key-123"));
        assertTrue(CoachApiKey.valid(""));
    }
    @Test public void doesNotSilentlyAlterEmbeddedWhitespaceOrLookalikes() {
        assertFalse(CoachApiKey.valid(CoachApiKey.normalize("example key")));
        assertFalse(CoachApiKey.valid("example\r\nheader"));
        assertFalse(CoachApiKey.valid("example\u2013key"));
    }
}
