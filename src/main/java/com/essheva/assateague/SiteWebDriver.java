package com.essheva.assateague;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM/d/yyyy");

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
        WebElement datePicker = webDriver.findElement(id("single-date-picker"));
        focusToElement(datePicker);
        datePicker.sendKeys(conf.getSearchStart().format(DateTimeFormatter.ofPattern(Configuration.DATE_FORMAT)));
        blurToElement(datePicker);
        webDriver.findElement(cssSelector("button.rec-button-link-small")).click();

        final Set<Site> result = new TreeSet<>();

        while(!checkStopCondition()) {

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

            scrollToElement(webDriver.findElement(xpath(
                    String.format("//table[@id='availability-table']//tbody//td[text()='%s']", loopName))));

            final String monthYear = webDriver.findElement(cssSelector("div.rec-month-availability-date-title")).getText();

            webDriver.findElement(id("availability-table")).findElements(cssSelector("tbody>tr")).stream().
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
                                        getAvailableDateCandidates(rowE, monthYear), conf.getMinLength());
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

    private Set<LocalDate> getAvailableDateCandidates(WebElement row, String monthYear) {
        Set<LocalDate> candidates = new TreeSet<>();
        row.findElements(cssSelector("td")).stream().
                filter(e -> e.getAttribute("class").equals("available")
                        || e.getAttribute("class").equals("walk-up")).
                filter(e -> {
                    LocalDate date = getAvailableDate(e, monthYear);
                    LocalDate start = conf.getSearchStart();
                    LocalDate stop = conf.getSearchStop();
                    boolean isInRange = (date.isAfter(start) && date.isBefore(stop))
                            || date.isEqual(start) || date.isEqual(stop);
                    boolean isDesiredDayOfWeek = conf.isDesiredDayOfWeek(date);
                    return isInRange && isDesiredDayOfWeek;
                }).
                forEach(e -> {
                    LocalDate date = getAvailableDate(e, monthYear);
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

    private boolean checkStopCondition() {
        final String monthYear = webDriver.findElement(cssSelector("div.rec-month-availability-date-title")).getText();
        LocalDate date = LocalDate.parse(defineDateForDay(monthYear, findLastDate()), dateFormatter);
        return conf.getSearchStop().isBefore(date) || conf.getSearchStop().isEqual(date);
    }

    private String defineDateForDay(String monthYear, String day) {
        String patternStr = ("([A-Z]{3})(\\s+/\\s+([A-Z]{3}))?\\s+(\\d{4})");
        Matcher matcher = Pattern.compile(patternStr).matcher(monthYear);
        if (matcher.find()) {
            String month = matcher.group(1);
            if (matcher.group(2) != null) {
                String firstDay = findDayFromTableHeader(3);
                if (Integer.valueOf(firstDay) > Integer.valueOf(day)) {
                    month = matcher.group(3);
                } else {
                    month = matcher.group(1);
                }
            }
            month = month.substring(0, 1) + month.substring(1).toLowerCase();
            String year = matcher.group(4);
            return month + "/" + day + "/" + year;
        }  else {
            throw new RuntimeException("Something has been changed in format");
        }
    }

    private LocalDate getAvailableDate(WebElement e, String monthYear) {
        int cellIndex = Integer.valueOf(e.getAttribute("cellIndex"));
        String dateStr = defineDateForDay(monthYear, findDayFromTableHeader(cellIndex + 1));
        return LocalDate.parse(dateStr, dateFormatter);
    }

    private String findDayFromTableHeader(int index) {
        return webDriver.findElement(cssSelector(String.format(
                "table[id='availability-table'] thead th:nth-of-type(%s) span[class='date']", index))).
                getText();
    }

    private Site getByNameOrNew(Set<Site> availableSites, String siteName, String loopName) {
        return availableSites.parallelStream().filter(s ->
                s.getSiteName().equals(siteName)).findFirst().orElse(new Site(siteName, loopName));
    }

    private void scrollToElement(WebElement e) {
        ((JavascriptExecutor)webDriver).executeScript("arguments[0].scrollIntoView()", e);
    }
    private void focusToElement(WebElement e) {
        ((JavascriptExecutor)webDriver).executeScript("arguments[0].focus()", e);
    }
    private void blurToElement(WebElement e) {
        ((JavascriptExecutor)webDriver).executeScript("arguments[0].blur()", e);
    }
    private String findLastDate() {
        return webDriver.findElement(
                xpath("//table[@id='availability-table']//thead//th[last()]//span[@class='date']")).getText();
    }
}
