package ntou.soselab.tabot.Service.ExamService;

import ntou.soselab.tabot.Entity.Student.StudentExam;
import ntou.soselab.tabot.repository.SheetsHandler;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ExamService {

    // exam correspondence table, like "24" to "1-1".
    private JSONObject examCorresponding;
    private List<StudentExam> allExamRecords = new ArrayList<>();

    /**
     * get exam correspondence table
     */
    public ExamService() {
        this.examCorresponding = new SheetsHandler("Java")
                .readContentByHeader("QuestionBank", "corresponding exam / exam");
    }

    /**
     * neo4j exam data update method for external class.
     */
    public void updateNeo4jExam() {
        List<String> commonExam = getCommonExam();
        this.allExamRecords = new ExamCrawler().getAllExamRecords();
        new ExamUpdater().updateNeo4jExam(commonExam, allExamRecords, examCorresponding);
        System.out.println("[DEBUG][ExamService] update student's exam data to neo4j: " + allExamRecords);
    }

    /**
     * get common exam for all student.
     *
     * @return list of common exam number.
     */
    private List<String> getCommonExam() {
        List<String> result = new ArrayList<String>();
        for (Object o : examCorresponding.keySet()) {
            // remove header
            if (o.equals("題號 / num")) continue;
            // ["*"] to *
            String value = examCorresponding.get(o.toString()).toString().split("\"")[1];
            // common exam
            if (value.equals("*")) result.add(o.toString());
        }
        return result;
    }
}
