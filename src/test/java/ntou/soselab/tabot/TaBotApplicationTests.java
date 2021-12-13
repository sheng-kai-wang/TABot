package ntou.soselab.tabot;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ntou.soselab.tabot.Entity.UserProfile;
import ntou.soselab.tabot.repository.Neo4jHandler;
import ntou.soselab.tabot.repository.SheetsHandler;
import org.apache.catalina.User;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@SpringBootTest
class TaBotApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void languageTest(){
        String testMsg = "中文測試";
        String testMsg1 = "天氣總算放晴，沒有rain、太陽很big、有點hot、讓我想到以前還是student時，喜歡在這樣的天氣，吃一球ice cream，真的會讓人很happy";
        String testMsg2 = "我是很busy，因為我很多things要do";
        String testMsg3 = "english testing";
        String testMsg4 = "import(引入) is one of the keep word in Java";

        LanguageDetector detector = LanguageDetectorBuilder.fromLanguages(Language.CHINESE, Language.ENGLISH).build();
//        String test = new String(testMsg2.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
//        System.out.println(test);
        System.out.println(testMsg2);
        System.out.println(detector.detectLanguageOf(testMsg2));
    }

    @Test
    void unicodeTest(){
        String test = "\u597d\u7684";
        System.out.println(test);
    }

    @Test
    void matcherTest(){
        String test = "0000A000-abcd";
        String result = "";
        Pattern pattern = Pattern.compile("^[0-9A-Z]{8}-(.*)$");
        Matcher matcher = pattern.matcher(test);
        matcher.find();
        result = matcher.group(1);
//        if(matcher.find())
//            result = matcher.group(1);
        System.out.println(result);
    }

    @Test
    void matcherTest2(){
        Pattern pattern = Pattern.compile("^\\[\"(.*)\"\\]$");
        Matcher matcher = pattern.matcher("[\"Methods\"]");
        String result = "";
        if(matcher.find())
            result = matcher.group(1);
        System.out.println(result);
    }

    @Test
    void allMatchTest(){
        List<String> components = Arrays.asList("[Sender ID]", "[Ref]", "[Channel]");
        String test = "[Sender ID][Channel]123";
        System.out.println(components.stream().allMatch(test::contains));
    }

    @Test
    void extractIdTest(){
        String rawMessage = "[fads]asfds \nafeafea \nfaegfa \n[Message ID] 456789\ntgfaga4eg";
        String temp = Arrays.stream(rawMessage.split("\n")).filter(line -> line.strip().startsWith("[Message ID] ")).findFirst().get().replace("[Message ID] ", "").strip();
        System.out.println(temp);
    }

    @Test
    void testParsePersonalScoreSheet() {
        HashMap<String, String> personalScoreMap = new HashMap<>();
        JSONObject scoreMap = new SheetsHandler("Java").readContentByKey("Grades", "0053A018");
        Iterator<String> jsonKey = scoreMap.keys();
        while (jsonKey.hasNext()) {
            String key = jsonKey.next();
            String keyName = "";
            String value = scoreMap.getJSONArray(key).getString(0);
            // check if key has entity name
            if (key.contains(" / ")) {
                keyName = key.split(" / ")[1];
            } else {
                keyName = key;
            }
            personalScoreMap.put(keyName, value);
        }
        System.out.println(personalScoreMap);
        System.out.println(personalScoreMap.get("midterm_exam"));
    }

    @Test
    void testParseKeywordSheet(){
        String keywordSheet = new SheetsHandler("Java").readContent("Keyword", "");
        Gson gson = new Gson();
        JsonArray keyword = gson.fromJson(keywordSheet, JsonArray.class);
//        System.out.println(keyword);
//        System.out.println(keyword.size());
        for(JsonElement element: keyword){
            JsonArray ele = element.getAsJsonArray();
//            System.out.println(ele);
//            System.out.println(ele.get(1).getAsString());
            if(Arrays.stream(ele.get(1).getAsString().split(",")).anyMatch(word -> word.strip().equals("Introduction")))
                System.out.println(ele.get(0).getAsString().strip());
        }
    }

    @Test
    void personalTextTest(){
        String query = new Neo4jHandler("Java").readPersonalizedSubjectMatter("0076D053");
        Gson gson = new Gson();
        JsonArray result = gson.fromJson(query, JsonArray.class);
        System.out.println(result);
        for(JsonElement chapter: result){
            System.out.println(chapter.getAsString());
        }
    }

    @Test
    void quizSearchTest(){
        JSONObject resp = new SheetsHandler("Java").readContentByKey("QuestionBank", "1");
        System.out.println(resp);
        JsonObject result = new JsonObject();
        Iterator<String> jsonKey = resp.keys();
        while(jsonKey.hasNext()){
            String key = jsonKey.next();
            String keyName = key.split(" / ")[1].strip();
            String value = resp.getJSONArray(key).getString(0);
//            result.addProperty(keyName, value);
            if(value.isEmpty()) continue;
//            System.out.println(keyName);
//            System.out.println(value);
            result.addProperty(keyName, value);
        }
        System.out.println(result);
        System.out.println(result.get("question").getAsString());
        System.out.println(result.get("ans").getAsString());
        System.out.println(result.get("optA").getAsString());
//        for(Map.Entry<String, JsonElement> entry: result.entrySet()){
//            System.out.println(entry.getKey());
//            System.out.println(entry.getValue().getAsString());
//        }
    }

    @Test
    void charTest(){
        char test = 'A';
        System.out.println(Character.toString(test + 1));
    }

    @Test
    public void readByValueTest() {
//        JSONObject value = new SheetsHandler("Java").readContentByKey("FAQ", "常見問題_Java亂碼");
//        JSONObject value = new SheetsHandler("Java").readContentByKey("FAQ", "java_garbled_code");
        JSONObject value = new SheetsHandler("Java").readContentByKey("Grades", "0053A018");
        System.out.println(value);

        // test iterator (iterate score map)
        Iterator<String> iter = value.keys();
        while(iter.hasNext()){
            String key = iter.next();
            System.out.println("key: " + key);
            System.out.println("value: " + value.getJSONArray(key).getString(0));
        }
//        System.out.println(value.getJSONArray("answer").get(0).toString());
    }

    private Firestore initFirestore() throws Exception{
        /* init phase */
        FileInputStream serviceAccount = new FileInputStream("src/main/resources/static/firebaseKey.json");
        FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
        FirebaseApp.initializeApp(options);
        System.out.println(">> Firebase init complete.");
        Firestore db = FirestoreClient.getFirestore();
        System.out.println(">> Firestore init complete.");
        return db;
    }

    @Test
    public void testFirestore() throws Exception{
//        /* init phase */
//        FileInputStream serviceAccount = new FileInputStream("src/main/resources/static/firebaseKey.json");
//        FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
//        FirebaseApp.initializeApp(options);
//        System.out.println(">> Firebase init complete.");
//        Firestore db = FirestoreClient.getFirestore();
//        System.out.println(">> Firestore init complete.");
        Firestore db = initFirestore();

        /* try to read data from firestore */
//        System.out.println(db.collection("tabotUser").get().get().getDocuments().get(0).getData());
        CollectionReference collection = db.collection("tabotUser");
//        System.out.println(">>>>> userList: " + collection.document("userData").get().get().getData());
//        HashMap<String, User> userMap = (HashMap)collection.document("userData").get().get().getData();
//        System.out.println(">>> UserMap: " + userMap);
//        System.out.println(userMap.size());
        System.out.println(">>>>> " + collection.document("userData").get().get().get("userList"));
        ArrayList userList = (ArrayList)collection.document("userData").get().get().get("userList");
        System.out.println(">>>>> class type: " + userList.get(0).getClass());
        System.out.println(">>>>> userList: " + userList);
        System.out.println(">>>>> first obj: " + userList.get(0));
        System.out.println(">>>>> cast to entity: " + new UserProfile((HashMap)userList.get(0)));

        /* try to write data from firestore */
    }

    @Test
    void TestFirestoreArray() throws Exception{
        Firestore db = initFirestore();
        HashMap<String, Object> test = new HashMap<>();
        test.put("name", "test");
        test.put("studentId", "0000");
        test.put("discordId", "fakeId");
        HashMap<String, Object> tester = new HashMap<>();
        tester.put("name", "tester");
        tester.put("studentId", "00000000");
        tester.put("discordId", "fakeDcId");
        UserProfile testProfile = new UserProfile(test);
        UserProfile testerProfile = new UserProfile(tester);
        /* try to add stuff in firestore database */
//        ApiFuture<WriteResult> future = db.collection("tabotUser").document("userData").set(new UserProfile("tester", "00000000", "fakeDdId").getProfileMap(), SetOptions.merge());
//        ApiFuture<WriteResult> future = db.collection("tabotUser").document("userData").update("userList", FieldValue.arrayUnion(new UserProfile("tester", "00000000", "anotherFakeId").getProfileMap()));
//        ApiFuture<WriteResult> future = db.collection("tabotUser").document("userData").update("userList", FieldValue.arrayUnion(testProfile.getProfileMap(), testerProfile.getProfileMap()));
        ArrayList<HashMap> testList = new ArrayList<>();
        testList.add(testProfile.getProfileMap());
        testList.add(testerProfile.getProfileMap());
        ApiFuture<WriteResult> future = db.collection("tabotUser").document("userData").update("userList", FieldValue.arrayUnion(testList.toArray()));
        System.out.println("Update time: " + future.get().getUpdateTime());
    }

    @Test
    void TestFirestoreRemove() throws Exception{
        Firestore db = initFirestore();
        HashMap<String, Object> test = new HashMap<>();
        test.put("name", "test");
        test.put("studentId", "0000");
        test.put("discordId", "fakeId");
        /* try to remove stuff from firestore */
        // create delete map
        Map<String, Object> deleteUpdate = new HashMap<>();
        deleteUpdate.put("userList", FieldValue.delete());
        DocumentReference docRef = db.collection("tabotUser").document("userData");
        ApiFuture<WriteResult> future = docRef.update(deleteUpdate);
        System.out.println(">>> " + future.get().toString());
    }

    @Test
    void sendMailTest(){
        String to = "dskyshad9527@gmail.com";
        String from = "noreply@test.com";
        String username = to;
        String pwd = "xygvtwhxgguhqcri";
        String host = "smtp.gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", 587);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, pwd);
            }
        });

        try{
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject("testing subject");
            msg.setText("hello from java mail");
            Transport.send(msg);
            System.out.println("try to send mail");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}