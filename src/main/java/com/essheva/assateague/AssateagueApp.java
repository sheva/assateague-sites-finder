package com.essheva.assateague;

import javax.mail.MessagingException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by Sheva on 3/7/2018.
 */
public class AssateagueApp {

    private static Configuration conf;
    static {
        try {
            conf = Configuration.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<Site> availableSites = new ConcurrentSkipListSet<>();

    private void retrieveAvailableDatesAndAdd(String loopName) {
        try (final SiteWebDriver driver = new SiteWebDriver(conf)){
            Set<Site> sitesFound = driver.getAvailableSites(loopName);
            availableSites.addAll(sitesFound);
        }
    }

    private void sendEmailNotification() throws MessagingException {
        if (availableSites.isEmpty() && !conf.isSendMailIfNotFound()) {
            System.out.println("Nothing to send. No available sites found.");
            return;
        }
        new SiteAvailabilityMailer(conf.getMailProps(), availableSites).sendEmail();
    }

    private void printSiteInfo() {
        availableSites.forEach((site) -> {
            final StringBuilder str = new StringBuilder(String.format(
                    "Site #%s in facility area '%s' available on dates: ", site.getSiteName(), site.getLoopName()));
            site.getAvailableDates().forEach(d -> str.append(d).append("; "));
            System.out.println(str);
        });
        if (availableSites.isEmpty()) {
            System.out.println("Nothing found.");
        }
    }

    public static void main(String... args) {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Launch of scheduler done on " + LocalDateTime.now());

                final AssateagueApp checker = new AssateagueApp();

                new ArrayList<>(conf.getCampGroups()).parallelStream().forEach(group -> {
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

                if (conf.isSendMail()) {
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