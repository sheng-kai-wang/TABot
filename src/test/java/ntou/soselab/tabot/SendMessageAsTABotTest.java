package ntou.soselab.tabot;

import ntou.soselab.tabot.Service.JDAConnect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource("classpath:application.properties")
public class SendMessageAsTABotTest {

    @Autowired
    private JDAConnect jda;

    @Test
    public void sendSelectYourGroupMessage() {
        var channelId = "1006765298424217741"; // read-me channel
        var serverId = "1006763391563612181"; // SE_1111's course server
        var msg = "Please select your group by emoji reaction, \nyou can ONLY select one.";
        jda.send(serverId, channelId, msg);
    }

    @Test
    public void sendAnnouncementMessage() {
        String channelId = "1013136422468866109"; // announcement channel
        String serverId = "1006763391563612181"; // SE_1111's course server
        StringBuilder sb = new StringBuilder();
        sb.append("```md\n");
        sb.append("[系統公告]\n");
        sb.append("謝謝同學的提醒，目前 TABot 驗證信功能已經修復。");
        sb.append("原先的設計是如果同學已經投票選擇組別，就會無法收到驗證信，");
        sb.append("目前相關功能已經修復，");
        sb.append("請同學先將伺服器暱稱修改成別的格式，再改回來，就可以收到驗證信了，謝謝！\n");
        sb.append("```");
        jda.send(serverId, channelId, sb.toString());
    }
}
