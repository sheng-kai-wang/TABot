package ntou.soselab.tabot.Entity;

import java.util.List;

public class StudentGrade {

    private String name;
    private String studentId;
    private List<String> grades;


    public StudentGrade(String name, String studentId, List<String> grades) {
        this.name = name;
        this.studentId = studentId;
        this.grades = grades;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
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
                "name='" + name + '\'' +
                ", studentId='" + studentId + '\'' +
                ", grades=" + grades +
                '}';
    }
}
