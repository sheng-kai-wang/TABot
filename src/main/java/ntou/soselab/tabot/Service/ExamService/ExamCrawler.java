package ntou.soselab.tabot.Service.ExamService;

import ntou.soselab.tabot.Entity.Student.StudentExam;
import ntou.soselab.tabot.repository.SheetsHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * get the student's answer status on Google sheets, and update it to neo4j.
 */
@Service
@SpringBootApplication
@EnableScheduling
public class ExamCrawler {

    private SheetsHandler examSheetsHandler;
    private SheetsHandler courseSheetsHandler;
    private List<StudentExam> allExamRecords = new ArrayList<>();

    /**
     * construct "SheetsHandler" for course data and exam data.
     */
    public ExamCrawler() {
        this.courseSheetsHandler = new SheetsHandler("Java");
        this.examSheetsHandler = new SheetsHandler("Java-exam");
        readExamRecords();
    }

    /**
     * get all exam records for external class.
     *
     * @return all exam records
     */
    public List<StudentExam> getAllExamRecords() {
        return this.allExamRecords;
    }

    /**
     * get every student's exam record
     *
     * execute every day
     */
    @Scheduled(cron = "0 0 0 * * *")
    private void readExamRecords() {
        JSONArray studentIds = new JSONArray(courseSheetsHandler.readContent("Grades", "A:A"));

        // "i=1" is for skip header
        for (int i = 1; i < studentIds.length(); i++) {

            // e.g. ["00457122"] to 00457122
            String studentId = studentIds.get(i).toString().split("\"")[1];
            StudentExam studentExam = new StudentExam(studentId);
            int examIndex = 1;
            for (String sheetTitle : examSheetsHandler.readSheetsTitles()) {
                studentExam = putOneSheetsRecord(examIndex, sheetTitle, studentExam);
                examIndex++;
            }
//            System.out.println(studentExam);
            allExamRecords.add(studentExam);
        }
    }

    /**
     * get the information of one sheets
     *
     * @param examIndex the index of exam
     * @param sheetTitle the sheet's title like "exam01"
     * @param studentExam the entity for storage student's exam data
     * @return one student's exam data
     */
    public StudentExam putOneSheetsRecord(int examIndex, String sheetTitle, StudentExam studentExam) {

        JSONObject studentData = examSheetsHandler.readContentByKey(sheetTitle, studentExam.getStudentId());

        // avoid exceeding the Google sheets api call limit
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // there is no such data on Google sheets
        if (studentData.isEmpty()) return studentExam;

        // the first time get data on Google sheets
        if (examIndex == 1 || studentExam.getExamRecord() == null) {
            putNewRecord(examIndex, studentData, studentExam);

            // NOT the first time, so just update answer status data
        } else {
            int questionIndex = 1;
            for (Object o : new JSONArray(studentData.get("狀態").toString())) {
                studentExam.getExamRecord().put(examIndex + "-" + questionIndex, o.toString().equals("答對"));
                questionIndex++;
            }
        }

        return studentExam;
    }

    /**
     * put new data into "StudentExam"
     *
     * @param examIndex the index of exam
     * @param studentData the student's raw exam data on Google sheets
     * @param studentExam the entity for storage student's exam data
     */
    private void putNewRecord(int examIndex, JSONObject studentData, StudentExam studentExam) {
        String name = studentData.get("姓名").toString().split("\"")[1];
        Map<String, Boolean> answerStatus = new HashMap<>();

        // answer status
        int questionIndex = 0;
        for (Object o : new JSONArray(studentData.get("狀態").toString())) {
            questionIndex++;
            // e.g. 1-1 1-2 1-3....
            answerStatus.put(examIndex + "-" + questionIndex, o.toString().equals("答對"));
        }

        studentExam.setName(name);
        studentExam.setExamRecord(answerStatus);
    }
}