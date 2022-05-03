package ntou.soselab.tabot.Service.ExamService;

import ntou.soselab.tabot.Entity.Student.StudentExam;
import ntou.soselab.tabot.repository.Neo4jHandler;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * update exam data to neo4j.
 */
public class ExamUpdater {

    private Neo4jHandler neo4jHandler;

    /**
     * get neo4j handler
     */
    public ExamUpdater() {
        this.neo4jHandler = new Neo4jHandler("Java");
    }

    /**
     * neo4j exam data update method for external class.
     *
     * @param commonExam exam marked with the star.
     * @param allExamRecords all exam records.
     * @param examCorresponding exam correspondence table, like "24" to "1-1".
     */
    public void updateNeo4jExam(List<String> commonExam, List<StudentExam> allExamRecords, JSONObject examCorresponding) {
        for (StudentExam student : allExamRecords) {
            updateCommonExam(student, commonExam);
            // absented all exam, only give common exam.
            if (student.getExamRecord() == null) continue;
            updatePersonalizedExam(student, examCorresponding);
        }
    }

    /**
     * update common exam
     *
     * @param student one student's data.
     * @param commonExam common exam number.
     */
    public void updateCommonExam(StudentExam student, List<String> commonExam) {
        for (String s : commonExam) {
            if (student.getName() == null) {
                // absented all exam, and without student's name.
                neo4jHandler.updatePersonalizedExam(student.getStudentId(), s);
            } else {
                neo4jHandler.updatePersonalizedExam(student.getStudentId(), student.getName(), s);
            }
        }
    }

    /**
     * update personalized exam data
     *
     * @param student one student's data.
     * @param examCorresponding exam correspondence table, like "24" to "1-1".
     */
    public void updatePersonalizedExam(StudentExam student, JSONObject examCorresponding) {
        for (String examStatus : student.getExamRecord().keySet()) {
            // if the answer is correct, just skip
            if (student.getExamRecord().get(examStatus)) continue;
            for (String examKey : examCorresponding.keySet()) {
                // mappedExam is just like "1-1".
                String mappedExam = examCorresponding.get(examKey).toString().split("\"")[1];
                if (mappedExam.equals(examStatus)) {
                    neo4jHandler.updatePersonalizedExam(student.getStudentId(), student.getName(), examKey);
                }
            }
        }
    }
}
