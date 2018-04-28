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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.essheva.assateague.DayOfWeek.SAT;
import static com.essheva.assateague.DayOfWeek.SUN;
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

    SiteWebDriver(Properties props) {
        System.setProperty("webdriver.chrome.driver", get(props.getProperty("web.driver.path")).toFile().getAbsolutePath());

        ChromeOptions options = new ChromeOptions();
        options.addArguments("window-size=1500,1280");

        this.webDriver = new ChromeDriver(options);

        webDriver.get("https://www.recreation.gov/camping/Assateague-Island-National-Seashore-Campground/r/campsiteCalendar.do?page=calendar&search=site&contractCode=NRSO&parkId=70989");
        this.wait = new WebDriverWait(webDriver, 60);
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
        Map<Integer, Set<LocalDate>> candidates = new HashMap<>();
        for (DayOfWeek day : params.getDaysOfWeek()) {

            int group = 0;
            final String tdSelector = getTdSelector(day);
            List<WebElement> elements = row.findElements(xpath(tdSelector));
            for (WebElement e : elements) {

                LocalDate date = getAvailableDateFromElement(e);
                LocalDate start = params.getStart();

                if (date.isAfter(start) || date.isEqual(start)) {
                    if (candidates.containsKey(group)) {
                        candidates.get(group).add(date);
                    } else {
                        candidates.put(group, new TreeSet<LocalDate>(){{add(date);}});
                    }
                }
                group++;
            }
        }
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
        String monthInLower = end.getMonth().toString().substring(0, 3).toLowerCase();
        String month = monthInLower.substring(0, 1).toUpperCase() + monthInLower.substring(1);
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

    private String getTdSelector(DayOfWeek day) {
        final String tdSelector;
        switch (day) {
            case WED:
                tdSelector = "td[@class='status a " + SAT.getCSSClass() + "']/preceding-sibling::td[3]"; break;
            case THU:
                tdSelector = "td[@class='status a " + SAT.getCSSClass() + "']/preceding-sibling::td[2]"; break;
            case FRI:
                tdSelector = "td[@class='status a " + SAT.getCSSClass() + "']/preceding-sibling::td[1]"; break;
            case SAT:
                tdSelector = "td[@class='status a " + SAT.getCSSClass() + "']"; break;
            case SUN:
                tdSelector = "td[@class='status a " + SUN.getCSSClass() + "']"; break;
            case MON:
                tdSelector = "td[@class='status a " + SUN.getCSSClass() + "']/following-sibling::td[1]"; break;
            case TUE:
                tdSelector = "td[@class='status a " + SUN.getCSSClass() + "']/following-sibling::td[2]"; break;
            default:
                throw new AssertionError(day);
        }
        return tdSelector;
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
