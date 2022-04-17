package ntou.soselab.tabot.Service.CrawlService;

import ntou.soselab.tabot.Entity.StudentGrade;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.security.Key;
import java.util.*;

@Service
public class GradesCrawler {

    private WebDriver driver;
    private List<String> gradeHeaders = new ArrayList<String>();
    private List<StudentGrade> StudentGradeList = new ArrayList<StudentGrade>();

    @Autowired
    public GradesCrawler(Environment env) {
        String gradesUrl = env.getProperty("tronclass.grades.url");
        String username = env.getProperty("tronclass.account.username");
        String password = env.getProperty("tronclass.account.password");

        System.setProperty("webdriver.chrome.driver", Objects.requireNonNull(env.getProperty("chromedriver.path")));
        login(gradesUrl, username, password);
    }

    private void login(String gradesUrl, String username, String password) {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--no-sandbox");
        driver = new ChromeDriver(chromeOptions);
//        driver = new ChromeDriver();

        driver.get(gradesUrl);

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.name("submit")).submit();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void getGrades() {
        // get the list of table headers
        gradeHeaders.add(driver.findElement(By.cssSelector("div.column.member")).getText());
        List<WebElement> headers = driver.findElements(By.cssSelector("ul.activity-list > li a"));
        for (WebElement e : headers) {
            String title = e.getText();
            if (title.isEmpty()) title = e.getAttribute("title");
            gradeHeaders.add(title);
        }

//        span ng-bind="student.name"
        List<WebElement> names = driver.findElements(By.cssSelector("span[ng-bind='student.name']"));
        List<WebElement> studentIDs = driver.findElements(By.cssSelector("span[ng-bind='student.user_no']"));
        List<WebElement> scores = driver.findElements(By.cssSelector("div.activity-body.sync-scroll > ul > li"));

        for (int i=0; i<names.size(); i++) {
            String name = names.get(i).getText();
            String studentID = studentIDs.get(i).getText();
            List<WebElement> scoreList = scores.get(i).findElements(By.cssSelector("div.score"));
            Map<String, String> scoreMap = new HashMap<>();
            for (int j=0; j<scoreList.size(); j++) {
                scoreMap.put(gradeHeaders.get(j+1), scoreList.get(j).getText());
            }
            StudentGradeList.add(new StudentGrade(name, studentID, scoreMap));
        }
        driver.quit();
    }

    public void updateSheet() {
    }
}
