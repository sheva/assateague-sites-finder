package com.essheva.assateague;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileReader;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * Created by Sheva on 3/8/2018.
 */
public class SiteAvailabilityMailer {

    private Set<Site> availableSites;
    private Address[] toAddress;
    private Address fromAddress;
    private Session session;

    SiteAvailabilityMailer(Properties prop, Set<Site> availableSites) throws AddressException {
        this.availableSites = availableSites;

        final String from = prop.getProperty("mail.from.user");
        this.fromAddress = new InternetAddress(from);

        String[] to = prop.getProperty("mail.to").trim().split("\\s*;\\s*");
        this.toAddress = new InternetAddress[to.length];
        for (int i = 0; i < to.length; i++) {
            toAddress[i] = new InternetAddress(to[i]);
        }

        this.session = Session.getDefaultInstance(prop, new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, prop.getProperty("mail.from.password"));
            }
        });
    }

    void sendEmail() throws MessagingException {

        Message message = new MimeMessage(session);

        message.setFrom(fromAddress);
        message.addRecipients(Message.RecipientType.TO, toAddress);
        message.setSubject("Assateague sites available!");

        String body = "<h4 class=\"green\">" + message.getSubject() + "</h4 >" +
                (availableSites.isEmpty() ? "<p>Nothing found</p>" : processAvailableDates());

        message.setContent(body, "text/html; charset=utf-8");
        message.saveChanges();

        Transport.send(message, toAddress);
    }

    private String processAvailableDates() {

        final StringBuilder sitesAvHtml = new StringBuilder();

        availableSites.forEach(s -> {
            sitesAvHtml.append("<tr>");
            sitesAvHtml.append("<td style=\"border: 1px solid black;\">").append(formHtmlSiteName(s)).append("</td>");
            sitesAvHtml.append("<td style=\"border: 1px solid black;\">").append(s.getLoopName()).append("</td>");
            sitesAvHtml.append("<td style=\"border: 1px solid black;\">").append(formatHtmlDatesForSite(s)).append("</td>");
            sitesAvHtml.append("</tr>");
        });

        return "<table style=\"border: 1px solid black;width:100%;border-collapse: collapse;\">" +
                "<tr style=\"color: green\">" +
                "<th>Site #</th>" +
                "<th>Facility Area</th>" +
                "<th>Available dates</th>" +
                "</tr>"
                + sitesAvHtml +
                "</table>";
    }

    private String formHtmlSiteName(Site site) {
        String siteNameText = site.getSiteName();
        if (site.getSiteLink() != null) {
            siteNameText = "<a href=\"" + site.getSiteLink().split("www.")[1] + "\">" + siteNameText + "</a>";
        }
        return siteNameText;
    }

    private StringBuilder formatHtmlDatesForSite(Site site) {
        StringBuilder dates = new StringBuilder();
        boolean isSameGroup = false;
        int colorIter = 0;
        LocalDate prevDate = null;
        final Iterator<LocalDate> iterator = site.getAvailableDates().iterator();
        while (iterator.hasNext() || prevDate != null) {
            if (!isSameGroup) {
                colorIter += 30;
                dates.append("<span style=\"background-color: rgb(").append(102 + colorIter).append(",").append(255 - colorIter).append(",204);\">");
                if (prevDate != null) {
                    dates.append(formatDateForMail(prevDate)).append("; ");
                }
            }

            if (iterator.hasNext()) {
                final LocalDate curDate = iterator.next();
                if (prevDate == null) {
                    isSameGroup = true;
                    dates.append(formatDateForMail(curDate)).append(";  ");
                } else {
                    if (prevDate.plus(1, ChronoUnit.DAYS).isEqual(curDate)) {
                        isSameGroup = true;
                        dates.append(formatDateForMail(curDate)).append("; ");
                    } else {
                        isSameGroup = false;
                        dates.append("</span>");
                    }
                }
                prevDate = curDate;
            } else {
                if (prevDate != null) {
                    dates.append("</span>");
                }
                prevDate = null;
            }
        }
        return dates;
    }

    private String formatDateForMail(LocalDate date) {
        return date.getDayOfWeek().toString().substring(0, 3) + "  " + date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    public static void main(String... args) throws Exception {
        Set<Site> sites = new TreeSet<>();
        Site site1 = new Site("G5", "Oceanside Group Sites");
        site1.setSiteLink("https//www.recreation.gov/camping/com.essheva.assateague-island-national-seashore-campground/r/campsiteDetails.do?" +
                "siteId=202328&amp;contractCode=NRSO&amp;parkId=70989");
        site1.addAvailableDate(LocalDate.of(2018, 3, 9));
        site1.addAvailableDate(LocalDate.of(2018, 3, 10));
        site1.addAvailableDate(LocalDate.of(2018, 3, 11));
        site1.addAvailableDate(LocalDate.of(2018, 3, 16));
        site1.addAvailableDate(LocalDate.of(2018, 3, 17));
        site1.addAvailableDate(LocalDate.of(2018, 3, 18));

        Site site2 = new Site("G4", "Oceanside Group Sites");
        site2.addAvailableDate(LocalDate.of(2018, 3, 9));
        site2.addAvailableDate(LocalDate.of(2018, 3, 10));
        site2.addAvailableDate(LocalDate.of(2018, 3, 11));
        site2.addAvailableDate(LocalDate.of(2018, 3, 16));
        site2.addAvailableDate(LocalDate.of(2018, 3, 17));
        site2.addAvailableDate(LocalDate.of(2018, 3, 18));
        site2.addAvailableDate(LocalDate.of(2018, 3, 16));
        site2.addAvailableDate(LocalDate.of(2018, 3, 23));
        site2.addAvailableDate(LocalDate.of(2018, 3, 24));
        sites.add(site1);
        sites.add(site2);

        Properties properties = new Properties();
        properties.load(new FileReader(Paths.get("src/main/resources/mail_default.properties").toFile()));
        properties.load(new FileReader(Paths.get("src/main/resources/user.secret").toFile()));

        new SiteAvailabilityMailer(properties, sites).sendEmail();
    }
}
