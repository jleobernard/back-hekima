package com.leo.hekima.utils;

import java.text.Normalizer;
import java.util.Locale;

public class DataUtils {

    public static String sanitize(final String q) {
        if( q == null) {
            return null;
        }
        return Normalizer.normalize(q.trim(), Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ENGLISH);
    }
}
