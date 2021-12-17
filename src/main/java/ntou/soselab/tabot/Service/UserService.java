package ntou.soselab.tabot.Service;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import ntou.soselab.tabot.Entity.UserProfile;
import ntou.soselab.tabot.Exception.NoAccountFoundError;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final String FIREBASE_TOKEN;
    private final String COLLECTION_NAME;
    private final String DOCUMENT_NAME;
    private final String FIELD_NAME;
    private Firestore db;
    public static ArrayList<UserProfile> currentUserList;
    public static HashMap<String, UserProfile> verifyList;

    private final JavaMailSender mailSender;

    public UserService(Environment env, JavaMailSender mailSender){
        /* firebase and firestore properties */
        this.FIREBASE_TOKEN = env.getProperty("firebase.token.path");
        this.COLLECTION_NAME = env.getProperty("firebase.firestore.collection");
        this.DOCUMENT_NAME = env.getProperty("firebase.firestore.document");
        this.FIELD_NAME = env.getProperty("firebase.firestore.field");
        init(FIREBASE_TOKEN);
        initUserProfileList();
        // todo: complete firebase mail function and register function
        /* mail properties */
        this.mailSender = mailSender;
    }

    /**
     * initialize firebase, try to connect to firebase
     * @param tokenPath firebase auth token file path
     */
    private void init(String tokenPath){
        try{
            FileInputStream serviceAccount = new FileInputStream(tokenPath);

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
            FirebaseApp.initializeApp(options);
            System.out.println(">> Firebase init complete.");

            /* try to init firestore */
            db = FirestoreClient.getFirestore();
            System.out.println(">> Firestore init complete.");
        }catch (IOException e){
            e.printStackTrace();
            System.out.println(">> Firebase init failed.");
        }
    }

    /**
     * initialize user list from firestore
     */
    private void initUserProfileList(){
        try {
            /* create instance for static user list */
            currentUserList = new ArrayList<>();
            verifyList = new HashMap<>();
            /* retrieve previous registered user from firestore */
            ArrayList userList = (ArrayList) db.collection(COLLECTION_NAME).document(DOCUMENT_NAME).get().get().get(FIELD_NAME);
            if(userList == null) {
//                throw new NullPointerException("[DEBUG][init firestore] no previous registered user found on firestore.");
                // insert fake data and try to retrieve data again
                insertFakeUserDataToFirestore();
                userList = (ArrayList) db.collection(COLLECTION_NAME).document(DOCUMENT_NAME).get().get().get(FIELD_NAME);
            }else{
            }
            System.out.println(">> [DEBUG][init firestore] raw list from firestore: " + userList);
            /* create current user list */
            for(Object obj: userList){
                UserProfile user = new UserProfile((HashMap) obj);
                currentUserList.add(user);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            System.out.println("[DEBUG][init firestore] " + e.getMessage());
            System.out.println("[DEBUG][init firestore] this may be caused by empty data found on firestore, make sure firestore has at least one data.");
            e.printStackTrace();
        }
    }

    private void insertFakeUserDataToFirestore(){
        HashMap<String, Object> fakeUser = new HashMap<>();
        fakeUser.put("name", "fakeUser");
        fakeUser.put("studentId", "0");
        fakeUser.put("discordId", "0");
        ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME).document(DOCUMENT_NAME).update(FIELD_NAME, FieldValue.arrayUnion(new HashMap[]{fakeUser}));
        try{
            System.out.println("[DEBUG][UserService] insert fake user data : " + future.get().getUpdateTime());
            System.out.println(">>> ALERT : please remove fake user data from firestore manually !");
        }catch (InterruptedException | ExecutionException e){
            System.out.println("[DEBUG][UserService] failed to insert fake user data.");
            e.printStackTrace();
        }
    }

    /**
     * check if nickname matches specific pattern
     * @param nickname nickname
     * @return True if matches, False if not
     */
    public static boolean verifyNickNameFormat(String nickname){
        String format = "[0-9]{8}-.*";
        return Pattern.matches(format, nickname);
    }

    /**
     * get student's name from nickname
     * @param nickName
     * @return
     */
    public static String getNameByNickName(String nickName){
        Pattern pattern = Pattern.compile("^[0-9A-Z]{8}-(.*)$");
        Matcher matcher = pattern.matcher(nickName);
        matcher.find();
        return matcher.group(1);
    }

    public static String getStudentIdByNickName(String nickName){
        Pattern pattern = Pattern.compile("^([0-9A-Z]{8})-.*$");
        Matcher matcher = pattern.matcher(nickName);
        matcher.find();
        return matcher.group(1);
    }

    private String getNTOUEmail(String studentId){
        return studentId + "@mail.ntou.edu.tw";
    }

    /**
     * use discord id to retrieve student id
     * @param discordId target discord id
     * @return correspond student id if received discord id did exist in current user list
     */
    public String getStudentIdFromDiscordId(String discordId) throws NoAccountFoundError{
        if(!registeredBefore(discordId))
            throw new NoAccountFoundError("no user matched in current user list.");
        return currentUserList.stream().filter(user -> user.getDiscordId().equals(discordId)).findFirst().get().getStudentId();
    }

    /**
     * use discord id to retrieve student full name, including student id and name
     * @param discordId target discord id
     * @return correspond student full name if received discord id did exist in current user list
     * @throws NoAccountFoundError if no correspond user found by discord id
     */
    public String getFullNameFromDiscordId(String discordId) throws NoAccountFoundError{
        if(!registeredBefore(discordId))
            throw new NoAccountFoundError("no user matched in current user list.");
        return currentUserList.stream().filter(user -> user.getDiscordId().equals(discordId)).findFirst().get().getUserFullName();
    }

    /**
     * use student id to retrieve discord id
     * @param studentId target student id
     * @return correspond discord id if received student id did exist in current user list
     */
    public String getDiscordIdFromStudentId(String studentId) throws NoAccountFoundError {
        if(currentUserList.stream().noneMatch(user -> user.getStudentId().equals(studentId)))
            throw new NoAccountFoundError("no user matched in current user list.");
        return currentUserList.stream().filter(user -> user.getStudentId().equals(studentId)).findFirst().get().getDiscordId();
    }

    /**
     * send verify mail to user, create and return uuid for this application
     * @param studentId registrant's student id
     * @return uuid of this verify application
     */
    public String sendVerificationMail(String studentId){
        System.out.println("[DEBUG][UserService] try to send verification mail.");
        String receiverMailAddress = getNTOUEmail(studentId);
        // sender setup
        String senderMailAddress = "noreply@tabot.com";
        // generate uuid for current user
        UUID uuid = UUID.randomUUID();
        // generate verify link
        String verifyLink = generateVerifyLink(uuid.toString());

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(receiverMailAddress);
        mailMessage.setFrom(senderMailAddress);
        mailMessage.setSubject("TABot Verify Mail");
        mailMessage.setText("click the link below to verify your email.\n" + verifyLink);

        mailSender.send(mailMessage);
        System.out.println("[DEBUG][UserService] mail delivered, store uuid in to verify list.");
        return uuid.toString();
    }

    /**
     * create verify link with uuid
     * @param uuid string of uuid
     * @return verify link
     */
    private String generateVerifyLink(String uuid){
        return "http://localhost:8080/verify/" + uuid;
    }

    /**
     * register new student profile, remove existed profile with same discord id contained
     * @param nickName student's nickname
     * @param discordId student's discord id
     */
    public void registerStudent(String nickName, String discordId){
        String name = getNameByNickName(nickName);
        String studentId = getStudentIdByNickName(nickName);
        /* create new user profile */
        UserProfile registrant = new UserProfile(name, studentId, discordId);
        // local change
        currentUserList.removeIf(userProfile -> userProfile.getDiscordId().equals(discordId));
        currentUserList.add(registrant);
        // remote change
        removeFirestoreUserList();
        updateFirestoreUserList();
    }

    /**
     * register new student profile, remove existed profile if same discord id contained
     * @param registrantProfile student's profile
     */
    public void registerStudent(UserProfile registrantProfile){
        System.out.println("[DEBUG][UserService] try to add " + registrantProfile.getStudentId() + " in to current user list and update firestore.");
        currentUserList.removeIf(userProfile -> userProfile.getDiscordId().equals(registrantProfile.getDiscordId()));
        currentUserList.add(registrantProfile);
        removeFirestoreUserList();
        updateFirestoreUserList();
    }

    /**
     * check if received user discord id already existed in current user list
     * @param discordId user's discord id
     * @return true if same discord id already exist in current user list
     */
    public boolean registeredBefore(String discordId){
        return currentUserList.stream().anyMatch(user -> user.getDiscordId().equals(discordId));
    }

    /**
     * remove all user list from firestore
     */
    private void removeFirestoreUserList(){
        HashMap<String, Object> empty = new HashMap<>();
        empty.put("userList", FieldValue.delete());
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(DOCUMENT_NAME);
        ApiFuture<WriteResult> future = docRef.update(empty);
        try {
            System.out.println("[DEBUG][UserService] remove all user from userList at " + future.get().getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("[DEBUG][UserService] error occurs when trying to empty user list on firestore.");
            e.printStackTrace();
        }
    }

    /**
     * update firestore userList with current user list
     */
    public void updateFirestoreUserList(){
        ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME).document(DOCUMENT_NAME).update(FIELD_NAME, FieldValue.arrayUnion(getParsedCurrentUserMapList()));
        try {
            System.out.println("[DEBUG][UserService] Complete update userList at " + future.get().getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("[DEBUG][UserService] error occurs when trying to update user list to firestore.");
        }
    }

    /**
     * update new user's profile to firestore userList
     * @param user new user's UserProfile
     */
    public void updateFirestoreUserList(UserProfile user){
        ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME).document(DOCUMENT_NAME).update(FIELD_NAME, FieldValue.arrayUnion(user.getProfileMap()));
        try {
            System.out.println("[DEBUG][UserService] Complete update userList at " + future.get().getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("[DEBUG][UserService] error occurs when trying to update user list to firestore.");
        }
    }

    /**
     * parse current userProfile arraylist into arraylist of hashmap
     * @return current userProfile hashmap arraylist
     */
    private Object[] getParsedCurrentUserMapList(){
        ArrayList<HashMap> resultList = new ArrayList<>();
        for(UserProfile profile: currentUserList){
            resultList.add(profile.getProfileMap());
        }
        return resultList.toArray();
    }
}
