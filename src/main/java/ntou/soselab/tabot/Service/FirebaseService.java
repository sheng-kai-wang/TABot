package ntou.soselab.tabot.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Service
public class FirebaseService {

    private final String FIREBASE_TOKEN;
    private final String DATABASE_URL;

    public FirebaseService(Environment env){
        this.FIREBASE_TOKEN = env.getProperty("firebase.token.path");
        this.DATABASE_URL = env.getProperty("firebase.database.url");
        init(FIREBASE_TOKEN, DATABASE_URL);
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
}
