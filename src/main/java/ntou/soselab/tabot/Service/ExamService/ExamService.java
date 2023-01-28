package ntou.soselab.tabot.Service.ExamService;

import ntou.soselab.tabot.Entity.Student.StudentExam;
import ntou.soselab.tabot.Repository.SheetsHandler;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@SpringBootApplication
@EnableScheduling
public class ExamService {

    private ExamUpdater examUpdater;

    // exam correspondence table, like "24" to "1-1".
    private JSONObject examCorresponding;
    private JSONObject examPublishable;
    private List<StudentExam> allExamRecords = new ArrayList<>();

    /**
     * get exam correspondence table
     */
    @Autowired
    public ExamService(ExamUpdater examUpdater) {
        SheetsHandler sheetsHandler = new SheetsHandler("course");
        this.examCorresponding = sheetsHandler.readContentByHeader("QuestionBank", "corresponding exam");
        this.examPublishable = sheetsHandler.readContentByHeader("QuestionBank", "publishable");
        this.examUpdater = examUpdater;
        updateNeo4jExam();
    }

    /**
     * neo4j exam data update method for external class.
     * <p>
     * execute every day.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void updateNeo4jExam() {
        List<String> commonExam = getCommonExam();
        this.allExamRecords = new ExamCrawler().getAllExamRecords();
        this.examUpdater.updateNeo4jExam(commonExam, allExamRecords, examCorresponding);
        System.out.println("[DEBUG][ExamService] update student's exam data to neo4j: " + allExamRecords);
    }

    /**
     * get common exam for all student.
     *
     * @return list of common exam number.
     */
    private List<String> getCommonExam() {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < examCorresponding.length(); i++) {
            // index to exam number
            String examNumber = String.valueOf(i+1);
            // ["*"] to *
            String correspondingValue = examCorresponding.get(examNumber).toString().split("\"")[1];
            // ["v"] to v
            String publishableValue = examPublishable.get(examNumber).toString().split("\"")[1];
            // add common and publishable exam
            if (correspondingValue.equals("*") && publishableValue.equals("v")) result.add(examNumber);
        }
        return result;
    }
}
