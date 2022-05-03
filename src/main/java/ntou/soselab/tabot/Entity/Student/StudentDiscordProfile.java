package ntou.soselab.tabot.Entity.Student;

import java.util.HashMap;

public class StudentDiscordProfile {

    private Student student = new Student();
    private String discordId;

    public StudentDiscordProfile(String name, String studentId, String discordId){
//        this.student = new Student(name, studentId);
//        this.student.name = name;
        this.student.setName(name);
        this.student.setStudentId(studentId);
        this.discordId = discordId;
    }

    public StudentDiscordProfile(HashMap map){
        this.student.setName((String) map.get("name"));
        this.student.setName((String) map.get("studentId"));
        this.discordId = (String)map.get("discordId");
    }

    public HashMap<String, String> getProfileMap() {
        HashMap<String, String> profileMap = new HashMap<>();
        profileMap.put("name", this.student.getName());
        profileMap.put("studentId", this.student.getStudentId());
        profileMap.put("discordId", this.discordId);
        return profileMap;
    }

    public String getUserFullName() {
        return this.student.getStudentId() + " - " + this.student.getName();
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

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getDiscordId() {
        return discordId;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "name='" + this.student.getName() + '\'' +
                ", studentId='" + this.student.getStudentId() + '\'' +
                ", discordId='" + discordId + '\'' +
                '}';
    }
}
