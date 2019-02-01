package com.essheva.assateague;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.Paths.get;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.openqa.selenium.By.*;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

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
        webDriver.get("https://www.recreation.gov/camping/Assateague-Island-National-Seashore-Campground/r/" +
                "campsiteCalendar.do?page=calendar&search=site&contractCode=NRSO&parkId=70989");

        wait = new WebDriverWait(webDriver, 60);
    }

    Set<Site> getAvailableSites(final String loopName) {
//        setAttrValue(webDriver.findElement(id("single-date-picker")), "value", conf.getSearchStartStr());
        webDriver.findElement(cssSelector(".rec-campground-availability-header")).click();

        final Set<Site> result = new TreeSet<>();

        while(!checkStopCondition(
                webDriver.findElement(cssSelector("div.rec-month-availability-date-title")).getText(),
                webDriver.findElement(xpath("//table[@id='availability-table']//thead//th[last()]//span[@class='date']")).getText())
                ) {

            for(;;) {
                try {
                    WebElement loadMoreBtn = webDriver.findElement(cssSelector("button.load-more-btn"));
                    scrollToElement(loadMoreBtn);
                    loadMoreBtn.click();
                    wait.until(elementToBeClickable(id("availability-table")));
                } catch (NoSuchElementException ignored) {
                    break;
                }
            }

            scrollToElement(webDriver.findElement(xpath(String.format("//table[@id='availability-table']//tbody//td[text()='%s']", loopName))));

            WebElement resultTable = webDriver.findElement(id("availability-table"));
            resultTable.findElements(cssSelector("tbody>tr")).stream().
                    filter(rowE ->
                    {
                        try {
                            return rowE.findElement(cssSelector("td.rec-site-loop")).getText().equals(loopName);
                        } catch (NoSuchElementException ignored) {
                            return false;
                        }
                    }).
                    forEach(rowE ->
                            {
                                final WebElement siteE = rowE.findElement(cssSelector("th button"));

                                Set<LocalDate> availableDates = getConsequentDatesRange(
                                        getAvailableDateCandidates(rowE), conf.getMinLength());
                                if (!availableDates.isEmpty()) {
                                    final Site site = getByNameOrNew(result, siteE.getText(), loopName);
//                            site.setSiteLink(siteE.getAttribute("href"));
                                    site.addAvailableDates(availableDates);
                                    result.add(site);
                                }
                            }
                    );
            scrollToElement(webDriver.findElement(cssSelector("div.rec-day-picker")));
            webDriver.findElement(xpath("//div[@class='rec-day-picker'] //button[last()]")).click();
        }
        return result;
    }

    @Override
    public void close() {
        webDriver.quit();
    }

    private Set<LocalDate> getAvailableDateCandidates(WebElement row) {
        Set<LocalDate> candidates = new TreeSet<>();
        String monthYear = webDriver.findElement(cssSelector("div.rec-month-availability-date-title")).getText();
        row.findElements(cssSelector("td")).stream().
                filter(e -> e.getAttribute("class").equals("available")).
                filter(e -> {
                    LocalDate date = getAvailableDateFromElement(e, transformMonthYear(monthYear));
                    LocalDate start = conf.getSearchStart();
                    LocalDate stop = conf.getSearchStop();
                    boolean isInRange = (date.isAfter(start) && date.isBefore(stop)) || date.isEqual(start) || date.isEqual(stop);
                    boolean isDesiredDayOfWeek = conf.isDesiredDayOfWeek(date);
                    return isInRange && isDesiredDayOfWeek;
                }).
                forEach(e -> {
                    LocalDate date = getAvailableDateFromElement(e, transformMonthYear(monthYear));
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

    private boolean checkStopCondition(String monthAvail, String lastDay) {
        String[] monthYear = transformMonthYear(monthAvail).split("/");
        String datePattern = "MMM/d/yyyy";
        LocalDate dateActual = LocalDate.parse(monthYear[0] + "/" + lastDay + "/" + monthYear[1],
                DateTimeFormatter.ofPattern(datePattern));
        return conf.getSearchStop().isBefore(dateActual) || conf.getSearchStop().isEqual(dateActual);
    }

    private String transformMonthYear(String monthAvail) {
        String patternStr = ("([A-Z]{3})(\\s+/\\s+([A-Z]{3}))?\\s+(\\d{4})");
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(monthAvail);
        if (matcher.find()) {
            String month = matcher.group(2) != null ? matcher.group(3) : matcher.group(1);
            month = month.substring(0, 1) + month.substring(1).toLowerCase();
            String year = matcher.group(4);
            return month + "/" + year;
        }  else {
            throw new RuntimeException("Something has been changed in format");
        }
    }

    private LocalDate getAvailableDateFromElement(WebElement e, String monthYear) {
        int cellIndex = Integer.valueOf(e.getAttribute("cellIndex"));

        String month = monthYear.split("/")[0];
        String year = monthYear.split("/")[1];
        String day = webDriver.findElement(cssSelector(String.format(
                "table[id='availability-table'] thead th:nth-of-type(%s) span[class='date']", cellIndex + 1))).
                getText();

        return LocalDate.parse(month + "/" + day + "/" + year, ofPattern("MMM/d/yyyy"));
    }

    private Site getByNameOrNew(Set<Site> availableSites, String siteName, String loopName) {
        return availableSites.parallelStream().filter(s ->
                s.getSiteName().equals(siteName)).findFirst().orElse(new Site(siteName, loopName));
    }

    private void click(WebElement element) {
        new Actions(webDriver).moveToElement(element).click().perform();
    }

    private void setAttrValue(WebElement element, String attrName, String attrValue) {
        JavascriptExecutor exec = (JavascriptExecutor) webDriver;

        String js = String.format("arguments[0].%s='%s'", attrName, attrValue);
        exec.executeScript(js, element);
    }

    private void scrollToElement(WebElement element) {
        JavascriptExecutor jse = (JavascriptExecutor)webDriver;
        jse.executeScript("arguments[0].scrollIntoView()", element);
    }
}
