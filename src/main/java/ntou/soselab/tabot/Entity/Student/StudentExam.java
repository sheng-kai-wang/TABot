package ntou.soselab.tabot.Entity.Student;

import java.util.HashMap;
import java.util.Map;

public class StudentExam {

    private Student student = new Student();
    private Map<String, Boolean> examRecord = new HashMap<>();

    public StudentExam(String studentId) {
        this.student.setStudentId(studentId);
    }

    public StudentExam(String name, String studentId, Map<String, Boolean> examRecord) {
        this.student.setName(name);
        this.student.setStudentId(studentId);
        this.examRecord = examRecord;
    }

    public void setName(String name) {
        this.student.setName(name);
    }

    public String getName() {
        return this.student.getName();
    }

    public void setStudentId(String studentId) {
        this.student.setStudentId(studentId);
    }

    public String getStudentId() {
        return this.student.getStudentId();
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
                "name='" + this.student.getName() + '\'' +
                ", studentId='" + this.student.getStudentId() + '\'' +
                ", examRecord=" + examRecord +
                '}';
    }
}
