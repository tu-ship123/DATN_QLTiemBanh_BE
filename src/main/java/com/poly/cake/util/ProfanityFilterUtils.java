package com.poly.cake.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfanityFilterUtils {

    // Danh sách các từ tục tĩu, nhạy cảm phổ biến (Bạn có thể bổ sung thêm)
    private static final List<String> BAD_WORDS = Arrays.asList(
        "đm", "dm", "đmm", "dmm", "đ.m", "vãi", "vl", "vcl", "vkl", 
        "cc", "cl", "lồn", "lon", "buồi", "buoi", "cặc", "cac", 
        "mẹ kiếp", "chó chết", "fuck", "bitch", "shit", "asshole"
    );

    private static final Pattern PROFANITY_PATTERN;

    static {
        // Xây dựng Regex pattern lọc chính xác từ ngữ vi phạm
        StringBuilder patternString = new StringBuilder("\\b(");
        for (int i = 0; i < BAD_WORDS.size(); i++) {
            patternString.append(Pattern.quote(BAD_WORDS.get(i)));
            if (i < BAD_WORDS.size() - 1) {
                patternString.append("|");
            }
        }
        patternString.append(")\\b");
        PROFANITY_PATTERN = Pattern.compile(patternString.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    }

    /**
     * Mã hóa các từ vi phạm thành dấu * (ví dụ: "đm" -> "**")
     */
    public static String filterText(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        Matcher matcher = PROFANITY_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String word = matcher.group();
            String stars = "*".repeat(word.length());
            matcher.appendReplacement(sb, stars);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}