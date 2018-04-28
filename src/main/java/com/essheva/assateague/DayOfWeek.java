package com.essheva.assateague;

/**
 *
 * Created by Sheva on 3/12/2018.
 */
public enum DayOfWeek {
    SUN("sun"), MON(""), TUE(""), WED(""), THU(""), FRI(""), SAT("sat");

    private String cssClass;

    DayOfWeek(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getCSSClass() {
        return cssClass;
    }
}