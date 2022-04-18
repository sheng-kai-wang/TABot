package ntou.soselab.tabot.Service.CrawlService;

import ntou.soselab.tabot.Entity.StudentGrade;
import ntou.soselab.tabot.repository.SheetsHandler;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GradesCrawler {

    private String gradesUrl;
    private String username;
    private String password;
    private List<StudentGrade> grades;
    private WebDriver driver;
    private List<String> gradeHeaders = new ArrayList<String>();

    @Autowired
    public GradesCrawler(Environment env) {
        this.gradesUrl = env.getProperty("tronclass.grades.url");
        this.username = env.getProperty("tronclass.account.username");
        this.password = env.getProperty("tronclass.account.password");

        System.setProperty("webdriver.chrome.driver", Objects.requireNonNull(env.getProperty("chromedriver.path")));
        this.grades = getGrades();
    }

    // bug
    public void updateSheet() {
        List<List<Object>> gradeList = new ArrayList<>();
        gradeList.add(new ArrayList<Object>(gradeHeaders));
        for (StudentGrade student : grades) {
            ArrayList<Object> tableRow = new ArrayList<Object>();
            tableRow.add(student.getStudentId());
            tableRow.addAll(student.getGrades());
            gradeList.add(tableRow);
        }
        new SheetsHandler("Java").updateContent("Grades Test", "A1", gradeList);
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

        List<WebElement> names = driver.findElements(By.cssSelector("span[ng-bind='student.name']"));
        List<WebElement> studentIDs = driver.findElements(By.cssSelector("span[ng-bind='student.user_no']"));
        List<WebElement> scores = driver.findElements(By.cssSelector("div.activity-body.sync-scroll > ul > li"));

        List<StudentGrade> allStudentGrade = new ArrayList<StudentGrade>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i).getText();
            String studentID = studentIDs.get(i).getText();
            List<WebElement> score = scores.get(i).findElements(By.cssSelector("div.score"));
            List<String> scoreList = new ArrayList<>();
            for (WebElement s : score) {
                scoreList.add(s.getText());
            }
            allStudentGrade.add(new StudentGrade(name, studentID, scoreList));
        }
        driver.quit();
        return allStudentGrade;
    }
}
