package ntou.soselab.tabot.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FirebaseService {

    private final String FIREBASE_TOKEN;
    private final String DATABASE_URL;

    private ActionCodeSettings actionCodeSettings;

    public FirebaseService(Environment env){
        this.FIREBASE_TOKEN = env.getProperty("firebase.token.path");
        this.DATABASE_URL = env.getProperty("firebase.database.url");
        init(FIREBASE_TOKEN, DATABASE_URL);
        // todo: complete firebase mail function and register function

        /* test block */
        // try to create user
//        createNewUser("286145047169335298", "10957033-david");
    }

    /**
     * initialize firebase, try to connect to firebase
     */
    public void init(String tokenPath, String databaseUrl){
        try{
            FileInputStream serviceAccount = new FileInputStream(tokenPath);

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
            FirebaseApp.initializeApp(options);
            System.out.println(">> Firebase init complete.");
        }catch (IOException e){
            e.printStackTrace();
            System.out.println(">> Firebase init failed.");
        }
    }

    public void generateMailVerifyLink(String studentId){
        String mail = studentId + "@mail.ntou.edu.tw";
        try{
            // todo: complete mail sending func
            String link = FirebaseAuth.getInstance().generateEmailVerificationLink(mail, ActionCodeSettings.builder().build());
        }catch (FirebaseAuthException e){
            e.printStackTrace();
            System.out.println("error generating verify email link.");
        }
    }

//    public void initActionCodeSetting(){
//        actionCodeSettings = ActionCodeSettings.builder()
//                .setHandleCodeInApp(true)
//                .build();
//    }

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
}
