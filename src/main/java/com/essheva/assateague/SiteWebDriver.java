package com.essheva.assateague;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.Paths.get;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.openqa.selenium.By.*;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

/**
 *
 * Created by Sheva on 3/12/2018.
 */
public class SiteWebDriver {

    private final WebDriver webDriver;
    private final Wait<WebDriver> wait;

    SiteWebDriver(final String driverPath) {
        System.setProperty("webdriver.chrome.driver", get(driverPath).toFile().getAbsolutePath());

        ChromeOptions options = new ChromeOptions();
        options.addArguments("window-size=1500,1280");

        webDriver = new ChromeDriver(options);

        webDriver.get("https://www.recreation.gov/camping/Assateague-Island-National-Seashore-Campground/r/campsiteCalendar.do" +
                "?page=calendar&search=site&contractCode=NRSO&parkId=70989");

        wait = new WebDriverWait(webDriver, 60);
    }

    Set<Site> getAvailableSites(String loopName, SearchParams params) {
        try {
            final Set<Site> result = new TreeSet<>();
            Select loop = new Select(webDriver.findElement(id("loop")));
            loop.selectByVisibleText(loopName);

            wait.until(elementToBeClickable(id("filter")));

            WebElement filterButton = webDriver.findElement(id("filter"));
            Actions actions = new Actions(webDriver);
            actions.moveToElement(filterButton).click().perform();

            webDriver.findElement(id("campCalendar")).click();

            String stopCondition = getEndDateCondition(params.getStop());
            while (!stopCondition.equals(webDriver.findElement(id("calendar")).findElement(cssSelector("td[class='weeknav month']>span")).getText())) {
                WebElement table = webDriver.findElement(id("calendar"));
                table.findElements(cssSelector("tbody>tr:not([class*='separator'])")).forEach(row ->
                {
                    WebElement siteElem = row.findElement(cssSelector("div[class='siteListLabel']")).findElement(tagName("a"));

                    final Site site = getByNameOrNew(result, siteElem.getText(), loopName);
                    site.setSiteLink(siteElem.getAttribute("href"));

                    Map<Integer, Set<LocalDate>> candidates = getAvailableDateCandidates(params, row);
                    candidates.values().forEach(d -> site.addAvailableDates(getConsequentDatesRange(d, params.getMinLength())));

                    if (!site.getAvailableDates().isEmpty()) {
                        result.add(site);
                    }
                });

                WebElement nextWeek = webDriver.findElement(id("nextWeek"));
                new Actions(webDriver).moveToElement(nextWeek).click().perform();

                wait.until(visibilityOfElementLocated(id("calendar")));
            }

            return result;
        }
        finally {
            webDriver.quit();
        }
    }

    private Map<Integer, Set<LocalDate>> getAvailableDateCandidates(SearchParams params, WebElement row) {
        Map<Integer, Set<LocalDate>> candidates = new TreeMap<>();

        params.getDaysOfWeek().forEach(day ->  {

            final AtomicInteger group = new AtomicInteger(0);

            row.findElements(xpath(DayOfWeek.getTdSelector(day))).stream().
                    filter(e -> e.getAttribute("class").contains(" a")).
                    filter(e -> {
                        LocalDate date = getAvailableDateFromElement(e);
                        LocalDate start = params.getStart();
                        return date.isAfter(start) || date.isEqual(start);}).
                    forEach(e -> {
                            LocalDate date = getAvailableDateFromElement(e);
                            int groupId = group.getAndAdd(1);
                            if (candidates.containsKey(groupId)) {
                                candidates.get(groupId).add(date);
                            } else {
                                candidates.put(groupId, new TreeSet<LocalDate>() {{ add(date); }});
                            }
                    });
        });
        return candidates;
    }

    private static Set<LocalDate> getConsequentDatesRange(Set<LocalDate> input, int minLength) {
        Set<LocalDate> result = new TreeSet<>();
        if (input.size() < minLength) {
            return result;
        }

        int count = 0;
        LocalDate prev = null;
        Set<LocalDate> group = new HashSet<>();
        for (final LocalDate cur : input) {
            if (prev == null || prev.plus(1, ChronoUnit.DAYS).equals(cur)) {
                count++;
            } else {
                if (count >= minLength) {
                    result.addAll(group);
                }
                count = 1;
                group.clear();
            }

            group.add(cur);
            prev = cur;
        }

        if (count >= minLength && !group.isEmpty()) {
            result.addAll(group);
        }

        return result;
    }

    /**
     *
     * @param end date e.g. 2018-03-10
     * @return e.g. "Mar 2018"
     */
    private String getEndDateCondition(LocalDate end) {
        String monthInLowerCase = end.getMonth().toString().substring(0, 3).toLowerCase();
        String month = monthInLowerCase.substring(0, 1).toUpperCase() + monthInLowerCase.substring(1);
        return month + " " + end.getYear();
    }

    private LocalDate getAvailableDateFromElement(WebElement e) {
        String href = e.findElement(tagName("a")).getAttribute("href");
        Matcher matcher = Pattern.compile("arvdate=([\\d/]+)&").matcher(href);
        if (matcher.find()) {
            return LocalDate.parse(matcher.group(1), ofPattern("M/d/yyyy"));
        }
        throw new IllegalArgumentException(href);
    }

    private Site getByNameOrNew(Set<Site> availableSites, String siteName, String loopName) {
        return availableSites.parallelStream().filter(s ->
                s.getSiteName().equals(siteName)).findFirst().orElse(new Site(siteName, loopName));
    }

    public static void main(String[] args) {
        Set<LocalDate> setInput = new TreeSet<>();
        setInput.add(LocalDate.of(2018, 3, 1));
        setInput.add(LocalDate.of(2018, 3, 4));
        setInput.add(LocalDate.of(2018, 3, 5));
        setInput.add(LocalDate.of(2018, 3, 6));
        setInput.add(LocalDate.of(2018, 3, 8));
        setInput.add(LocalDate.of(2018, 3, 9));
        setInput.add(LocalDate.of(2018, 3, 11));
        Set<LocalDate> filterd = getConsequentDatesRange(setInput, 2);
        System.out.println(filterd);
    }
}
