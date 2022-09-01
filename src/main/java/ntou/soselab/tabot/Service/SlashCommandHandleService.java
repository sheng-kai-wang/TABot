package ntou.soselab.tabot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import ntou.soselab.tabot.repository.Neo4jHandler;
import ntou.soselab.tabot.repository.RedisHandler;
import ntou.soselab.tabot.repository.SheetsHandler;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SlashCommandHandleService {
    @Autowired
    Neo4jHandler neo4jHandler;
    @Autowired
    RedisHandler redisHandler;
    private static HashMap<String, JsonObject> ongoingQuizMap;

    private final String anonymousQuestionChannelUrl;
    private final String userRequirementsFolderPath;
    public final String NO_GROUP = "no group";
    public final String NOT_STUDENT = "not student";
    private final String DOWN_ARROW = "↓";

    @Autowired
    public SlashCommandHandleService(Environment env) {
        ongoingQuizMap = new HashMap<>();
        this.userRequirementsFolderPath = env.getProperty("user-requirements.folder.path");
        this.anonymousQuestionChannelUrl = env.getProperty("discord.channel.anonymous-question.url");
    }

    public static HashMap<String, JsonObject> getOngoingQuizMap() {
        return ongoingQuizMap;
    }

    public Message getAnonymousQuestionResponse(String question) {
        MessageBuilder mb = new MessageBuilder();
        mb.append("ok, got it.\n");
        mb.append("Your question is `").append(question).append("`.\n");
        mb.setEmbeds(new EmbedBuilder()
                .addField("Click the link below to view", "[anonymous-question](" + anonymousQuestionChannelUrl + ")", false)
                .build());
        return mb.build();
    }

    public Message readPpt() {
        MessageBuilder mb = new MessageBuilder();
        Map<String, String> allSlideshowMap = neo4jHandler.readAllSlideshow();
        if (allSlideshowMap == null || allSlideshowMap.isEmpty()) {
            System.out.println("[WARNING] No course ppt are available yet.");
            mb.append("```[WARNING] Sorry, No course ppt are available yet.```");
            return mb.build();
        }
        mb.append("Here you are ! :grinning:\n");
        EmbedBuilder eb = new EmbedBuilder();
        // sort by key (chapter name)
        allSlideshowMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(map -> {
                    eb.addField("", "[" + map.getKey() + "](" + map.getValue() + ")", false);
                });
        mb.setEmbeds(eb.build());
        return mb.build();
    }

    public Message personalQuiz(String studentId) {
        MessageBuilder mb = new MessageBuilder();
        if (studentId.equals(NOT_STUDENT)) {
            System.out.println("[WARNING] there isn't registered as a student role.");
            mb.append("```[WARNING] Sorry, you are not registered as a student role yet.```");
            return mb.build();
        }
        try {
            // search quiz number from neo4j (incorrectExam contains common exam questions)
            String incorrectExam = neo4jHandler.readPersonalizedExam(studentId);
            System.out.println("[Neo4j] [Incorrect Exam] " + incorrectExam);
            JsonArray incorrectExamList = new Gson().fromJson(incorrectExam, JsonArray.class);
            // random pick one of the quiz, retrieve quiz data from Google sheet
            JsonObject quiz = parsePersonalQuiz(new SheetsHandler("course").readContentByKey("QuestionBank", setExamAlgorithm(incorrectExamList)));
            System.out.println("[personal quiz] " + quiz);
            // store user data and quiz in ongoing quiz map
            ongoingQuizMap.put(studentId, quiz);
            mb.append(quiz.get("question").getAsString());
            mb.setActionRows(ActionRow.of(getQuizComponents(quiz)));
        } catch (Exception e) {
            e.printStackTrace();
            return mb.append("```[WARNING] Sorry, something was wrong.```").build();
        }
        return mb.build();
    }

    /**
     * @param incorrectExamList used to compute the intersection
     * @return publishable and correct exam list
     */
    private String setExamAlgorithm(JsonArray incorrectExamList) throws Exception {
        JsonArray correctExamList = getCorrectExamList(incorrectExamList);
        int randomInt = ThreadLocalRandom.current().nextInt(0, 10);
        if (randomInt >= 8) return getRandomExam(correctExamList);
        else return getRandomExam(incorrectExamList);
//        may be wrong
//        return quizList.get(ThreadLocalRandom.current().nextInt(0, quizList.size())).getAsString();
    }

    /**
     * @param incorrectExamList used to compute the intersection
     * @return publishable and correct exam list
     */
    private JsonArray getCorrectExamList(JsonArray incorrectExamList) {
        JsonArray result = new JsonArray();
        JsonArray allPublishableExam = getAllPublishableExam();
        for (JsonElement exam : allPublishableExam) {
            // it means that this exam is correct
            if (!incorrectExamList.contains(exam)) result.add(exam);
        }
        return result;
    }

    /**
     * @return all publishable exam
     */
    private JsonArray getAllPublishableExam() {
        JSONObject allPublishableExam = new SheetsHandler("course").readContentByHeader("QuestionBank", "publishable");
        JsonArray result = new JsonArray();
        for (String key : allPublishableExam.keySet()) {
            // ["v"] to v
            String value = allPublishableExam.get(key).toString().split("\"")[1];
            if (value.equals("v")) result.add(key);
        }
        return result;
    }

    /**
     * @param examList correct or incorrect exam list
     * @return random exam in exam list
     */
    private String getRandomExam(JsonArray examList) throws Exception {
        int index = ThreadLocalRandom.current().nextInt(0, examList.size());
        // "16" to 16
        return String.valueOf(examList.get(index)).split("\"")[1];
    }

    /**
     * parse JSONObject(org.json) quiz to JsonObject(Gson)
     * change key name into english
     *
     * @param quiz
     * @return
     */
    private JsonObject parsePersonalQuiz(JSONObject quiz) {
        System.out.println("quiz: " + quiz);
        JsonObject result = new JsonObject();
        Iterator<String> jsonKey = quiz.keys();
        while (jsonKey.hasNext()) {
            String key = jsonKey.next();
//            String keyName = key.split(" / ")[1].strip();
            String value = quiz.getJSONArray(key).getString(0);
            if (value.isEmpty()) continue;
            result.addProperty(key, value);
        }
        return result;
    }

    /**
     * create button list for quiz options
     * button id should be 'A', 'B', 'C' and so on
     *
     * @param quiz
     * @return
     */
    private List<Button> getQuizComponents(JsonObject quiz) {
        ArrayList<Button> result = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : quiz.entrySet()) {
            String optionName = entry.getKey().strip();
            String optionValue = optionName.replace("opt", "");
            if (optionName.startsWith("opt"))
//                result.add(Button.primary(optionName.replace("opt", ""), entry.getValue().getAsString()));
                result.add(Button.primary(optionValue, optionValue));
        }
        Collections.shuffle(result); // shuffle button list
        return result;
    }

    public Message personalScore(String studentId) {
        MessageBuilder mb = new MessageBuilder();
        if (studentId.equals(NOT_STUDENT)) {
            System.out.println("[WARNING] there isn't registered as a student role.");
            mb.append("```[WARNING] Sorry, you are not registered as a student role yet.```");
            return mb.build();
        }
        JSONObject scoreMap = new SheetsHandler("course").readContentByKey("Grades", studentId);
        if (scoreMap.isEmpty()) {
            System.out.println("[WARNING] there is no scores yet.");
            mb.append("```[WARNING] Sorry, there is no scores yet.```");
            return mb.build();
        }
        scoreMap.remove("班級成員");
        mb.append("Here you are ! :grinning:\n");
        mb.append("```properties\n");
        // sort by key (exam or homework name)
        scoreMap.keySet()
                .stream()
                .sorted()
                .forEach(key -> {
                    String value = scoreMap.get(key).toString().split("\"")[1];
                    key = key.replaceAll(" ", "_");
                    mb.append(key).append(" = ").append(value).append("\n");
                });
        mb.append("```");
        return mb.build();
    }

    public Message readUserRequirements(String groupTopic, String groupName) {
        MessageBuilder mb = new MessageBuilder();
        String groupDocPath = userRequirementsFolderPath + File.separator + groupTopic + ".md";
        InputStream is = getClass().getResourceAsStream(groupDocPath);
        if (is != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            mb.append("here are the user requirements of your group. ( ").append(groupName).append(" )\n");
            mb.append("```markdown").append("\n");
            while (true) {
                try {
                    if (!br.ready()) break;
                    mb.append(br.readLine()).append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            mb.append("```");
        } else {
            mb.append("```[WARNING] Your user requirements is not found.```");
            System.out.println("[WARNING] User requirements is not found.");
        }
        return mb.build();
    }

    public Message createKeep(String groupName, String key, String value) {
        MessageBuilder mb = new MessageBuilder();
        if (redisHandler.hasContent(groupName, key)) {
            System.out.println("[WARNING] This key already exists.");
            return mb.append("```[WARNING] This key already exists.```").build();
        }
        redisHandler.createPair(groupName, key, value);
        mb.append("ok, got it.\n");
        mb.append("you created a content:\n");
        mb.append("```properties\n");
        mb.append(key).append(" = ").append(value).append("\n");
        mb.append("```");
        return mb.build();
    }

    public Message readKeep(String groupName) {
        Map allPair = redisHandler.readPair(groupName);
        MessageBuilder mb = new MessageBuilder();
        if (allPair.size() == 0) {
            System.out.println("[WARNING] no content yet.");
            return mb.append("```[WARNING] no content yet.```").build();
        }
        mb.append("ok, got it.\n");
        mb.append("The following are the contents of your group's keep:\n");
        mb.append("```properties\n");
        allPair.forEach((k, v) -> mb.append(k).append(" = ").append(v).append("\n"));
        mb.append("```");
        return mb.build();
    }

    public Message updateKeep(String groupName, String key, String value) {
        MessageBuilder mb = new MessageBuilder();
        if (!redisHandler.hasContent(groupName, key)) {
            System.out.println("[WARNING] There is no such key in the keep.");
            return mb.append("```[WARNING] There is no such key in the keep.```").build();
        }
        String oldValue = redisHandler.updatePair(groupName, key, value);
        System.out.println("[Old Value] " + oldValue);
        System.out.println("[New Value] " + value);
        mb.append("ok, got it.\n");
        mb.append("you update a content:\n");
        mb.append("```properties\n");
        mb.append(key).append(" = ").append(oldValue).append("\n");
        mb.append(DOWN_ARROW).append("\n");
        mb.append(key).append(" = ").append(value).append("\n");
        mb.append("```");
        return mb.build();
    }

    public Message deleteKeep(String groupName, String key) {
        System.out.println("[Deleted Key] " + key);
        MessageBuilder mb = new MessageBuilder();
        if (!redisHandler.hasContent(groupName, key)) {
            System.out.println("[WARNING] There is no such key in the keep.");
            return mb.append("```[WARNING] There is no such key in the keep.```").build();
        }
        String deletedValue = redisHandler.deletePair(groupName, key);
        System.out.println("[Deleted Value] " + deletedValue);
        mb.append("ok, got it.\n");
        mb.append("you deleted a content:\n");
        mb.append("```properties\n");
        mb.append(key).append(" = ").append(deletedValue).append("\n");
        mb.append("```");
        return mb.build();
    }
}
