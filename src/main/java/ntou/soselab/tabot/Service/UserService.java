package ntou.soselab.tabot.Service;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final String FIREBASE_TOKEN;
    private final String COLLECTION_NAME;
    private final String DOCUMENT_NAME;
    private Firestore db;

    private ActionCodeSettings actionCodeSettings;

    public UserService(Environment env){
        this.FIREBASE_TOKEN = env.getProperty("firebase.token.path");
        this.COLLECTION_NAME = env.getProperty("firebase.firestore.collection");
        this.DOCUMENT_NAME = env.getProperty("firebase.firestore.document");
        init(FIREBASE_TOKEN);
        // todo: complete firebase mail function and register function

        /* test block */
        // try to create user
//        createNewUser("286145047169335298", "10957033-david");
    }

    /**
     * initialize firebase, try to connect to firebase
     */
    public void init(String tokenPath){
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

    public void createNewUser(String id, String nickName){
        String studentId = getStudentIdByNickName(nickName);
        String studentName = getNameByNickName(nickName);
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setUid(id)
                .setEmail(getNTOUEmail(studentId))
//                .setEmail("david02653@gmail.com")
                .setEmailVerified(false)
                .setDisplayName(studentName);

        try {
            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
            System.out.println("Successfully create new user " + userRecord.getDisplayName());
        } catch (FirebaseAuthException e) {
            e.printStackTrace();
        }
    }

    private String getNameByNickName(String nickName){
        Pattern pattern = Pattern.compile("^[0-9A-Z]{8}-(.*)$");
        Matcher matcher = pattern.matcher(nickName);
        matcher.find();
        return matcher.group(1);
    }

    private String getStudentIdByNickName(String nickName){
        Pattern pattern = Pattern.compile("^([0-9A-Z]{8})-.*$");
        Matcher matcher = pattern.matcher(nickName);
        matcher.find();
        return matcher.group(1);
    }

    private String getNTOUEmail(String studentId){
        return studentId + "@mail.ntou.edu.tw";
    }

    public void getStudentIdFromDiscordId(String discordId){
        // todo: use discord id to retrieve student id
    }

    public void getDiscordIdFromStudentId(String studentId){
        // todo: use student id to retrieve discord id
    }

    public void sendVerificationMail(String studentId){
        // todo: send verification mail to target student
    }

    public void getUserList() throws ExecutionException, InterruptedException {
        // todo: retrieve user list from firestore
//        CollectionReference userData = db.collection("tabotUser");
//        userData.get().get().getDocuments().get(0).getData();
        DocumentReference userData = db.collection(COLLECTION_NAME).document(DOCUMENT_NAME);
        // asynchronously retrieve document from firestore

    }

    public void registerStudent(String nickName){
        // todo: register new student
    }

    public void writeUserListFile(){
        // todo: write user data in local file
    }

    public void updateUserListFile(){
        // todo: update user data file
    }
}
