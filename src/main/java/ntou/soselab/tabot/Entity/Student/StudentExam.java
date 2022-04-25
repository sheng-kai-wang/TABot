package ntou.soselab.tabot.Entity.Student;

import java.util.Map;

public class StudentExam extends Student {

    private Map<String, Boolean> examRecord;

    public StudentExam(String studentId) {
        super(studentId);
        this.examRecord = null;
    }

    public StudentExam(String name, String studentId, Map<String, Boolean> examRecord) {
        super(name, studentId);
        this.examRecord = examRecord;
    }

    public Map<String, Boolean> getExamRecord() {
        return examRecord;
    }

    public void setExamRecord(Map<String, Boolean> examRecord) {
        this.examRecord = examRecord;
    }

    @Override
    public String toString() {
        return "StudentGrade{" +
                "name='" + super.getName() + '\'' +
                ", studentId='" + super.getStudentId() + '\'' +
                ", examRecord=" + examRecord +
                '}';
    }
}
