package com.essheva.assateague;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * Created by Sheva on 3/8/2018.
 */
public class Site implements Cloneable, Comparable<Site> {

    private String siteName;
    private String loopName;
    private String siteLink;
    private Set<LocalDate> availableDates = new TreeSet<>();

    Site(String name, String loopName) {
        this.siteName = name;
        this.loopName = loopName;
    }

    String getSiteName() {
        return siteName;
    }

    void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    String getLoopName() {
        return loopName;
    }

    void setLoopName(String loopName) {
        this.loopName = loopName;
    }

    String getSiteLink() {
        return siteLink;
    }

    void setSiteLink(String siteLink) {
        this.siteLink = siteLink;
    }

    Set<LocalDate> getAvailableDates() {
        return availableDates;
    }

    void setAvailableDates(Set<LocalDate> availableDates) {
        this.availableDates = availableDates;
    }

    void addAvailableDate(LocalDate date) {
        availableDates.add(date);
    }

    void addAvailableDates(Set<LocalDate> dates) {
        availableDates.addAll(dates);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return siteName.equals(site.siteName);
    }

    @Override
    public int compareTo(Site o) {
        return o.getSiteName().compareToIgnoreCase(this.getSiteName());
    }

    @Override
    public int hashCode() {
        return siteName.hashCode();
    }

    @Override
    public String toString() {
        return "Site{" +
                "siteName='" + siteName + '\'' +
                ", loopName='" + loopName + '\'' +
                ", siteLink='" + siteLink + '\'' +
                ", availableDates=" + availableDates +
                '}';
    }
}

