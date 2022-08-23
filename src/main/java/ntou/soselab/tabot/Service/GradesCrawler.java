package ntou.soselab.tabot.Service;

import ntou.soselab.tabot.Entity.Student.StudentGrade;
import ntou.soselab.tabot.repository.SheetsHandler;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * automatically periodically crawl student grades from tronclass and update to google sheets.
 */
@Service
@SpringBootApplication
@EnableScheduling
public class GradesCrawler {

    private String gradesUrl;
    private String username;
    private String password;
    private List<StudentGrade> grades;
    private WebDriver driver;

    // the header of the grade date in the table
    private List<String> gradeHeaders = new ArrayList<String>();

    /**
     * automatically get students grades as long as it is constructed
     *
     * @param env it will be autowired by IOC.
     */
    @Autowired
    public GradesCrawler(Environment env) {
        this.gradesUrl = env.getProperty("tronclass.grades.url");
        this.username = env.getProperty("tronclass.account.username");
        this.password = env.getProperty("tronclass.account.password");

        System.setProperty("webdriver.chrome.driver", Objects.requireNonNull(env.getProperty("chromedriver.path")));
//        this.grades = getGrades();
    }

    /**
     * execute every hour
     */
//    @Scheduled(cron = "0 0 * * * *")
    public void updateSheet() {
        List<List<Object>> gradeList = new ArrayList<>();
        gradeList.add(new ArrayList<Object>(gradeHeaders));
        for (StudentGrade student : grades) {
            ArrayList<Object> tableRow = new ArrayList<Object>();
            tableRow.add(student.getStudentId());
            tableRow.addAll(student.getGrades());
            gradeList.add(tableRow);
        }
        new SheetsHandler("course").updateContent("Grades", "A1", gradeList);
        System.out.println("[DEBUG][GradesCrawler] update student's course grades on Google sheets");
    }

    /**
     * login to tronclass
     *
     * @param gradesUrl grades page of java course
     * @param username  temporarily use the student ID of sheng-kai-wang
     * @param password  temporarily use the password of sheng-kai-wang
     */
    private void login(String gradesUrl, String username, String password) {
        // use headless mode without open the browser window
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--no-sandbox");
        driver = new ChromeDriver(chromeOptions);
//        driver = new ChromeDriver();

        driver.get(gradesUrl);

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.name("submit")).submit();

        // waiting to render the page
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * use selenium to crawl tronclass scores
     *
     * @return list of student grades
     */
    public List<StudentGrade> getGrades() {

        login(gradesUrl, username, password);

        // get the list of table headers
        gradeHeaders.add(driver.findElement(By.cssSelector("div.column.member")).getText());
        List<WebElement> headers = driver.findElements(By.cssSelector("ul.activity-list > li a"));
        for (WebElement e : headers) {
            String title = e.getText();
            if (title.isEmpty()) title = e.getAttribute("title");
            gradeHeaders.add(title);
        }

        // get the student data web element
        List<WebElement> names = driver.findElements(By.cssSelector("span[ng-bind='student.name']"));
        List<WebElement> studentIDs = driver.findElements(By.cssSelector("span[ng-bind='student.user_no']"));
        List<WebElement> scoreBars = driver.findElements(By.cssSelector("div.activity-body.sync-scroll > ul > li"));

        // construct StudentGrade and put in the list
        List<StudentGrade> allStudentGrade = new ArrayList<StudentGrade>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i).getText();
            String studentID = studentIDs.get(i).getText();
            List<WebElement> scores = scoreBars.get(i).findElements(By.cssSelector("div.score"));
            List<String> scoreList = new ArrayList<>();
            for (WebElement s : scores) {
                scoreList.add(s.getText());
            }
            allStudentGrade.add(new StudentGrade(name, studentID, scoreList));
        }
        driver.quit();
        return allStudentGrade;
    }
}
