package com.essheva.assateague;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ofPattern;


public class Configuration {
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final char PROPERTY_VALUES_SEPARATOR = ';';

    private final Set<String> campGroups;
    private final Set<DayOfWeek> daysOfWeek;
    private final LocalDate searchStart;
    private final LocalDate searchStop;
    private int minLength;
    private boolean sendMail;
    private boolean sendMailIfNotFound;

    Configuration(Properties props) {
        campGroups = Arrays.stream(spiltValues(getValue(props, "search.campgroup.names"))).
                distinct().
                collect(Collectors.toSet());

        daysOfWeek = Arrays.stream(spiltValues(getValue(props, "search.days.of.week")))
                .distinct()
                .map(String::toUpperCase)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());

        searchStart = LocalDate.parse(getValue(props,"search.start.date"), ofPattern(DATE_FORMAT));
        searchStop = LocalDate.parse(getValue(props,"search.stop.date"), ofPattern(DATE_FORMAT));
        minLength = Integer.valueOf(getValue(props,"search.length.of.stay"));

        sendMail = Boolean.valueOf(props.getProperty("mail.send"));
        sendMailIfNotFound = Boolean.valueOf(props.getProperty("mail.send.if.not.found"));
    }

    Set<String> getCampGroups() {
        return campGroups;
    }

    Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    LocalDate getSearchStart() {
        return searchStart;
    }

    LocalDate getSearchStop() {
        return searchStop;
    }

    int getMinLength() {
        return minLength;
    }

    public boolean isSendMail() {
        return sendMail;
    }

    public boolean isSendMailIfNotFound() {
        return sendMailIfNotFound;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "campGroups=" + campGroups +
                ", daysOfWeek=" + daysOfWeek +
                ", searchStart=" + searchStart +
                ", searchStop=" + searchStop +
                ", minLength=" + minLength +
                ", sendMail=" + sendMail +
                ", sendMailIfNotFound=" + sendMailIfNotFound +
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
