package ntou.soselab.tabot.Service.ExamService;

import ntou.soselab.tabot.Entity.Student.StudentExam;
import ntou.soselab.tabot.Repository.SheetsHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * get the student's answer status on Google sheets, and update it to neo4j.
 */
public class ExamCrawler {

    private SheetsHandler examSheetsHandler;
    private SheetsHandler courseSheetsHandler;
    private List<StudentExam> allExamRecords = new ArrayList<>();
    private static final int WAIT_FOR_GOOGLE_SHEETS_API_LIMIT = 3000;

    /**
     * construct "SheetsHandler" for course data and exam data.
     */
    public ExamCrawler() {
        this.courseSheetsHandler = new SheetsHandler("course");
        this.examSheetsHandler = new SheetsHandler("exam");
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
     */
    private void readExamRecords() {
        JSONArray studentIds = new JSONArray(courseSheetsHandler.readContent("Grades", "A:A"));

        // "i=1" is for skip header
        for (int i = 1; i < studentIds.length(); i++) {

            // e.g. ["00457122"] to 00457122
            String studentId = studentIds.get(i).toString().split("\"")[1];
            StudentExam studentExam = new StudentExam(studentId);
            int examIndex = 0;
            for (String sheetTitle : examSheetsHandler.readSheetsTitles()) {
                examIndex++;
                JSONObject studentData = examSheetsHandler.readContentByKey(sheetTitle, studentId);
                // there is no such data on Google sheets
                if (studentData.isEmpty()) putOneSheetsAbsentRecord(examIndex, sheetTitle, studentExam);
                else putOneSheetsRecord(examIndex, studentData, studentExam);

                // avoid exceeding the Google sheets api call limit
                try {
                    Thread.sleep(WAIT_FOR_GOOGLE_SHEETS_API_LIMIT);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            System.out.println(studentExam);
            allExamRecords.add(studentExam);
        }
    }

    /**
     * get the information of one sheets
     *
     * @param examIndex   the index of exam
     * @param studentData one sheet's student data
     * @param studentExam the entity for storage student's exam data
     */
    private void putOneSheetsRecord(int examIndex, JSONObject studentData, StudentExam studentExam) {
        // the first time get data on Google sheets, maybe absent the first exam.
        if (studentExam.getName() == null) putNameRecord(studentData, studentExam);
        putAnswerStatusRecord(examIndex, studentData, studentExam);
    }

    /**
     * if the student absents that exam, the answer is all wrong.
     *
     * @param examIndex   the index of exam
     * @param sheetTitle  the title of sheet
     * @param studentExam the entity for storage student's exam data
     */
    private void putOneSheetsAbsentRecord(int examIndex, String sheetTitle, StudentExam studentExam) {
        // count the number of questions using the question number
        JSONObject sheetData = examSheetsHandler.readContentByHeader(sheetTitle, "題號");
        // get the quantity of exam in this sheet
        int questionLength = sheetData.get(sheetData.keys().next()).toString().split(",").length;
        for (int i = 0; i < questionLength; i++) {
            int questionIndex = i + 1;
            studentExam.getExamRecord().put(examIndex + "-" + questionIndex, false);
        }
    }

    /**
     * just put the name
     *
     * @param studentData the student's raw exam data on Google sheets
     * @param studentExam the entity for storage student's exam data
     */
    private void putNameRecord(JSONObject studentData, StudentExam studentExam) {
        String name = studentData.get("姓名").toString().split("\"")[1];
        studentExam.setName(name);
    }

    /**
     * put answer status record
     *
     * @param examIndex   the index of exam
     * @param studentData the student's raw exam data on Google sheets
     * @param studentExam the entity for storage student's exam data
     */
    private void putAnswerStatusRecord(int examIndex, JSONObject studentData, StudentExam studentExam) {
        int questionIndex = 0;
        for (Object o : new JSONArray(studentData.get("狀態").toString())) {
            questionIndex++;
            // e.g. 1-1 1-2 1-3....
            studentExam.getExamRecord().put(examIndex + "-" + questionIndex, o.toString().equals("答對"));
        }
    }
}