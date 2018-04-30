package com.essheva.assateague;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ofPattern;


public class SearchParams {
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String PROPERTY_VALUES_SEPARATOR = ";";

    private final Set<String> campGroups;
    private final Set<DayOfWeek> daysOfWeek;
    private final LocalDate start;
    private final LocalDate stop;
    private int minLength;

    SearchParams(Properties props) {
        campGroups = Arrays.stream(spiltValues(getValue(props, "search.campgroup.names"))).
                distinct().
                collect(Collectors.toSet());

        daysOfWeek = Arrays.stream(spiltValues(getValue(props, "search.days.of.week")))
                .distinct()
                .map(String::toUpperCase)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());

        start = LocalDate.parse(getValue(props,"search.start.date"), ofPattern(DATE_FORMAT));
        stop = LocalDate.parse(getValue(props,"search.stop.date"), ofPattern(DATE_FORMAT));
        minLength = Integer.valueOf(getValue(props,"search.length.of.stay"));
    }

    Set<String> getCampGroups() {
        return campGroups;
    }

    Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    LocalDate getStart() {
        return start;
    }

    LocalDate getStop() {
        return stop;
    }

    int getMinLength() {
        return minLength;
    }

    @Override
    public String toString() {
        return "SearchParams{" +
                "campGroups=" + campGroups +
                ", daysOfWeek=" + daysOfWeek +
                ", start=" + start +
                ", stop=" + stop +
                ", minLength=" + minLength +
                '}';
    }

    private String[] spiltValues(String value) {
        return value.split("\\s*" + PROPERTY_VALUES_SEPARATOR + "\\s*");
    }

    private String getValue(Properties props, String s)  {
        final String value = props.getProperty(s);
        if (value == null) {
            throw new IllegalArgumentException("Property not set " + s);
        }
        return value;
    }
}
