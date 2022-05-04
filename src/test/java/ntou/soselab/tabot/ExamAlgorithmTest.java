package ntou.soselab.tabot;

import ntou.soselab.tabot.Service.IntentHandleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class ExamAlgorithmTest {

    @Autowired
    Environment env;

    @Test
    public void getPersonalQuizTest() {
        new IntentHandleService(env).getPersonalQuiz("00957055");
    }
}
