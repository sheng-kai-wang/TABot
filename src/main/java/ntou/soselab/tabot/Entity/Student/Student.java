package ntou.soselab.tabot.Entity.Student;

public class Student {
    private String name;
    private String studentId;

    public Student() {
    }

    public Student(String name, String studentId) {
        this.name = name;
        this.studentId = studentId;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final String getStudentId() {
        return studentId;
    }

    public final void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
