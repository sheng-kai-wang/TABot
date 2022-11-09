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
    private final Neo4jHandler neo4jHandler;
    private final RedisHandler redisHandler;
    private final CommitmentRetriever commitmentRetriever;
    private static HashMap<String, JsonObject> ongoingQuizMap;

    private final String anonymousQuestionChannelUrl;
    private final String userRequirementsFolderPath;
    private final String contributionAnalysisUrl;
    private final String viewTheCommitmentUrl;
    private final String browseCommitFilesUrl;
    public final static String NO_GROUP = "no group";
    public final static String NOT_STUDENT = "not student";
    private final static String DOWN_ARROW = "↓";

    @Autowired
    public SlashCommandHandleService(Environment env, Neo4jHandler neo4jHandler, RedisHandler redisHandler, CommitmentRetriever commitmentRetriever) {
        ongoingQuizMap = new HashMap<>();
        this.userRequirementsFolderPath = env.getProperty("user-requirements.folder.path");
        this.anonymousQuestionChannelUrl = env.getProperty("discord.channel.anonymous-question.url");
        this.contributionAnalysisUrl = env.getProperty("contribution-analysis.url");
        this.viewTheCommitmentUrl = env.getProperty("view-the-commitment.url");
        this.browseCommitFilesUrl = env.getProperty("browse-commit-files.url");

        this.neo4jHandler = neo4jHandler;
        this.redisHandler = redisHandler;
        this.commitmentRetriever = commitmentRetriever;
    }

    public static HashMap<String, JsonObject> getOngoingQuizMap() {
        return ongoingQuizMap;
    }

    public Message getAnonymousQuestionResponse(String question) {
        MessageBuilder mb = new MessageBuilder();
        mb.append("ok, got it.\n");
        mb.append("Your question is `").append(question).append("`\n");
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
            mb.append("```properties" + "\n[WARNING] Sorry, No course ppt are available yet.```");
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
            mb.append("```properties" + "\n[WARNING] Sorry, you are not registered as a student role yet.```");
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
            return mb.append("```properties" + "\n[WARNING] Sorry, something was wrong.```").build();
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
        try {
            if (randomInt >= 8) return getRandomExam(correctExamList);
            else return getRandomExam(incorrectExamList);
        } catch (Exception e) {
            System.out.println("[WARNING] no exam record yet");
            return getRandomExam(incorrectExamList);
        }
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
            mb.append("```properties" + "\n[WARNING] Sorry, you are not registered as a student role yet.```");
            return mb.build();
        }
        JSONObject scoreMap = new SheetsHandler("course").readContentByKey("Grades", studentId);
        if (scoreMap.isEmpty()) {
            System.out.println("[WARNING] there is no scores yet.");
            mb.append("```properties" + "\n[WARNING] Sorry, there is no scores yet.```");
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
                    mb.append(key).append(": ").append(value).append("\n");
                });
        mb.append("```");
        return mb.build();
    }

    public Message readUserRequirements(String groupTopic, String groupName) {
        MessageBuilder mb = new MessageBuilder();
        try {
            String groupDocPath = userRequirementsFolderPath + groupTopic + ".md";
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
                mb.append("```properties" + "\n[WARNING] Sorry, your group topic has NOT been set.```");
                System.out.println("[WARNING] User requirements is not found.");
            }
            return mb.build();

        } catch (Exception e) {
            e.printStackTrace();
            return mb.append("```properties" + "\n[WARNING] Sorry, your group topic has NOT been set.```").build();
        }
    }

    public Message createKeep(String groupName, String keys, String value) {
        MessageBuilder mb = new MessageBuilder();

        // format keys
        StringBuilder keySb = new StringBuilder();
        Arrays.stream(keys.split(",")).forEach(k -> {
            if (!k.startsWith(redisHandler.GITHUB_REPOSITORY_KEEP_KEY_PREFIX)) {
                keySb.append(k.trim().replace(" ", "_")).append(",");
            }
        });
        String formattedKeys;
        if (keySb.length() > 0) {
            formattedKeys = keySb.deleteCharAt(keySb.length() - 1).toString();
        } else {
            System.out.println("[WARNING] Can't use these keys.");
            return mb.append("```properties" + "\n[WARNING] Sorry, you cannot use these keys.```").build();
        }

        if (redisHandler.hasSameKey(groupName, formattedKeys)) {
            System.out.println("[WARNING] This key already exists.");
            return mb.append("```properties" + "\n[WARNING] This key already exists.```").build();
        }
        redisHandler.createPair(groupName, formattedKeys, value);
        mb.append("ok, got it.\n");
        mb.append("you created a content:\n");
        mb.append("```properties\n");
        mb.append(formattedKeys).append(": ").append(value).append("\n");
        mb.append("```");
        return mb.build();
    }

    public Message createAliasesOfKey(String groupName, String key, String aliases) {
        MessageBuilder mb = new MessageBuilder();

        // format key
        String formattedKey = key.trim().replace(" ", "_");

        if (!redisHandler.hasSameKey(groupName, formattedKey)) {
            System.out.println("[WARNING] There is no such key in the keep.");
            return mb.append("```properties" + "\n[WARNING] There is no such key in the keep.```").build();
        }

        String oldKey = redisHandler.getCompletedKey(groupName, formattedKey);
        if (oldKey == null) {
            System.out.println("[WARNING] Just use one of the keys.");
            return mb.append("```properties" + "\n[WARNING] Just use one of the keys.```").build();
        }


        // format aliases
        Set<String> aliasSet = new HashSet<>(List.of(aliases.split(",")));
        StringBuilder aliasSb = new StringBuilder();
        aliasSet.forEach(a -> {
            if (Arrays.stream(oldKey.split(",")).noneMatch(a::equals)) {
                if (!a.startsWith(redisHandler.GITHUB_REPOSITORY_KEEP_KEY_PREFIX)) {
                    aliasSb.append(a.trim().replace(" ", "_")).append(",");
                }
            }
        });
        String formattedAliases;
        if (aliasSb.length() > 0) {
            formattedAliases = aliasSb.deleteCharAt(aliasSb.length() - 1).toString();
        } else {
            System.out.println("[WARNING] Can't use these aliases.");
            return mb.append("```properties" + "\n[WARNING] Sorry, you cannot use these aliases.```").build();
        }

        String newKey = oldKey + "," + formattedAliases;
        String value = redisHandler.readPairByKey(groupName, formattedKey);

        // update pair's key
        redisHandler.deletePair(groupName, oldKey);
        redisHandler.createPair(groupName, newKey, value);

        mb.append("ok, got it.\n");
        mb.append("You updated the key of a content:\n");
        mb.append("```properties\n");
        mb.append(newKey).append(": ").append(value).append("\n");
        mb.append("```");
        return mb.build();
    }

    public Message deleteAliasesOfKey(String groupName, String key, String aliases) {
        MessageBuilder mb = new MessageBuilder();

        // format key
        String formattedKey = key.trim().replace(" ", "_");

        if (!redisHandler.hasSameKey(groupName, formattedKey)) {
            System.out.println("[WARNING] There is no such key in the keep.");
            return mb.append("```properties" + "\n[WARNING] There is no such key in the keep.```").build();
        }

        // format aliases
        Set<String> aliasSet = new HashSet<>(List.of(aliases.split(",")));
        StringBuilder aliasSb = new StringBuilder();
        aliasSet.forEach(a -> {
            if (!a.startsWith(redisHandler.GITHUB_REPOSITORY_KEEP_KEY_PREFIX)) {
                aliasSb.append(a.trim().replace(" ", "_")).append(",");
            }
        });
        String formattedAliases;
        if (aliasSb.length() > 0) {
            formattedAliases = aliasSb.deleteCharAt(aliasSb.length() - 1).toString();
        } else {
            System.out.println("[WARNING] Can't delete these aliases.");
            return mb.append("```properties" + "\n[WARNING] Sorry, you cannot delete these aliases.```").build();
        }

        // remove aliases from oldKey
        String oldKey = redisHandler.getCompletedKey(groupName, formattedKey);
        if (oldKey == null) {
            System.out.println("[WARNING] Just use one of the keys.");
            return mb.append("```properties" + "\n[WARNING] Just use one of the keys.```").build();
        }

        StringBuilder newKeySb = new StringBuilder();
        List.of(oldKey.split(",")).forEach(k -> {
            if (Arrays.stream(formattedAliases.split(",")).noneMatch(k::equals)) {
                newKeySb.append(k).append(",");
            }
        });
        String newKey;
        if (newKeySb.length() > 0) {
            newKey = newKeySb.deleteCharAt(newKeySb.length() - 1).toString();
        } else {
            System.out.println("[WARNING] Can't delete all of aliases.");
            return mb.append("```properties" + "\n[WARNING] Sorry, you cannot delete all of aliases.```").build();
        }

        String value = redisHandler.readPairByKey(groupName, formattedKey);

        // update pair's key
        redisHandler.deletePair(groupName, oldKey);
        redisHandler.createPair(groupName, newKey, value);

        mb.append("ok, got it.\n");
        mb.append("You updated the key of a content:\n");
        mb.append("```properties\n");
        mb.append(newKey).append(": ").append(value).append("\n");
        mb.append("```");
        return mb.build();
    }

    public Message readKeep(String groupName, String key) {
        MessageBuilder mb = new MessageBuilder();
        EmbedBuilder eb = new EmbedBuilder();
        Map<String, String> allPair = redisHandler.readPairAll(groupName);
        if (allPair.size() == 0) {
            System.out.println("[WARNING] no content yet.");
            return mb.append("```properties" + "\n[WARNING] no content yet.```").build();
        }
        mb.append("ok, got it.\n");
        mb.append("The following are the contents of your group's keep:\n");
        mb.append("```properties\n");
        if (key == null) {
            allPair.forEach((k, v) -> {
                if (v.toLowerCase().startsWith("https://")) {
                    eb.addField(k, "[" + v + "](" + v + ")", false);
                }
                mb.append(k).append(": ").append(v).append("\n");
            });
        } else {
            // format key
            String formattedKey = key.trim().replace(" ", "_");

            if (!redisHandler.hasSameKey(groupName, formattedKey)) {
                System.out.println("[WARNING] There is no such key in the keep.");
                return mb.append("[WARNING] There is no such key in the keep.```").build();
            }

            String completedKey = redisHandler.getCompletedKey(groupName, formattedKey);
            if (completedKey == null) {
                System.out.println("[WARNING] Just use one of the keys.");
                return mb.append("```properties" + "\n[WARNING] Just use one of the keys.```").build();
            }
            String value = allPair.get(completedKey);
            eb.addField(completedKey, "[" + value + "](" + value + ")", false);
            mb.append(completedKey).append(": ").append(value);
        }
        mb.append("```");
        mb.setEmbeds(eb.build());
        return mb.build();
    }

    public Message updateKeep(String groupName, String key, String value) {
        MessageBuilder mb = new MessageBuilder();

        // format key
        String formattedKey = key.trim().replace(" ", "_");

        if (!redisHandler.hasSameKey(groupName, formattedKey)) {
            System.out.println("[WARNING] There is no such key in the keep.");
            return mb.append("```properties" + "\n[WARNING] There is no such key in the keep.```").build();
        }

        String completedKey = redisHandler.getCompletedKey(groupName, formattedKey);
        if (completedKey == null) {
            System.out.println("[WARNING] Just use one of the keys.");
            return mb.append("```properties" + "\n[WARNING] Just use one of the keys.```").build();
        }

        String oldValue = redisHandler.updatePair(groupName, completedKey, value);
        System.out.println("[Old Value] " + oldValue);
        System.out.println("[New Value] " + value);
        mb.append("ok, got it.\n");
        mb.append("you update a content:\n");
        mb.append("```properties\n");
        mb.append(completedKey).append(": ").append(oldValue).append("\n");
        mb.append(DOWN_ARROW).append("\n");
        mb.append(completedKey).append(": ").append(value).append("\n");
        mb.append("```");
        return mb.build();
    }

    public Message deleteKeep(String groupName, String key) {
        MessageBuilder mb = new MessageBuilder();

        // format key
        String formattedKey = key.trim().replace(" ", "_");

        if (!redisHandler.hasSameKey(groupName, formattedKey)) {
            System.out.println("[WARNING] There is no such key in the keep.");
            return mb.append("```properties" + "\n[WARNING] There is no such key in the keep.```").build();
        }

        String completedKey = redisHandler.getCompletedKey(groupName, formattedKey);
        if (completedKey == null) {
            System.out.println("[WARNING] Just use one of the keys.");
            return mb.append("```properties" + "\n[WARNING] Just use one of the keys.```").build();
        }

        String deletedValue = redisHandler.deletePair(groupName, completedKey);
        System.out.println("[Deleted Value] " + deletedValue);
        mb.append("ok, got it.\n");
        mb.append("you deleted a content:\n");
        mb.append("```properties\n");
        mb.append(completedKey).append(": ").append(deletedValue).append("\n");
        mb.append("```");
        return mb.build();
    }

    public Message setRepository(String groupName, String url) {
        MessageBuilder mb = new MessageBuilder();
        String repository = url.split("/")[4].split("\\.")[0];
        redisHandler.createPair(groupName, redisHandler.GITHUB_REPOSITORY_KEEP_KEY_PREFIX + repository, url);
        commitmentRetriever.registerRepository(groupName);
        mb.append("ok, got it.\n");
        mb.append("you created a content:\n");
        mb.append("```properties\n");
        mb.append(redisHandler.GITHUB_REPOSITORY_KEEP_KEY_PREFIX).append(repository).append(": ").append(url).append("\n");
        mb.append("```");
        return mb.build();
    }

    public Message contributionAnalysis(String groupName, String groupTopic) {
        MessageBuilder mb = new MessageBuilder();

        HashMap<String, String> repoMap = new HashMap<>();
        redisHandler.readPairAll(groupName).forEach((k, v) -> {
            if (k.startsWith(redisHandler.GITHUB_REPOSITORY_KEEP_KEY_PREFIX)) {
                String repository = k.split(",")[0].replace(redisHandler.GITHUB_REPOSITORY_KEEP_KEY_PREFIX, "");
                repoMap.put(repository, v);
            }
        });

        if (repoMap.size() == 0) {
            System.out.println("[WARNING] Hasn't set up any repository.");
            return mb.append("```properties" + "\n[WARNING] You haven't set up any repository, please use \"/set_github_repository\" command.```").build();
        }

        mb.append("ok, got it.\n");
        mb.append("These are your group's contribution analysis for MAIN branch ! :grinning:\n");
        StringBuilder linkSb = new StringBuilder();
        repoMap.forEach((k, v) -> {
            String username = v.split("/")[3];
            String contributionUrl = contributionAnalysisUrl
                    .replace("<<username>>", username)
                    .replace("<<repository>>", k);
            linkSb.append("[").append(k).append("](").append(contributionUrl).append(")\n");
        });
        EmbedBuilder eb = new EmbedBuilder();
        eb.addField(groupTopic, linkSb.toString(), false);
        mb.setEmbeds(eb.build());
        return mb.build();
    }

    public Message commitmentRetrieval(String groupName, String repositories, String branches, String keywords, int quantity) {
        MessageBuilder mb = new MessageBuilder();
        EmbedBuilder eb = new EmbedBuilder();
        JsonArray range = new JsonArray();

        if (quantity <= 0) {
            System.out.println("[WARNING] Quantity must be a positive integer.");
            return mb.append("```properties" + "\n[WARNING] Sorry, quantity must be a positive integer.```").build();
        }

        if (repositories == null) {
            if (branches != null) {
                System.out.println("[WARNING] There's not specify the repository.");
                return mb.append("```properties" + "\n[WARNING] Sorry, you didn't specify the repository.```").build();
            }
            range.add("*:*");
        } else {
            for (String repo : repositories.split(",")) {
                String repoName = repo.trim();
                if (!redisHandler.hasSameKey(groupName, redisHandler.GITHUB_REPOSITORY_KEEP_KEY_PREFIX + repoName)) {
                    System.out.println("[WARNING] The input contains some repositories that doesn't exist in the keep.");
                    return mb.append("```properties" + "\n[WARNING] Sorry, your input contains some repositories that doesn't exist in the keep.```").build();
                }
                String userNameAndRepoName = redisHandler.getUserNameAndRepoName(groupName, repoName);
                if (branches == null) range.add(userNameAndRepoName + ":*");
                else {
                    for (String branch : branches.split(",")) {
                        range.add(userNameAndRepoName + ":" + branch.trim());
                    }
                }
            }
        }

        JsonArray rank = commitmentRetriever.retrieveCommitMsg(groupName, keywords, range, quantity);
        if (rank == null) {
            System.out.println("[WARNING] Something was wrong, maybe a non-existent branch was entered.");
            return mb.append("```properties" + "\n[WARNING] Sorry, something was wrong, maybe a non-existent branch was entered.```").build();
        }
        if (rank.isEmpty()) {
            System.out.println("[WARNING] Could not find any similar results.");
            return mb.append("```properties" + "\n[WARNING] Sorry, could not find any similar results.```").build();
        }
        mb.append("ok, got it.\n");
        mb.append("keywords: `").append(keywords).append("`\n");
        mb.append("The following are the similarity ranking of your group's commit messages.\n");
        mb.append("This is in descending order and the content is ONLY updated to an hour ago:\n");
        int number = 1;
        for (JsonElement item : rank) {
            JsonObject commitment = item.getAsJsonObject();
            String commitMsg = "[" + (number++) + "] " + commitment.get("message").getAsString();
            String[] repoData = commitment.get("repo").getAsString().split(",|:");
            String username = repoData[0];
            String repository = repoData[1];
            String id = commitment.get("id").getAsString();
            String fromWhere = commitment.get("repo").getAsString()
                    .replace(",", "/")
                    .replace(":", " : ");

            String currentViewTheCommitmentUrl = viewTheCommitmentUrl
                    .replace("<<username>>", username)
                    .replace("<<repository>>", repository)
                    .replace("<<hash_id>>", id);

            String currentBrowseCommitFilesUrl = browseCommitFilesUrl
                    .replace("<<username>>", username)
                    .replace("<<repository>>", repository)
                    .replace("<<hash_id>>", id);

            String commitData = new StringBuilder()
                    .append("from -> ").append(fromWhere).append("\n")
                    .append("[view the commitment](").append(currentViewTheCommitmentUrl).append(")").append("\n")
                    .append("[browse the files](").append(currentBrowseCommitFilesUrl).append(")").append("\n")
                    .toString();

            eb.addField(commitMsg, commitData, false);
        }

        mb.setEmbeds(eb.build());
        return mb.build();
    }
}
