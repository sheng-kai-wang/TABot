package ntou.soselab.tabot.Entity.Student;

import java.util.List;

public class StudentGrade {

    private Student student = new Student();
    private List<String> grades;

    public StudentGrade(String name, String studentId, List<String> grades) {
        this.student.setName(name);
        this.student.setStudentId(studentId);
        this.grades = grades;
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


    public List<String> getGrades() {
        return grades;
    }

    public void setGrades(List<String> grades) {
        this.grades = grades;
    }

    @Override
    public String toString() {
        return "StudentGrade{" +
                "name='" + this.student.getName() + '\'' +
                ", studentId='" + this.student.getStudentId() + '\'' +
                ", grades=" + grades +
                '}';
    }
}
