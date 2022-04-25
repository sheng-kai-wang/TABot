package ntou.soselab.tabot.Entity.Student;

import java.util.HashMap;

public class StudentDiscordProfile extends Student {

    private String discordId;

    public StudentDiscordProfile(String name, String studentId, String discordId){
        super(name, studentId);
//        super.setName(name);
//        super.setStudentId(studentId);
        this.discordId = discordId;
    }

    public StudentDiscordProfile(HashMap map){
        super((String) map.get("name"), (String) map.get("studentId"));
        this.discordId = (String)map.get("discordId");
    }

    public HashMap<String, String> getProfileMap(){
        HashMap<String, String> profileMap = new HashMap<>();
        profileMap.put("name", super.getName());
        profileMap.put("studentId", super.getStudentId());
        profileMap.put("discordId", this.discordId);
        return profileMap;
    }

    public String getUserFullName(){
        return super.getStudentId() + " - " + super.getName();
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
                "name='" + super.getName() + '\'' +
                ", studentId='" + super.getStudentId() + '\'' +
                ", discordId='" + discordId + '\'' +
                '}';
    }
}
