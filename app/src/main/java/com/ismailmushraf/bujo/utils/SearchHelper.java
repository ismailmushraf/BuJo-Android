package com.ismailmushraf.bujo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchHelper {

    public interface StringExtractor<T> {
        String extract(T item);
    }

    public static <T> List<T> filter(List<T> originalList, String query, StringExtractor<T> extractor) {
        if (originalList == null) return new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(originalList);
        }

        String lowerQuery = query.trim().toLowerCase(Locale.US);
        List<T> filtered = new ArrayList<>();
        for (T item : originalList) {
            if (item != null) {
                String text = extractor.extract(item);
                if (text != null && text.toLowerCase(Locale.US).contains(lowerQuery)) {
                    filtered.add(item);
                }
            }
        }
        return filtered;
    }
}
