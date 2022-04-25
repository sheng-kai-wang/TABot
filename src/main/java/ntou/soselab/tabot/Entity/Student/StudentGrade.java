package ntou.soselab.tabot.Entity.Student;

import java.util.List;

public class StudentGrade extends Student {

    private List<String> grades;

    public StudentGrade(String name, String studentId, List<String> grades) {
        super(name, studentId);
        this.grades = grades;
    }

    public List<String> getGrades() {
        return grades;
    }

    public void setGrades(List<String> grades) {
        this.grades = grades;
    }

    @Override
    public String toString() {
        return "StudentGrade{" +
                "name='" + super.getName() + '\'' +
                ", studentId='" + super.getStudentId() + '\'' +
                ", grades=" + grades +
                '}';
    }
}
