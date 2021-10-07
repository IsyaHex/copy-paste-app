package com.app.check;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/*
 * Enum class represents the date file filter options.
 * Also see FileFilters and FileFiltersDialog.
 */
public enum DateOption {

    ALL_DAYS,
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS;

    @Override
    public String toString() {

        return getFormattedString(super.toString());
    }

    /*
     * Formats enum's string, for example: from LAST_7_DAYS to: Last 7 days.
     * Replaces the underscore ("_") with a blank space. Changes the case to
     * first letter upper and remaining lower case.
     */
    private static String getFormattedString(String input) {

        String s = input.toLowerCase();
        s = s.replace("_", " ");
        return (s.substring(0, 1).toUpperCase() + s.substring(1));
    }

    /* Map with formatted string as key and enum constant as value */
    private static final Map<String, DateOption> map;

    /* Initially, populates the map */
    static {
        map = EnumSet.allOf(DateOption.class)
                .stream()
                .collect(Collectors.toMap(
                        d -> getFormattedString(d.toString()), Function.identity()));
    }

    /*
     * The lookup for the map. Returns the enum constant for the
     * given string representing the constant.
     */
    public static DateOption lookup(String s) {

        return map.get(s);
    }
}
