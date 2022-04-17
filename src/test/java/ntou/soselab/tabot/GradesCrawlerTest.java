package ntou.soselab.tabot;

import ntou.soselab.tabot.Service.CrawlService.GradesCrawler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
public class GradesCrawlerTest {

    @Autowired
    Environment env;

    @Test
    public void getGradesTest() {
        new GradesCrawler(env).getGrades();
    }
}
