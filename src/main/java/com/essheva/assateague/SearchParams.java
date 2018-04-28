package com.essheva.assateague;

import java.time.LocalDate;
import java.util.*;

import static java.time.format.DateTimeFormatter.ofPattern;


public class SearchParams {

    private final Set<String> loopNames;
    private final Set<DayOfWeek> daysOfWeek;
    private final LocalDate start;
    private final LocalDate stop;
    private int minLength = 1;

    SearchParams(Properties props) {
        loopNames = new HashSet<>(Arrays.asList(props.getProperty("search.loop.names").trim().split("\\s*;\\s*")));

        daysOfWeek = new TreeSet<>();
        for (String day : props.getProperty("search.days.of.week").trim().split("\\s*;\\s*")) {
            daysOfWeek.add(DayOfWeek.valueOf(day.toUpperCase()));
        }

        start = LocalDate.parse(props.getProperty("search.start.date"), ofPattern("yyyy-MM-dd"));
        stop = LocalDate.parse(props.getProperty("search.stop.date"), ofPattern("yyyy-MM-dd"));
        minLength = Integer.valueOf(props.getProperty("search.length.of.stay"));
    }

    Set<String> getLoopNames() {
        return loopNames;
    }

    Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public LocalDate getStart() {
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
                "loopNames=" + loopNames +
                ", daysOfWeek=" + daysOfWeek +
                ", start=" + start +
                ", stop=" + stop +
                ", minLength=" + minLength +
                '}';
    }
}
