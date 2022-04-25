package ntou.soselab.tabot;

import ntou.soselab.tabot.Service.ExamService.ExamCrawler;
import ntou.soselab.tabot.Service.ExamService.ExamService;
import org.junit.jupiter.api.Test;

public class ExamServiceTest {

    /**
     * test for getting all exam records from Google sheets.
     */
    @Test
    public void getExamRecordsTest() {
        System.out.println("new ExamService().getAllExamRecords(): " + new ExamCrawler().getAllExamRecords());
    }

    /**
     * test for crawling data from Google sheets, and update to neo4j.
     */
    @Test
    public void examUpdateTest() {
        new ExamService().updateNeo4jExam();
    }
}
