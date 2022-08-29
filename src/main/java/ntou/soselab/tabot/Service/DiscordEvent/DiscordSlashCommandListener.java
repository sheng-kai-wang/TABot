package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.tabot.repository.RedisHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.List;
import java.util.Map;

@Service
public class DiscordSlashCommandListener extends ListenerAdapter {

    @Autowired
    RedisHandler redisHandler;
    private final String anonymousQuestionChannelName;
    private final String groupWorkspaceChannelName;
    private final String userRequirementsFolderPath;
    private final Map<String, String> userRequirementsMap;
    private final static String GROUP_PREFIX = "GROUP";
    private final static String DOWN_ARROW = "↓";

    @Autowired
    public DiscordSlashCommandListener(Environment env) {
        this.anonymousQuestionChannelName = env.getProperty("discord.channel.anonymous-question.name");
        this.groupWorkspaceChannelName = env.getProperty("discord.channel.group-workspace.name");
        this.userRequirementsFolderPath = env.getProperty("user-requirements.folder.path");

        InputStream is = getClass().getResourceAsStream(env.getProperty("user-requirements.config.path"));
        this.userRequirementsMap = new Yaml().load(is);
        try {
            assert is != null;
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        System.out.println(">>> trigger slash command event");
        System.out.println("[DEBUG] " + event.getName());
        System.out.println("[User] " + event.getUser().getName());
//        if(event.getName().equals("global_test")){
//            System.out.println("[DEBUG] global slash command.");
//            event.deferReply().queue();
//            event.getHook().sendMessage("hello from global command").setEphemeral(true).queue();
//        }
//        if(event.getName().equals("contact_ta")){
//            String msg = event.getOption("msg").getAsString();
//            Member author = event.getMember();
//            event.deferReply().queue();
//            event.getHook().sendMessage("ok").setEphemeral(false).queue();
//            // generate message
//            MessageBuilder builder = new MessageBuilder();
//            builder.append("[Sender ID] " + author.getId());
////            builder.append("[Ref] " + event.getChannel());
//            if(!event.isFromGuild())
//                builder.append("[Channel] private\n");
//            else
//                builder.append("[Channel] " + event.getChannel().getName() + "\n");
//            builder.append("[RawContent] " + msg);
////            DiscordGeneralEventListener.guild.getTextChannelById(adminChannelId).sendMessage().queue();
//        }
        /* global command */
        if (event.getName().equals("anonymous_question")) {
            // check user identity
//            if(event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(adminRoleId))){
//            }else {
//                event.reply("not enough permission to access").setEphemeral(true).queue();
////                event.reply("ok send send").setEphemeral(true).queue();
//            }
            MessageChannel targetChannel = DiscordGeneralEventListener.channelMap.get(anonymousQuestionChannelName);
            String question = event.getOption("question").getAsString();
            System.out.println("[Question] " + question);
            MessageBuilder mb = new MessageBuilder();
            mb.append("ok, got it.\n");
            mb.append("Your question is `").append(question).append("`.\n");
            mb.append("It will be show on the \"anonymous_question\" channel.");
            event.reply(mb.build()).setEphemeral(true).queue();
            targetChannel.sendMessage("[Question] " + question).queue();
            return;
        }

        /* guild command */
        System.out.println("[Channel] " + event.getChannel().getName());
        String groupName = judgeGroupName(event);
        System.out.println("[Group Name] " + groupName);
        String groupTopic = userRequirementsMap.get(groupName);
        System.out.println("[Group Topic] " + groupTopic);

        if (event.getName().equals("read_user_requirements")) {
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
            }
            System.out.println("[Target Channel] " + event.getChannel());
            event.reply(mb.build()).setEphemeral(false).queue();
        }

        if (event.getName().equals("create_keep")) {
            String key = event.getOption("key").getAsString();
            System.out.println("[Key] " + key);
            String value = event.getOption("value").getAsString();
            System.out.println("[Value] " + value);
            redisHandler.createPair(groupName, key, value);
            MessageBuilder mb = new MessageBuilder();
            mb.append("ok, got it.\n");
            mb.append("you created a content:\n");
            mb.append("```properties\n");
            mb.append(key).append(" = ").append(value).append("\n");
            mb.append("```");
            event.reply(mb.build()).setEphemeral(isOutsideTheGroup(event)).queue();
        }

        if (event.getName().equals("read_keep")) {
            Map allPair = redisHandler.readPair(groupName);
            MessageBuilder mb = new MessageBuilder();
            mb.append("ok, got it.\n");
            mb.append("The following are the contents of your group's keep:\n");
            mb.append("```properties\n");
            allPair.forEach((k, v) -> mb.append(k).append(" = ").append(v).append("\n"));
            mb.append("```");
            event.reply(mb.build()).setEphemeral(isOutsideTheGroup(event)).queue();
        }

        if (event.getName().equals("update_keep")) {
            String key = event.getOption("key").getAsString();
            System.out.println("[Key] " + key);
            String value = event.getOption("value").getAsString();
            String oldValue = redisHandler.updatePair(groupName, key, value);
            System.out.println("[Old Value] " + oldValue);
            System.out.println("[New Value] " + value);
            MessageBuilder mb = new MessageBuilder();
            mb.append("ok, got it.\n");
            mb.append("you update a content:\n");
            mb.append("```properties\n");
            mb.append(key).append(" = ").append(oldValue).append("\n");
            mb.append(DOWN_ARROW).append("\n");
            mb.append(key).append(" = ").append(value).append("\n");
            mb.append("```");
            event.reply(mb.build()).setEphemeral(isOutsideTheGroup(event)).queue();
        }

        if (event.getName().equals("delete_keep")) {
            String key = event.getOption("key").getAsString();
            System.out.println("[Deleted Key] " + key);
            String deletedValue = redisHandler.deletePair(groupName, key);
            System.out.println("[Deleted Value] " + deletedValue);
            MessageBuilder mb = new MessageBuilder();
            mb.append("ok, got it.\n");
            mb.append("you deleted a content:\n");
            mb.append("```properties\n");
            mb.append(key).append(" = ").append(deletedValue).append("\n");
            mb.append("```");
            event.reply(mb.build()).setEphemeral(isOutsideTheGroup(event)).queue();
        }
//        if (event.getName().equals("send_private_as_bot")) {
//            // check user identity
//            if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(taRoleId))) {
//                event.reply("ok, got it").setEphemeral(true).queue();
//                event.getOption("user").getAsUser().openPrivateChannel().queue(channel -> {
//                    channel.sendMessage(event.getOption("message").getAsString()).queue();
//                });
//            } else
//                event.reply("not enough permission to access").setEphemeral(true).queue();
//        }
//        if(event.getName().equals("suggest_material")){
//            String sectionName = event.getOption("section").getAsString();
//            String title = event.getOption("title").getAsString();
//            String content = event.getOption("content").getAsString();
//            String note = event.getOption("note").getAsString();
//            Member author = event.getMember();
//            event.reply("ok, thanks for you suggestion.").setEphemeral(true).queue();
//            // add suggest to suggest-warehouse channel
//            MessageBuilder suggest = new MessageBuilder();
//            suggest.append("```")
//                    .append("[Referrer] " + author.getId() + "\n")
//                    .append("[Section] " + sectionName + "\n")
//                    .append("[Title] " + title + "\n")
//                    .append("[Content] " + content + "\n")
//                    .append("[note] " + note + "\n```");
//            DiscordGeneralEventListener.adminChannelMap.get(suggestChannelName).sendMessage(suggest.build()).queue();
//            // add to google sheet
//            new SheetsHandler("course").createContent("AuditList", new ArrayList<>(List.of(new ArrayList<>(List.of(sectionName, title, content, note)))));
//        }
        System.out.println("<<< end of current slash command event");
        System.out.println();
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
//        super.onSelectionMenu(event);
    }

    private String judgeGroupName(SlashCommandEvent event) {
        List<Role> userRoles = event.getGuild().getMember(event.getUser()).getRoles();
        return userRoles.stream()
                .filter(r -> r.getName().startsWith(GROUP_PREFIX))
                .findFirst()
                .get()
                .getName();
    }

    public boolean isOutsideTheGroup(SlashCommandEvent event) {
        return !event.getChannel().getName().equals(groupWorkspaceChannelName);
    }

}
