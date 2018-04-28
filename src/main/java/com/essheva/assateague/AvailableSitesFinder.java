package com.essheva.assateague;

import javax.mail.MessagingException;
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

    private static Properties props;
    static {
        try {
            props = new Properties();
            props.load(new FileReader(Paths.get("src/main/resources/app.properties").toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<Site> availableSites = new ConcurrentSkipListSet<>();
    private final SearchParams params;

    private AvailableSitesFinder(SearchParams params) {
        this.params = params;
    }

    private void retrieveAvailableDatesAndAdd(String loopName) {
        SiteWebDriver driver = new SiteWebDriver(props);
        Set<Site> sitesFound = driver.getAvailableSites(loopName, params);
        availableSites.addAll(sitesFound);
    }

    private void sendEmailNotification() throws MessagingException {

        printSiteInfo();

        if (!availableSites.isEmpty()) {
            new SiteAvailabilityMailer(props, availableSites).sendEmail();
        } else {
            synchronized (System.out) {
                System.out.println("Nothing found.");
            }
        }
    }

    private void printSiteInfo() {
        availableSites.forEach((site) -> {
            StringBuilder str = new StringBuilder(String.format(
                    "Site #%s in facility area '%s' available on dates: ", site.getSiteName(), site.getLoopName()));
            site.getAvailableDates().forEach(d -> str.append(d).append("; "));
            synchronized (System.out) {
                System.out.println(str);
            }
        });
    }

    public static void main(String... args) {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Launch of scheduler done on " + LocalDateTime.now());

                SearchParams params = new SearchParams(props);
                AvailableSitesFinder checker = new AvailableSitesFinder(params);

                new ArrayList<>(params.getLoopNames()).stream().parallel().forEach(loopName -> {
                    try {
                        checker.retrieveAvailableDatesAndAdd(loopName);
                        scheduler.awaitTermination(5L, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        synchronized (System.err) {
                            e.printStackTrace();
                        }
                    }
                });
                checker.sendEmailNotification();

                System.out.println("Scheduler task was finished on " + LocalDateTime.now());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        },0, 3, TimeUnit.HOURS);
    }
}