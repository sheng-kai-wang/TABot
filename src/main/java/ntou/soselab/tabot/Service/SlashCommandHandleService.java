package ntou.soselab.tabot.Service;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import ntou.soselab.tabot.repository.RedisHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;

@Service
public class SlashCommandHandleService {

    @Autowired
    RedisHandler redisHandler;

    private final String userRequirementsFolderPath;
    private final static String DOWN_ARROW = "↓";

    @Autowired
    public SlashCommandHandleService(Environment env) {
        this.userRequirementsFolderPath = env.getProperty("user-requirements.folder.path");
    }

    public Message anonymousQuestion(String question) {
        MessageBuilder mb = new MessageBuilder();
        mb.append("ok, got it.\n");
        mb.append("Your question is `").append(question).append("`.\n");
        mb.append("It will be show on the \"anonymous_question\" channel.");
        return mb.build();
    }

    public Message userRequirements(String groupTopic, String groupName) {
        MessageBuilder mb = new MessageBuilder();
        String groupDocPath = userRequirementsFolderPath + File.separator + groupTopic + ".md";
        InputStream is = getClass().getResourceAsStream(groupDocPath);
        if (is != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            mb.append("here are the user requirements of your group. ( ").append(groupName).append(" )\n");
            mb.append("```markdown").append("\n");
            while (true) {
                try {
                    if (!br.ready()) break;
                    mb.append(br.readLine()).append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            mb.append("```");
        } else {
            mb.append("[Warning] Your user requirements is not found.");
            System.out.println("[Warning] User requirements is not found.");
        }
        return mb.build();
    }

    public Message createKeep(String groupName, String key, String value) {
        MessageBuilder mb = new MessageBuilder();
        if (redisHandler.hasContent(groupName, key)) {
            System.out.println("[Warning] This key already exists.");
            System.out.println("<<< end of current slash command event");
            System.out.println();
            return mb.append("[Warning] This key already exists.").build();
        }
        redisHandler.createPair(groupName, key, value);
        mb.append("ok, got it.\n");
        mb.append("you created a content:\n");
        mb.append("```properties\n");
        mb.append(key).append(" = ").append(value).append("\n");
        mb.append("```");
        return mb.build();
    }

    public Message readKeep(String groupName) {
        Map allPair = redisHandler.readPair(groupName);
        MessageBuilder mb = new MessageBuilder();
        if (allPair.size() == 0) {
            System.out.println("[Warning] no content yet.");
            System.out.println("<<< end of current slash command event");
            System.out.println();
            return mb.append("[Warning] no content yet.").build();
        }
        mb.append("ok, got it.\n");
        mb.append("The following are the contents of your group's keep:\n");
        mb.append("```properties\n");
        allPair.forEach((k, v) -> mb.append(k).append(" = ").append(v).append("\n"));
        mb.append("```");
        return mb.build();
    }

    public Message updateKeep(String groupName, String key, String value) {
        MessageBuilder mb = new MessageBuilder();
        if (!redisHandler.hasContent(groupName, key)) {
            System.out.println("[Warning] There is no such key in the keep.");
            System.out.println("<<< end of current slash command event");
            System.out.println();
            return mb.append("[Warning] There is no such key in the keep.").build();
        }
        String oldValue = redisHandler.updatePair(groupName, key, value);
        System.out.println("[Old Value] " + oldValue);
        System.out.println("[New Value] " + value);
        mb.append("ok, got it.\n");
        mb.append("you update a content:\n");
        mb.append("```properties\n");
        mb.append(key).append(" = ").append(oldValue).append("\n");
        mb.append(DOWN_ARROW).append("\n");
        mb.append(key).append(" = ").append(value).append("\n");
        mb.append("```");
        return mb.build();
    }

    public Message deleteKeep(String groupName, String key) {
        System.out.println("[Deleted Key] " + key);
        MessageBuilder mb = new MessageBuilder();
        if (!redisHandler.hasContent(groupName, key)) {
            System.out.println("[Warning] There is no such key in the keep.");
            System.out.println("<<< end of current slash command event");
            System.out.println();
            return mb.append("[Warning] There is no such key in the keep.").build();
        }
        String deletedValue = redisHandler.deletePair(groupName, key);
        System.out.println("[Deleted Value] " + deletedValue);
        mb.append("ok, got it.\n");
        mb.append("you deleted a content:\n");
        mb.append("```properties\n");
        mb.append(key).append(" = ").append(deletedValue).append("\n");
        mb.append("```");
        return mb.build();
    }
}
