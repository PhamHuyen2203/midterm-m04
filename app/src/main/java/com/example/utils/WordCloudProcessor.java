package com.example.utils;

import android.content.Context;

import com.example.mcommercemobile04.R;
import com.example.models.WordFrequency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class WordCloudProcessor {

    private static final Pattern WORD_SEPARATOR =
            Pattern.compile(
                    "[^\\p{L}\\p{Nd}]+"
            );

    private WordCloudProcessor() {
        // Không cho phép khởi tạo đối tượng.
    }

    /**
     * Tạo danh sách từ và số lần xuất hiện.
     */
    public static List<WordFrequency> buildWordFrequencies(
            Context context,
            List<String> comments,
            int maximumWords
    ) {

        if (comments == null
                || comments.isEmpty()
                || maximumWords <= 0) {

            return new ArrayList<>();
        }

        Set<String> stopWords =
                new HashSet<>(
                        Arrays.asList(
                                context.getResources()
                                        .getStringArray(
                                                R.array
                                                        .word_cloud_stop_words
                                        )
                        )
                );

        Map<String, Integer> frequencyMap =
                new HashMap<>();

        Locale vietnameseLocale =
                Locale.forLanguageTag("vi-VN");

        for (String comment : comments) {

            if (comment == null
                    || comment.trim().isEmpty()) {

                continue;
            }

            String normalizedComment =
                    comment.toLowerCase(
                            vietnameseLocale
                    );

            String[] words =
                    WORD_SEPARATOR.split(
                            normalizedComment
                    );

            for (String word : words) {

                String normalizedWord =
                        word.trim();

                if (normalizedWord.length() < 2) {
                    continue;
                }

                if (stopWords.contains(normalizedWord)) {
                    continue;
                }

                int currentFrequency =
                        frequencyMap.getOrDefault(
                                normalizedWord,
                                0
                        );

                frequencyMap.put(
                        normalizedWord,
                        currentFrequency + 1
                );
            }
        }

        List<WordFrequency> frequencies =
                new ArrayList<>();

        for (
                Map.Entry<String, Integer> entry
                : frequencyMap.entrySet()
        ) {

            frequencies.add(
                    new WordFrequency(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }

        frequencies.sort(
                Comparator
                        .comparingInt(
                                WordFrequency::getFrequency
                        )
                        .reversed()
                        .thenComparing(
                                WordFrequency::getWord
                        )
        );

        if (frequencies.size() > maximumWords) {

            return new ArrayList<>(
                    frequencies.subList(
                            0,
                            maximumWords
                    )
            );
        }

        return frequencies;
    }
}