package me.stephenminer.redvsblue.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringCaser {
    public static String toTitleCase(String input) {
        return Arrays.stream(input.toLowerCase().split("[\\s_\\-]")).map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
                .collect(Collectors.joining(" "));
    }
}
