package ntou.soselab.tabot.Entity;

import java.util.Map;

public class StudentGrade {

    public String name;
    public String studentId;
    public Map<String, String> grades;


    public StudentGrade(String name, String studentId, Map<String, String> grades) {
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

    public Map<String, String> getGrades() {
        return grades;
    }

    public void setGrades(Map<String, String> grades) {
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
