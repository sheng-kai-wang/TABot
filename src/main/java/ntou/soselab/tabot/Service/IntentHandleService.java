package ntou.soselab.tabot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import ntou.soselab.tabot.Entity.Rasa.Intent;
import ntou.soselab.tabot.repository.Neo4jHandler;
import ntou.soselab.tabot.repository.SheetsHandler;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IntentHandleService {

    public static HashMap<String, JsonObject> ongoingQuizMap;
    private final String SUGGEST_FORM_URL;

    @Autowired
    public IntentHandleService(Environment env){
        ongoingQuizMap = new HashMap<>();
        this.SUGGEST_FORM_URL = env.getProperty("env.setting.suggest");
    }

    /**
     * declare what bot should do with each intent
     * <br>Note: expect STUDENT id passed as parameter
     * @param userStudentId student id
     * @param intent incoming intent
     */
    public Message checkIntent(String userStudentId, Intent intent){
        String intentName = intent.getCustom().getIntent();
        switch (intentName){
            case "greet":
                System.out.println("[DEBUG][checkIntent] greet");
                return generateGreetingsMessage();
            case "help":
                System.out.println("[DEBUG][checkIntent] help");
                return generateHelpMessage();
            case "classmap_search":
                return searchClassMap(intent);
            case "classmap_ppt":
                return searchSlide(intent);
            case "classmap_suggest":
                return sendSuggestFormMsg();
            default:
                if(intentName.startsWith("confirm"))
                    return confirmHandler(intent);
                else if(intentName.startsWith("faq"))
                    return faqHandle(intent);
                else if(intentName.startsWith("personal"))
                    return personalFuncHandler(userStudentId, intent);
                else
                    System.out.printf("[DEBUG][intent analyze]: '%s' detected, no correspond result found.", intentName);
                break;
        }
        return null;
    }

    /**
     * generate simple greeting message
     * @return greeting message
     */
    private Message generateGreetingsMessage(){
        MessageBuilder builder = new MessageBuilder();
        builder.append("Hi, how do you do. :sunglasses:");
        return builder.build();
    }

    /**
     * generate simple help message
     * @return help message
     */
    private Message generateHelpMessage(){
        MessageBuilder builder = new MessageBuilder();
        builder.append("Hi, how can i help you ?");
        return  builder.build();
    }

    /**
     * get faq data from google sheet, return response message
     * @param faqIntent
     * @return
     */
    public Message faqHandle(Intent faqIntent){
        System.out.println("[DEBUG][intentHandle] faq handle triggered.");
        String faqName = faqIntent.getCustom().getIntent().replace("faq/", "");
        System.out.println(" --- [DEBUG][faq] " + faqName + " detected.");
        // call google sheet api to query correspond result of faq
        JSONObject searchResult = new SheetsHandler("SE").readContentByKey("FAQ", faqName);
        System.out.println("--- [DEBUG][faq] searchResult: " + searchResult);
        String response = searchResult.getJSONArray("answer").get(0).toString();
        return new MessageBuilder().append(response).build();
    }

    public Message confirmHandler(Intent confirmIntent){
        // classmap_search, personal_score_query, classmap_ppt_query
        String response = confirmIntent.getCustom().getResponseMessage();
        return new MessageBuilder().append(response).build();
    }

    /**
     * send Google form link to let user add suggestion
     * SUSPEND
     * @return
     */
    private Message sendSuggestFormMsg(){
        System.out.println("--- [DEBUG][intentHandle] try to send suggest form.");
        MessageBuilder builder = new MessageBuilder();
        builder.append("That's sound good, check out link below to contribute material, thanks.");
        builder.setEmbeds(new EmbedBuilder().addField("Submit   your suggestion here !", ":scroll: [click me to submit stuff](" + SUGGEST_FORM_URL + ")", false).build());
        return builder.build();
    }

    private Message searchSlide(Intent intent){
        System.out.println("[DEBUG][intentHandle] slide search triggered.");
        String targetChapter = intent.getCustom().getEntity();
        System.out.println("--- [DEBUG][search slide] raw chapter name: " + targetChapter);
        // check if entity extraction successfully captured values
        if(targetChapter == null || targetChapter.equals("None") || targetChapter.isEmpty())
            return sendErrorMessage(intent);
        int chapterNum = extractChapterNumber(targetChapter);
        System.out.println("--- [DEBUG][search slide] raw chapter name: " + targetChapter);
        // search neo4j for chapter slide info
        String queryResp = new Neo4jHandler("SE").readSlideshowById(chapterNum);
        MessageBuilder builder = new MessageBuilder();
        builder.append("Here you are ! :grinning:");
        builder.setEmbeds(new EmbedBuilder().addField("Chapter " + chapterNum, "[link](" + queryResp + ")", false).build());
        return builder.build();
    }

    /**
     * get chapter number from raw chapter name<br>
     * example: get chapter number 'n' from 'chapter_n'
     * @param rawChapterName
     * @return
     */
    private int extractChapterNumber(String rawChapterName){
        String raw =  rawChapterName.strip().replace("chapter_", "");
        return Integer.parseInt(raw);
    }

    /**
     * search keyword from Google sheet
     * @param intent
     * @return
     */
    private Message searchClassMap(Intent intent){
        System.out.println("[DEBUG][intentHandle] class map search triggered.");
        String queryTarget = intent.getCustom().getEntity();
        System.out.println("--- [DEBUG][search class map] try to query '" + queryTarget + "'.");
        // check if entity extraction successfully captured values
        if(queryTarget == null || queryTarget.equals("None") || queryTarget.isEmpty())
            return sendErrorMessage(intent);
        // check section name from Google sheet 'keyword' page
        String sectionName = searchKeywordSheet(queryTarget);
        System.out.println("--- [DEBUG][search class map] found section '" + sectionName + "' from google sheet.");
        if(sectionName.isEmpty())
            return sendErrorMessage(intent);
        // check query result from neo4j
        String chapterTitle = removeBrackets(new Neo4jHandler("SE").readCurriculumMap(sectionName));
        System.out.println("--- [DEBUG][search class map][neo4j] found correspond chapter title: " + chapterTitle);
        String slideLink = new Neo4jHandler("SE").readSlideshowByName(sectionName);
        System.out.println("--- [DEBUG][search class map][neo4j] found section slideLink: " + slideLink);
        // build response message
        Message result = generateSearchClassMapResponseMessage(sectionName, chapterTitle, slideLink);
        return result;
    }

    /**
     * generate response message for class map search
     * response message should contain keyword, correspond chapter title and slide link
     * chapter title and slide link will be placed in an embed object
     * @param sectionName
     * @param chapterTitle correspond chapter title
     * @param slideLink slide link
     * @return
     */
    private Message generateSearchClassMapResponseMessage(String sectionName, String chapterTitle, String slideLink){
        MessageBuilder builder = new MessageBuilder();
        builder.append("Looks like you are searching about `" + sectionName + "` .\n");
        builder.append("Maybe you can checkout more from link below. :sunglasses:");
//        builder.setEmbeds(new EmbedBuilder().addField("Section '" + sectionName + "' from Chapter '" + chapterTitle + "'", "[" + sectionName + "](" + slideLink + ")", false).build());
        builder.setEmbeds(new EmbedBuilder().addField(chapterTitle, "[" + sectionName + "](" + slideLink + ")", false).build());
        return builder.build();
    }

    /**
     * remove brackets from neo4j response
     * example: '["something"]' -> 'something'
     * @param raw raw content
     * @return
     */
    private String removeBrackets(String raw){
        Pattern pattern = Pattern.compile("^\\[\"(.*)\"\\]$");
        Matcher matcher = pattern.matcher(raw);
        String result = "";
        if(matcher.find())
            result = matcher.group(1);
        return result;
    }

    private String searchKeywordSheet(String target){
        String rawKeywordSheet = new SheetsHandler("SE").readContent("Keyword", "");
        System.out.println("--- [DEBUG][class map search] retrieve all keyword data from google sheet.");
        Gson gson = new Gson();
        JsonArray keywordSheet = gson.fromJson(rawKeywordSheet, JsonArray.class);
        String resultKeyword = "";
        for(JsonElement keywordSet: keywordSheet){
            JsonArray temp = keywordSet.getAsJsonArray();
            if(Arrays.stream(temp.get(1).getAsString().split(",")).anyMatch(keyword -> keyword.strip().equals(target))){
                resultKeyword = temp.get(0).getAsString().strip();
                return resultKeyword;
            }
        }
        return resultKeyword;
    }

    /**
     * handle personal-related function, include score query, textbook query and quiz query<br>
     * Note: expect STUDENT id as parameter, NOT discord id
     * @param studentId student id
     * @param personalIntent detected intent
     * @return result Message
     */
    public Message personalFuncHandler(String studentId, Intent personalIntent){
        /* --- test block: change all personal function access from testing id --- */
        // remember to change all testId back to studentId
        String testId = "00000000";
        /* --- end of test block --- */
        String intentName = personalIntent.getCustom().getIntent();
        String scoreQueryTarget = personalIntent.getCustom().getEntity();
        // check if entity extraction successfully captured values
        if(intentName.contains("score") && (scoreQueryTarget == null || scoreQueryTarget.equals("None") || scoreQueryTarget.isEmpty()))
            return sendErrorMessage(personalIntent);
        switch(intentName){
            case "personal_score_query":
//                return generatePersonalScoreQueryResponse(checkPersonalScore(studentId, scoreQueryTarget));
                return generatePersonalScoreQueryResponse(checkPersonalScore(testId, scoreQueryTarget));
            case "personal_textbook_query":
//                return getPersonalTextbook(studentId);
                return getPersonalTextbook(testId);
            case "personal_quiz_query":
//                return getPersonalQuiz(studentId);
                return getPersonalQuiz(testId);
        }
        return sendErrorMessage(personalIntent);
    }

    /**
     * search personal textbook
     * get personal textbook title from neo4j and use it to search textbook(slide) info from neo4j again
     * @param studentId
     * @return
     */
    public Message getPersonalTextbook(String studentId){
        System.out.println("[DEBUG][intentHandle] personal textbook triggered.");
        System.out.println("[DEBUG] try to search personal textbook for " + studentId);
        Gson gson = new Gson();
        String queryResp = new Neo4jHandler("SE").readPersonalizedSubjectMatter(studentId);
        System.out.println("--- [DEBUG][personal textbook][neo4j] raw queryResp: " + queryResp);
        // get textbook title from neo4j
        JsonArray queryResult = gson.fromJson(queryResp, JsonArray.class);
//        System.out.println("--- [DEBUG][personal textbook] query result: " + queryResult);
        System.out.println("--- [DEBUG][personal textbook] query result size " + queryResult.size());
        if(queryResult.size() < 1)
            return getNullTextbookMessage();
        // search neo4j for each slide data
        HashMap<String, String> resultMap = new HashMap<>();
        for(JsonElement chapterName: queryResult){
            String chapterData = new Neo4jHandler("SE").readSlideshowByName(chapterName.getAsString());
            System.out.println("--- [DEBUG][personal textbook] chapterData: " + chapterData);
            resultMap.put(chapterName.getAsString(), chapterData);
        }
        return generatePersonalTextbookMsg(resultMap, queryResp);
    }

    /**
     * generate response message if no personal textbook found
     * @return response message
     */
    private Message getNullTextbookMessage(){
        MessageBuilder builder = new MessageBuilder();
        builder.append("> Seems like you have no personal textbook for now. :neutral_face:");
        return builder.build();
    }

    /**
     * generate response message for textbook query
     * each textbook data will be placed in an embed field
     * @param textbookMap
     * @return
     */
    private Message generatePersonalTextbookMsg(HashMap<String, String> textbookMap, String titleList){
        System.out.println("[DEBUG][personal textbook] start to generate response message.");
        MessageBuilder builder = new MessageBuilder();
        builder.append("I found some textbook information just for you !\n");
        builder.append("Looks like you might want to checkout " + titleList);
        builder.setEmbeds(getTextbookEmbedList(textbookMap));
        return builder.build();
    }

    private List<MessageEmbed> getTextbookEmbedList(HashMap<String, String> textbookMap){
        ArrayList<MessageEmbed> resultList = new ArrayList<>();
        for(Map.Entry<String, String> textbook: textbookMap.entrySet()){
            String name = textbook.getKey();
            System.out.println("--- [DEBUG][generate textbook resp msg] textbook name: " + name);
            String link = textbook.getValue();
            System.out.println("--- [DEBUG][generate textbook resp msg] textbook link: " + link);
            resultList.add(new EmbedBuilder().addField(name, "[textbook link](" + link + ")", false).build());
        }
        return resultList;
    }

    public Message getPersonalQuiz(String studentId){
        System.out.println("[DEBUG][intentHandle] personal quiz triggered.");
        Gson gson = new Gson();
        // search quiz number from neo4j
        String quizResp = new Neo4jHandler("SE").readPersonalizedTest(studentId);
        System.out.println("--- [DEBUG][personal quiz] id: "+ studentId);
        System.out.println("--- [DEBUG][personal quiz][neo4j] quizResp: " + quizResp);
        JsonArray quizNumList = gson.fromJson(quizResp, JsonArray.class);
        // random pick one of the quiz, retrieve quiz data from Google sheet
        JsonObject quiz = parsePersonalQuiz(new SheetsHandler("SE").readContentByKey("QuestionBank", pickRandomQuiz(quizNumList).strip()));
        System.out.println("--- [DEBUG][personal quiz] quiz: " + quiz);
        // store user data and quiz in ongoing quiz map
        ongoingQuizMap.put(studentId, quiz);
        return createQuizMessage(quiz);
    }

    private String pickRandomQuiz(JsonArray quizList){
        return quizList.get(ThreadLocalRandom.current().nextInt(0, quizList.size())).getAsString();
    }

    /**
     * parse JSONObject(org.json) quiz to JsonObject(Gson)
     * change key name into english
     * @param quiz
     * @return
     */
    private JsonObject parsePersonalQuiz(JSONObject quiz){
        JsonObject result = new JsonObject();
        Iterator<String> jsonKey = quiz.keys();
        while(jsonKey.hasNext()){
            String key = jsonKey.next();
            String keyName = key.split(" / ")[1].strip();
            String value = quiz.getJSONArray(key).getString(0);
            if(value.isEmpty()) continue;
            result.addProperty(keyName, value);
        }
        return result;
    }

    /**
     * create quiz message for one personal quiz
     * @param quiz
     * @return
     */
    private Message createQuizMessage(JsonObject quiz){
        MessageBuilder builder = new MessageBuilder();
        builder.append(quiz.get("question").getAsString());
        builder.setActionRows(ActionRow.of(getQuizComponents(quiz)));
        return builder.build();
    }

    /**
     * create button list for quiz options
     * button id should be 'A', 'B', 'C' and so on
     * @param quiz
     * @return
     */
    private List<Button> getQuizComponents(JsonObject quiz){
        ArrayList<Button> result = new ArrayList<>();
        for(Map.Entry<String, JsonElement> entry: quiz.entrySet()){
            String optionName = entry.getKey().strip();
            String optionValue = optionName.replace("opt", "");
            if(optionName.startsWith("opt"))
//                result.add(Button.primary(optionName.replace("opt", ""), entry.getValue().getAsString()));
                result.add(Button.primary(optionValue, optionValue));
        }
        Collections.shuffle(result); // shuffle button list
        return result;
    }

    /**
     * call google sheet api to get call personal score, parse personal score in to hashmap and return target score
     * score map data set example: 'final_exam=102'
     * @param studentId student id
     * @param target query target
     * @return target score
     */
    private String checkPersonalScore(String studentId, String target){
        System.out.println("[DEBUG][intentHandle] personal score triggered.");
        HashMap<String, String> personalScoreMap = new HashMap<>();
        JSONObject scoreMap = new SheetsHandler("SE").readContentByKey("Grades", studentId);
        System.out.println("--- [DEBUG][personal score] scoreMap: " + scoreMap);
        Iterator<String> jsonKey = scoreMap.keys();
        while(jsonKey.hasNext()){
            String key = jsonKey.next();
            String keyName = "";
            String value = scoreMap.getJSONArray(key).getString(0);
            // check if key has entity name
            if(key.contains(" / ")){
                keyName = key.split(" / ")[1];
            }else{
                keyName = key;
            }
            personalScoreMap.put(keyName, value);
        }
        return personalScoreMap.get(target);
    }

    /**
     * generate response message for personal score query
     * @param score
     * @return
     */
    private Message generatePersonalScoreQueryResponse(String score){
        MessageBuilder builder = new MessageBuilder();
        builder.append("Your score is **" + score + "**");
        return builder.build();
    }

    /**
     * throw an error message back if
     * @param intent
     * @return
     */
    private Message sendErrorMessage(Intent intent){
        String user = intent.getRecipient_id();
        System.out.println("[DEBUG][intent handle] error message triggered ! check detected intent below");
        System.out.println(" >> intent: " + intent);
        MessageBuilder msgBuilder = new MessageBuilder();
        msgBuilder.mentionUsers(user);
        msgBuilder.append("Sorry, i'm not sure what do you mean, can you please say it again in another way ? :pray:");
//        EmbedBuilder builder  = new EmbedBuilder();
//        builder.setDescription("<@" + user + "> Sorry, i'm not sure what do you mean, can you please say it again in another way ? :(");
//        builder.setColor(Color.orange);
//        return builder.build();
        return msgBuilder.build();
    }

    /**
     * SUSPEND
     * @return
     */
    private Message suggestPassed(){
        return null;
    }

    /**
     * SUSPEND
     * @return
     */
    private Message suggestFailed(){
        return null;
    }


}
