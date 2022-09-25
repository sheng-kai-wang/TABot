package ntou.soselab.tabot;

import ntou.soselab.tabot.Service.JDAConnect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource("classpath:application.properties")
public class SelectYourGroupTest {

    @Autowired
    private JDAConnect jda;

    @Test
    public void sendMessage() {
        var playGroundId = "1006765298424217741"; // read-me channel
        var serverId = "1006763391563612181"; // SE_1111's course server
        var msg = "Please select your group by emoji reaction, \nyou can ONLY select one.";
        jda.send(serverId, playGroundId, msg);
    }
}
