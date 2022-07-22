package ntou.soselab.tabot;

import ntou.soselab.tabot.Service.JDAConnect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource("classpath:application.properties")
public class TABotTest {

    @Autowired
    private JDAConnect jda;

    @Test
    public void testConnectionTODiscord(){
        // play ground id: 999939292640063568
        var playGroundId = "999939292640063568";
        var serverId = "955883376487833630";
        var msg = "testing message";
        jda.send(serverId, playGroundId, msg);
    }
}
