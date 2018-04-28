package com.essheva.assateague;

import javax.mail.MessagingException;
import javax.naming.ConfigurationException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by Sheva on 3/7/2018.
 */
public class AvailableSitesFinder {

    private static final String resourceDirPath = "src/main/resources";
    private static Properties props;

    static {
        try {
            props = new Properties();
            props.load(getReader(resourceDirPath + "/app.properties"));
            props.load(getReader(resourceDirPath + "/user.secret"));
            if (!props.containsKey("mail.smtp.host") || !props.containsKey("mail.imap.host") || !props.containsKey("mail.pop3.host")) {
                props.load(getReader(resourceDirPath + "/mail_default.properties"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<Site> availableSites = new ConcurrentSkipListSet<>();
    private final SearchParams params;
    private final String driverPath;

    private AvailableSitesFinder(SearchParams params) {
        this.params = params;
        this.driverPath = getOSDriverFolderPath();
    }

    private void retrieveAvailableDatesAndAdd(String loopName) {
        SiteWebDriver driver = new SiteWebDriver(driverPath);
        Set<Site> sitesFound = driver.getAvailableSites(loopName, params);
        availableSites.addAll(sitesFound);
    }

    private void sendEmailNotification() throws MessagingException, ConfigurationException {
        if (availableSites.isEmpty() && "false".equalsIgnoreCase(props.getProperty("mail.send.if.not.found"))) {
            System.out.println("Nothing to send. No available sites found.");
        }
        new SiteAvailabilityMailer(props, availableSites).sendEmail();
    }

    private void printSiteInfo() {
        availableSites.forEach((site) -> {
            StringBuilder str = new StringBuilder(String.format(
                    "Site #%s in facility area '%s' available on dates: ", site.getSiteName(), site.getLoopName()));
            site.getAvailableDates().forEach(d -> str.append(d).append("; "));
            System.out.println(str);
        });
        if (availableSites.isEmpty()) {
            System.out.println("Nothing found.");
        }
    }

    private static FileReader getReader(String resource) throws FileNotFoundException {
        return new FileReader(Paths.get(resource).toFile());
    }

    private String getOSDriverFolderPath() {
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

    public static void main(String... args) {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Launch of scheduler done on " + LocalDateTime.now());

                SearchParams params = new SearchParams(props);
                AvailableSitesFinder checker = new AvailableSitesFinder(params);

                new ArrayList<>(params.getCampGroups()).stream().parallel().forEach(group -> {
                    try {
                        checker.retrieveAvailableDatesAndAdd(group);
                        scheduler.awaitTermination(5L, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        synchronized (System.err) {
                            e.printStackTrace();
                        }
                    }
                });

                checker.printSiteInfo();

                if ("true".equals(props.getProperty("mail.send"))) {
                    System.out.println("Send email notification action requested.");
                    checker.sendEmailNotification();
                }

                System.out.println("Scheduler task was finished on " + LocalDateTime.now());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        },0, 3, TimeUnit.HOURS);
    }
}