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
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.TreeSet;
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
public class SiteWebDriver implements AutoCloseable {

    private final WebDriver webDriver;
    private final Wait<WebDriver> wait;
    private final Configuration conf;

    SiteWebDriver(Configuration conf) {
        this.conf = conf;

        System.setProperty("webdriver.chrome.driver", get(conf.getDriverPath()).toFile().getAbsolutePath());

        ChromeOptions options = new ChromeOptions();
        options.addArguments("window-size=1500,1280");

        webDriver = new ChromeDriver(options);
        webDriver.get("https://www.recreation.gov/camping/Assateague-Island-National-Seashore-Campground/r/campsiteCalendar.do?page=calendar&search=site&contractCode=NRSO&parkId=70989");

        wait = new WebDriverWait(webDriver, 60);
    }

    Set<Site> getAvailableSites(String loopName) {
        loopFilterSubmit(loopName);
        return processSearchResults(loopName);
    }

    private Set<Site> processSearchResults(String loopName) {
        webDriver.findElement(id("campCalendar")).click();

        String stopCondition = getSearchStopCondition(conf.getSearchStop());
        final Set<Site> result = new TreeSet<>();
        while (!stopCondition.equals(webDriver.findElement(id("calendar")).findElement(cssSelector("td[class='weeknav month']>span")).getText())) {

            boolean hasNext;
            int pageCounter = 0;
            do {
                WebElement resultTable = webDriver.findElement(id("calendar"));
                resultTable.findElements(cssSelector("tbody>tr:not([class*='separator'])")).forEach(rowE ->
                {
                    final WebElement siteE = rowE.findElement(cssSelector("div[class='siteListLabel']")).findElement(tagName("a"));

                    Set<LocalDate> availableDates = getConsequentDatesRange(getAvailableDateCandidates(rowE), conf.getMinLength());
                    if (!availableDates.isEmpty()) {
                        final Site site = getByNameOrNew(result, siteE.getText(), loopName);
                        site.setSiteLink(siteE.getAttribute("href"));
                        site.addAvailableDates(availableDates);
                        result.add(site);
                    }
                });

                WebElement nextLinkE = webDriver.findElement(cssSelector("tfoot>tr>td>span[class='pagenav']>a[id^='resultNext_']"));

                hasNext = nextLinkE.getAttribute("href") != null;
                if (hasNext) {
                    click(nextLinkE);
                    wait.until(visibilityOfElementLocated(id("calendar")));
                    pageCounter++;
                }
            } while (hasNext);

            while(pageCounter-- > 0) {
                WebElement prevLinkE = webDriver.findElement(cssSelector("tfoot>tr>td>span[class='pagenav']>a[id^='resultPrevious_']"));
                wait.until(visibilityOfElementLocated(id("calendar")));
                click(prevLinkE);
            }

            WebElement nextWeekE = webDriver.findElement(id("nextWeek"));
            click(nextWeekE);

            wait.until(visibilityOfElementLocated(id("calendar")));
        }

        return result;
    }

    private void loopFilterSubmit(String loopName) {
        final Select loop = new Select(webDriver.findElement(id("loop")));
        loop.selectByVisibleText(loopName);

        wait.until(elementToBeClickable(id("filter")));

        WebElement buttonE = webDriver.findElement(id("filter"));
        click(buttonE);
    }

    @Override
    public void close() {
        webDriver.quit();
    }

    private Set<LocalDate> getAvailableDateCandidates(WebElement row) {
        Set<LocalDate> candidates = new TreeSet<>();
        row.findElements(cssSelector("td")).stream().
                filter(e -> e.getAttribute("class").contains(" a")).
                filter(e -> {
                    LocalDate date = getAvailableDateFromElement(e);
                    LocalDate start = conf.getSearchStart();
                    LocalDate stop = conf.getSearchStop();
                    boolean isInRange = (date.isAfter(start) && date.isBefore(stop)) || date.isEqual(start) || date.isEqual(stop);
                    boolean isDesiredDayOfWeek = conf.isDesiredDayOfWeek(date);
                    return isInRange && isDesiredDayOfWeek;
                }).
                forEach(e -> {
                    LocalDate date = getAvailableDateFromElement(e);
                    candidates.add(date);
                });
        return candidates;
    }

    private static Set<LocalDate>  getConsequentDatesRange(Set<LocalDate> input, int minLength) {
        Set<LocalDate> result = new TreeSet<>();
        if (input.size() < minLength) {
            return result;
        }

        int count = 0;
        LocalDate prev = null;
        Set<LocalDate> datesOfGroup = new TreeSet<>();
        for (final LocalDate cur : input) {
            if (prev == null || prev.plus(1, ChronoUnit.DAYS).equals(cur)) {
                count++;
            } else {
                if (count >= minLength) {
                    result.addAll(datesOfGroup);
                }
                count = 1;
                datesOfGroup.clear();
            }

            datesOfGroup.add(cur);
            prev = cur;
        }

        if (count >= minLength && !datesOfGroup.isEmpty()) {
            result.addAll(datesOfGroup);
        }

        return result;
    }

    /**
     * Transform search stop date into site representation to make stop search.
     *
     * @param end date e.g. 2018-03-10
     * @return e.g. "Mar 2018"
     */
    private String getSearchStopCondition(LocalDate end) {
        Month endSearchMonth = (end.getDayOfMonth() < 14) ? end.getMonth() : end.plusMonths(1).getMonth();
        String monthInLowerCase = endSearchMonth.toString().substring(0, 3).toLowerCase();
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

    private void click(WebElement element) {
        new Actions(webDriver).moveToElement(element).click().perform();
    }
}
