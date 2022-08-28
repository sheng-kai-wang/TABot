package ntou.soselab.tabot;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.JsonObject;
import ntou.soselab.tabot.Entity.Student.StudentDiscordProfile;
import ntou.soselab.tabot.repository.SheetsHandler;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
class TaBotApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void languageTest() {
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
    void unicodeTest() {
        String test = "\u597d\u7684";
        System.out.println(test);
    }

    @Test
    void matcherTest() {
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
    void matcherTest2() {
        Pattern pattern = Pattern.compile("^\\[\"(.*)\"\\]$");
        Matcher matcher = pattern.matcher("[\"Methods\"]");
        String result = "";
        if (matcher.find())
            result = matcher.group(1);
        System.out.println(result);
    }

    @Test
    void allMatchTest() {
        List<String> components = Arrays.asList("[Sender ID]", "[Ref]", "[Channel]");
        String test = "[Sender ID][Channel]123";
        System.out.println(components.stream().allMatch(test::contains));
    }

    @Test
    void extractIdTest() {
        String rawMessage = "[fads]asfds \nafeafea \nfaegfa \n[Message ID] 456789\ntgfaga4eg";
        String temp = Arrays.stream(rawMessage.split("\n")).filter(line -> line.strip().startsWith("[Message ID] ")).findFirst().get().replace("[Message ID] ", "").strip();
        System.out.println(temp);
    }

    @Test
    void testParsePersonalScoreSheet() {
        HashMap<String, String> personalScoreMap = new HashMap<>();
        JSONObject scoreMap = new SheetsHandler("course").readContentByKey("Grades", "0053A018");
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

//    @Test
//    void personalTextTest() {
//        String query = new Neo4jHandler("Java").readPersonalizedSubjectMatter("0076D053");
//        Gson gson = new Gson();
//        JsonArray result = gson.fromJson(query, JsonArray.class);
//        System.out.println(result);
//        for (JsonElement chapter : result) {
//            System.out.println(chapter.getAsString());
//        }
//    }

    @Test
    void quizSearchTest() {
        JSONObject resp = new SheetsHandler("course").readContentByKey("QuestionBank", "1");
        System.out.println(resp);
        JsonObject result = new JsonObject();
        Iterator<String> jsonKey = resp.keys();
        while (jsonKey.hasNext()) {
            String key = jsonKey.next();
            String keyName = key.split(" / ")[1].strip();
            String value = resp.getJSONArray(key).getString(0);
//            result.addProperty(keyName, value);
            if (value.isEmpty()) continue;
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
    void charTest() {
        char test = 'A';
        System.out.println(Character.toString(test + 1));
    }

    @Test
    public void readByValueTest() {
//        JSONObject value = new SheetsHandler("Java").readContentByKey("FAQ", "常見問題_Java亂碼");
//        JSONObject value = new SheetsHandler("Java").readContentByKey("FAQ", "java_garbled_code");
        JSONObject value = new SheetsHandler("course").readContentByKey("Grades", "0053A018");
        System.out.println(value);

        // test iterator (iterate score map)
        Iterator<String> iter = value.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            System.out.println("key: " + key);
            System.out.println("value: " + value.getJSONArray(key).getString(0));
        }
//        System.out.println(value.getJSONArray("answer").get(0).toString());
    }

    private Firestore initFirestore() throws Exception {
        /* init phase */
        FileInputStream serviceAccount = new FileInputStream("src/main/resources/static/firebaseKey.json");
        FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
        FirebaseApp.initializeApp(options);
        System.out.println(">> Firebase init complete.");
        Firestore db = FirestoreClient.getFirestore();
        System.out.println(">> Firestore init complete.");
        return db;
    }

//    @Test
//    void testAssertAllUser() throws Exception {
//        File file = new File("./src/main/resources/students.txt");
//        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
//        String line;
//        while ((line = br.readLine()) != null) {
//            String studentId = line.split("-")[0];
//            String name = line.split("-")[1];
//            String discordId = line.split("-")[2];
//            testAssertNewUser(name, studentId, discordId);
//        }
//    }

//    @Test
//    public void testAssertNewUser() throws Exception {
//        String name = "李俊杰";
//        String studentId = "11057035";
//        String discordId = "222276938369073152";
//        StudentDiscordProfile user = new StudentDiscordProfile(name, studentId, discordId);
//
//        Firestore db = initFirestore();
//        ApiFuture<WriteResult> future = db.collection("tabotUser").document("userData").update("userList", FieldValue.arrayUnion(user.getProfileMap()));
//        try {
//            System.out.println("[DEBUG][UserService] Complete update userList at " + future.get().getUpdateTime());
//        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//            System.out.println("[DEBUG][UserService] error occurs when trying to update user list to firestore.");
//        }
//    }

    @Test
    public void testFirestore() throws Exception {
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
        ArrayList userList = (ArrayList) collection.document("userData").get().get().get("userList");
        System.out.println(">>>>> class type: " + userList.get(0).getClass());
        System.out.println(">>>>> userList: " + userList);
        System.out.println(">>>>> first obj: " + userList.get(0));
        System.out.println(">>>>> cast to entity: " + new StudentDiscordProfile((HashMap) userList.get(0)));

        /* try to write data from firestore */
    }

//    @Test
//    void TestFirestoreArray() throws Exception {
//        Firestore db = initFirestore();
//        HashMap<String, Object> test = new HashMap<>();
//        test.put("name", "test");
//        test.put("studentId", "0000");
//        test.put("discordId", "fakeId");
//        HashMap<String, Object> tester = new HashMap<>();
//        tester.put("name", "tester");
//        tester.put("studentId", "00000000");
//        tester.put("discordId", "fakeDcId");
//        StudentDiscordProfile testProfile = new StudentDiscordProfile(test);
//        StudentDiscordProfile testerProfile = new StudentDiscordProfile(tester);
//        /* try to add stuff in firestore database */
////        ApiFuture<WriteResult> future = db.collection("tabotUser").document("userData").set(new UserProfile("tester", "00000000", "fakeDdId").getProfileMap(), SetOptions.merge());
////        ApiFuture<WriteResult> future = db.collection("tabotUser").document("userData").update("userList", FieldValue.arrayUnion(new UserProfile("tester", "00000000", "anotherFakeId").getProfileMap()));
////        ApiFuture<WriteResult> future = db.collection("tabotUser").document("userData").update("userList", FieldValue.arrayUnion(testProfile.getProfileMap(), testerProfile.getProfileMap()));
//        ArrayList<HashMap> testList = new ArrayList<>();
//        testList.add(testProfile.getProfileMap());
//        testList.add(testerProfile.getProfileMap());
//        ApiFuture<WriteResult> future = db.collection("tabotUser").document("userData").update("userList", FieldValue.arrayUnion(testList.toArray()));
//        System.out.println("Update time: " + future.get().getUpdateTime());
//    }

//    @Test
//    void TestFirestoreRemove() throws Exception{
//        Firestore db = initFirestore();
//        HashMap<String, Object> test = new HashMap<>();
//        test.put("name", "test");
//        test.put("studentId", "0000");
//        test.put("discordId", "fakeId");
//        /* try to remove stuff from firestore */
//        // create delete map
//        Map<String, Object> deleteUpdate = new HashMap<>();
//        deleteUpdate.put("userList", FieldValue.delete());
//        DocumentReference docRef = db.collection("tabotUser").document("userData");
//        ApiFuture<WriteResult> future = docRef.update(deleteUpdate);
//        System.out.println(">>> " + future.get().toString());
//    }

//    @Test
//    void sendMailTest() {
//        String to = "dskyshad9527@gmail.com";
//        String from = "noreply@test.com";
//        String username = to;
//        String pwd = "";
//        String host = "smtp.gmail.com";
//
//        Properties props = new Properties();
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
//        props.put("mail.smtp.host", host);
//        props.put("mail.smtp.port", 587);
//
//        Session session = Session.getInstance(props, new Authenticator() {
//            @Override
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(username, pwd);
//            }
//        });
//
//        try {
//            Message msg = new MimeMessage(session);
//            msg.setFrom(new InternetAddress(from));
//            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
//            msg.setSubject("testing subject");
//            msg.setText("hello from java mail");
//            Transport.send(msg);
//            System.out.println("try to send mail");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    @Test
    void testArrayListParse() {
        StudentDiscordProfile profile1 = new StudentDiscordProfile("a", "1", "11");
        StudentDiscordProfile profile2 = new StudentDiscordProfile("b", "2", "22");
        ArrayList<StudentDiscordProfile> testList = new ArrayList<>();
        testList.add(profile1);
        testList.add(profile2);
        System.out.println(testList);
        ArrayList<HashMap> resultMapList = new ArrayList<>();
        for (StudentDiscordProfile profile : testList) {
            resultMapList.add(profile.getProfileMap());
        }
        System.out.println(resultMapList);
    }

//    @Test
//    void testRasaService(){
//        String testMsg = "什麼是介面設計";
//        String testMsg0 = "garbage collection";
//        String testMsgU = "\u529F\u80FD\u9700\u6C42\u7684\u5B9A\u7FA9";
//        String path = "http://localhost:5005/webhooks/rest/webhook";
//        JsonObject content = new JsonObject();
//        content.addProperty("sender", "test0");
//        content.addProperty("message", testMsg);
//        Gson gson = new Gson();
//        System.out.println(content.toString());
//
//        RestTemplate template = new RestTemplate();
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
//        HttpEntity<String> entity = new HttpEntity<>(content.toString(), headers);
//        ResponseEntity<String> resp = template.exchange(path, HttpMethod.POST, entity, String.class);
////        System.out.println("[raw] " + resp);
//        System.out.println("[raw body] " + resp.getBody());
//        JsonArray temp = gson.fromJson(resp.getBody(), JsonArray.class);
//        System.out.println(temp);
//        JsonObject intentJson = temp.get(0).getAsJsonObject();
//        System.out.println("intentJson: " + intentJson);
//        System.out.println(gson.fromJson(intentJson, Intent.class));
////        System.out.println(gson.fromJson(checkString(intentJson.toString()), Intent.class).getCustom().getEntity());
////        System.out.println(gson.fromJson(intentJson, Intent.class));
//
//        System.out.println("---");
////        String testJson = "{\"recipient_id\":\"test0\",\"custom\":{'intent': 'classmap_search', 'entity': '介面設計', 'endOfChat': True}}";
////        System.out.println(gson.fromJson(testJson, Intent.class));
////        String testJson2 = "{\\"recipient_id\\":\\"test0\\",\\"custom\\":\\"{'intent': 'classmap_search', 'entity': '介面設計', 'endOfChat': True}\\"}";
////        System.out.println(gson.fromJson(testJson2, Intent.class));
//
////        System.out.println(gson.fromJson(intentJson, Intent.class));
////        Intent intent = gson.fromJson(intentJson, Intent.class);
////        System.out.println(intent);
////        Intent intent = gson.fromJson(temp.get(0).toString(), Intent.class);
////        System.out.println(intent);
//    }

    private String checkString(String raw) {
        return raw.replace("\"{", "{").replace("}\"", "}");
    }

    private String removeBackSlash(String raw) {
        String[] token = raw.split("");
        StringBuilder result = new StringBuilder();
        for (String t : token) {
            if (t.equals("\\")) continue;
            if (t.equals("'")) {
                result.append("\"");
                continue;
            }
            result.append(t);
        }
        result.deleteCharAt(result.length() - 1);
        result.deleteCharAt(0);

        String temp = result.toString();
        StringBuilder output = new StringBuilder("{");
        String[] second = temp.split("");
        for (int i = 1; i < second.length; i++) {
            int open = StringUtils.countOccurrencesOf(temp.substring(0, i), "{");
            int close = StringUtils.countOccurrencesOf(temp.substring(0, i), "}");
            if (second[i].equals("\"")) {
                if (open - close == 1) {
                    if (second[i + 1].equals("{") || second[i - 1].equals("}")) {
                        continue;
                    }
                }
            }
            output.append(second[i]);
        }
        return output.toString();
    }

    @Test
    void testPath() {
        String fileName = "firebaseKey.json";
        URL url = getClass().getClassLoader().getResource(fileName);
        System.out.println(url.getPath());
    }

}