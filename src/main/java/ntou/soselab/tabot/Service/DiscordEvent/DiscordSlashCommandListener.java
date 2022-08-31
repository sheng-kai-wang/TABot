package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.tabot.Service.SlashCommandHandleService;
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
    SlashCommandHandleService slashCommandHandleService;
    private final String anonymousQuestionChannelName;
    private final String groupWorkspaceChannelName;
    private final Map<String, String> groupTopicMap;
    private final String groupNamePrefix;

    @Autowired
    public DiscordSlashCommandListener(Environment env) {
        this.anonymousQuestionChannelName = env.getProperty("discord.channel.anonymous-question.name");
        this.groupWorkspaceChannelName = env.getProperty("discord.channel.group-workspace.name");

        InputStream is = getClass().getResourceAsStream(env.getProperty("group-topic-map.path"));
        this.groupTopicMap = new Yaml().load(is);
        try {
            assert is != null;
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.groupNamePrefix = env.getProperty("judge-group-name.prefix");
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
        if (event.getName().equals("send_anonymous_question")) {
            // check user identity
//            if(event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(adminRoleId))){
//            }else {
//                event.reply("not enough permission to access").setEphemeral(true).queue();
////                event.reply("ok send send").setEphemeral(true).queue();
//            }
            MessageChannel targetChannel = DiscordGeneralEventListener.channelMap.get(anonymousQuestionChannelName);
            String question = event.getOption("question").getAsString();
            System.out.println("[Question] " + question);
            Message response = slashCommandHandleService.getAnonymousQuestionResponse(question);
            event.reply(response).setEphemeral(true).queue();
            targetChannel.sendMessage("[Question] " + question).queue();
        }

        if (event.getName().equals("read_ppt")) {
            System.out.println("[chapterNumber] all");
            Message response = slashCommandHandleService.readPpt();
            event.reply(response).setEphemeral(false).queue();
        }

        if (event.getName().equals("personal_quiz")) {

        }

        /* guild command */
        if (event.isFromGuild()) {
            System.out.println("[Channel] " + event.getChannel().getName());
            String groupName = judgeGroupName(event);
            System.out.println("[Group Name] " + groupName);
            if (groupName.equals(slashCommandHandleService.NO_GROUP)) {
                System.out.println("<<< end of current slash command event");
                System.out.println();
                if (!event.isAcknowledged()) {
                    event.reply("```[Warning] Sorry, you don't have a group yet.```")
                            .setEphemeral(true)
                            .queue();
                }
                return;
            }
            String groupTopic = groupTopicMap.get(groupName);
            System.out.println("[Group Topic] " + groupTopic);


            if (event.getName().equals("read_user_requirements")) {
                Message response = slashCommandHandleService.readUserRequirements(groupTopic, groupName);
                event.reply(response).setEphemeral(false).queue();
            }

            if (event.getName().equals("create_keep")) {
                String key = event.getOption("key").getAsString();
                System.out.println("[Key] " + key);
                String value = event.getOption("value").getAsString();
                System.out.println("[Value] " + value);
                Message response = slashCommandHandleService.createKeep(groupName, key, value);
                event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
            }

            if (event.getName().equals("read_keep")) {
                Message response = slashCommandHandleService.readKeep(groupName);
                event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
            }

            if (event.getName().equals("update_keep")) {
                String key = event.getOption("key").getAsString();
                System.out.println("[Key] " + key);
                String value = event.getOption("value").getAsString();
                Message response = slashCommandHandleService.updateKeep(groupName, key, value);
                event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
            }

            if (event.getName().equals("delete_keep")) {
                String key = event.getOption("key").getAsString();
                Message response = slashCommandHandleService.deleteKeep(groupName, key);
                event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
            }
        }
        System.out.println("<<< end of current slash command event");
        System.out.println();
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
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
//        super.onSelectionMenu(event);
    }

    private String judgeGroupName(SlashCommandEvent event) {
        try {
            List<Role> userRoles = event.getGuild().getMember(event.getUser()).getRoles();
            return userRoles.stream()
                    .filter(r -> r.getName().startsWith(groupNamePrefix))
                    .findFirst()
                    .get()
                    .getName();

        } catch (Exception e) {
            System.out.println("[Warning] no group");
            return slashCommandHandleService.NO_GROUP;
        }
    }

    private boolean isOutsideTheGroup(SlashCommandEvent event) {
        return !event.getChannel().getName().equals(groupWorkspaceChannelName);
    }
}
