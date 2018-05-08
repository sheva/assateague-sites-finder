package com.essheva.assateague;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ofPattern;


public class Configuration {

    private static final String resourceDirPath = "src/main/resources";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final char PROPERTY_VALUES_SEPARATOR = ';';

    private static Configuration instance;

    private final String driverPath;
    private final Set<String> campGroups;
    private final Set<DayOfWeek> daysOfWeek;
    private final LocalDate searchStart;
    private final LocalDate searchStop;
    private final int minLength;
    private final boolean sendMail;
    private final boolean sendMailIfNotFound;

    private Properties mailProps;

    static synchronized Configuration getInstance() throws IOException {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    private Configuration() throws IOException {
        Properties props = new Properties();
        props.load(getReader(resourceDirPath + "/app.properties"));

        driverPath = getWebDriverFolderPathByOS();

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

        if (sendMail) {
            mailProps = new Properties();
            mailProps.load(getReader(resourceDirPath + "/user.secret"));
            if (!mailProps.containsKey("mail.smtp.host")
                    || !mailProps.containsKey("mail.imap.host")
                    || !mailProps.containsKey("mail.pop3.host")) {
                mailProps.load(getReader(resourceDirPath + "/mail_default.properties"));
            }

            if (!mailProps.containsKey("mail.from.user") || !mailProps.containsKey("mail.from.password")) {
                throw new IllegalArgumentException("You should configure authentication mail server credentials. " +
                        "Please, set 'mail.from.user' and 'mail.from.password'");
            }
        }
    }

    String getDriverPath() {
        return driverPath;
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

    boolean isSendMail() {
        return sendMail;
    }

    boolean isSendMailIfNotFound() {
        return sendMailIfNotFound;
    }

    Properties getMailProps() {
        return mailProps;
    }

    private String getWebDriverFolderPathByOS() {
        String os = System.getProperty("os.name").toLowerCase();
        final String dirName;
        if (os.contains("win")) {
            dirName = "win32";
        } else if (os.contains("mac")) {
            dirName = "mac64";
        } else if (os.contains("nux")) {
            dirName = "linux64";
        } else {
            throw new UnsupportedOperationException(os + " is not supported");
        }
        return resourceDirPath + "/chomedriver/" + dirName + "/chromedriver.exe";
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

    private static FileReader getReader(String resource) throws FileNotFoundException {
        return new FileReader(Paths.get(resource).toFile());
    }

    public boolean isDesiredDayOfWeek(LocalDate date) {
        return daysOfWeek.contains(date.getDayOfWeek());
    }
}
