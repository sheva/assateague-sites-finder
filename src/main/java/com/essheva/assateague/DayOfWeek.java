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

    public static String getTdSelector(DayOfWeek day) {
        final String tdSelector;
        switch (day) {
            case WED: tdSelector = "td[contains(@class, '" + SAT.getCSSClass() + "')]/preceding-sibling::td[3]"; break;
            case THU: tdSelector = "td[contains(@class, '" + SAT.getCSSClass() + "')]/preceding-sibling::td[2]"; break;
            case FRI: tdSelector = "td[contains(@class, '" + SAT.getCSSClass() + "')]/preceding-sibling::td[1]"; break;
            case SAT: tdSelector = "td[contains(@class, '" + SAT.getCSSClass() + "')]"; break;
            case SUN: tdSelector = "td[contains(@class, '" + SUN.getCSSClass() + "')]"; break;
            case MON: tdSelector = "td[contains(@class, '" + SUN.getCSSClass() + "')]/following-sibling::td[1]"; break;
            case TUE: tdSelector = "td[contains(@class, '" + SUN.getCSSClass() + "')]/following-sibling::td[2]"; break;
            default:
                throw new AssertionError(day);
        }
        return tdSelector;
    }
}