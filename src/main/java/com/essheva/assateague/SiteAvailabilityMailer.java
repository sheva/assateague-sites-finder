package com.essheva.assateague;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 *
 * Created by Sheva on 3/8/2018.
 */
public class SiteAvailabilityMailer {

    private static final char PROPERTY_VALUES_SEPARATOR = ';';

    private final Set<Site> availableSites;
    private final Address[] toAddress;
    private final Address fromAddress;
    private final Session session;

    SiteAvailabilityMailer(Properties props, Set<Site> availableSites) throws AddressException {
        this.availableSites = availableSites;

        final String from = props.getProperty("mail.from.user");
        fromAddress = new InternetAddress(from);

        final String toList = props.getProperty("mail.to");
        if (toList != null) {
            toAddress = Arrays.stream(toList.split("\\s*" + PROPERTY_VALUES_SEPARATOR + "\\s*")).
                    map(address -> {
                        try {
                            return new InternetAddress(address);
                        } catch (AddressException e) {
                           throw new RuntimeException(e);
                        }
                    }).toArray(Address[]::new);
        } else {
            synchronized (System.out) {
                System.out.println("Using 'mail.from.user' as mail recipient address.");
            }
            toAddress = new InternetAddress[]{ new InternetAddress(from)};
        }

        session = Session.getDefaultInstance(props, new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, props.getProperty("mail.from.password"));
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

        final StringBuilder sitesHtml = new StringBuilder();

        availableSites.forEach(s -> {
            sitesHtml.append("<tr>");
            sitesHtml.append("<td style=\"border: 1px solid black;\">").append(formHtmlSiteName(s)).append("</td>");
            sitesHtml.append("<td style=\"border: 1px solid black;\">").append(s.getLoopName()).append("</td>");
            sitesHtml.append("<td style=\"border: 1px solid black;\">").append(formatHtmlDatesForSite(s)).append("</td>");
            sitesHtml.append("</tr>");
        });

        return "<table style=\"border: 1px solid black;width:100%;border-collapse: collapse;\">" +
                "<tr style=\"color: green\">" +
                "<th>Site #</th>" +
                "<th>Facility Area</th>" +
                "<th>Available dates</th>" +
                "</tr>"
                + sitesHtml +
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
        final StringBuilder result = new StringBuilder();
        boolean isSameGroup = false;
        int colorIter = 0;
        LocalDate prevDate = null;
        final Iterator<LocalDate> iterator = site.getAvailableDates().iterator();
        while (iterator.hasNext() || prevDate != null) {
            if (!isSameGroup) {
                colorIter += 30;
                result.append("<span style=\"background-color: rgb(").append(102 + colorIter).append(",").append(255 - colorIter).append(",204);\">");
                if (prevDate != null) {
                    result.append(formatDateForMail(prevDate)).append("; ");
                }
            }

            if (iterator.hasNext()) {
                final LocalDate curDate = iterator.next();
                if (prevDate == null) {
                    isSameGroup = true;
                    result.append(formatDateForMail(curDate)).append(";  ");
                } else {
                    if (prevDate.plus(1, ChronoUnit.DAYS).isEqual(curDate)) {
                        isSameGroup = true;
                        result.append(formatDateForMail(curDate)).append("; ");
                    } else {
                        isSameGroup = false;
                        result.append("</span>");
                    }
                }
                prevDate = curDate;
            } else {
                if (prevDate != null) {
                    result.append("</span>");
                }
                prevDate = null;
            }
        }
        return result;
    }

    private String formatDateForMail(LocalDate date) {
        return date.getDayOfWeek().toString().substring(0, 3) + "  " + date.getMonthValue() + "/" + date.getDayOfMonth();
    }
}
