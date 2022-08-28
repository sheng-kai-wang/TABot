package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class DiscordSlashCommandListener extends ListenerAdapter {

    private final String anonymousQuestionChannelName;
    private final String userRequirementsFolderPath;
    private final Map<String, String> userRequirementsConfig;
    private final static String GROUP_PREFIX = "GROUP";

    public DiscordSlashCommandListener(Environment env) {
        this.anonymousQuestionChannelName = env.getProperty("discord.channel.anonymous-question.name");
        this.userRequirementsFolderPath = env.getProperty("user-requirements.folder.path");

        InputStream is = getClass().getResourceAsStream(env.getProperty("user-requirements.config.path"));
        this.userRequirementsConfig = new Yaml().load(is);
        try {
            assert is != null;
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        /* global command */
        System.out.println(">>> trigger slash command event");
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
        if (event.getName().equals("anonymous_question")) {
            // check user identity
//            if(event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(adminRoleId))){
//            }else {
//                event.reply("not enough permission to access").setEphemeral(true).queue();
////                event.reply("ok send send").setEphemeral(true).queue();
//            }
            System.out.println("[DEBUG] get anonymous question");
            MessageChannel targetChannel = DiscordGeneralEventListener.channelMap.get(anonymousQuestionChannelName);
            String question = event.getOption("question").getAsString();
            System.out.println("[Question] " + question);
            MessageBuilder mb = new MessageBuilder();
            mb.append("ok, got it.\n");
            mb.append("Your question is `" + question + "`.\n");
            mb.append("It will be show on the \"anonymous_question\" channel.");
            event.reply(mb.build()).setEphemeral(true).queue();
            targetChannel.sendMessage("[Question] " + question).queue();
        }

        if (event.getName().equals("show_user_requirements")) {
            System.out.println("[DEBUG] show user requirements");
            StringBuilder sb = new StringBuilder();
            System.out.println("[Requester] " + event.getUser().getName());
            String groupName = judgeGroupName(event);
            System.out.println("[Group Name] " + groupName);
            String groupTopic = userRequirementsConfig.get(groupName);
            System.out.println("[Group Topic] " + groupTopic);
            String groupDocPath = userRequirementsFolderPath + File.separator + groupTopic + ".md";
            InputStream is = getClass().getResourceAsStream(groupDocPath);
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                sb.append("here are the user requirements of your group.\n");
                sb.append("```markdown").append("\n");
                while (true) {
                    try {
                        if (!br.ready()) break;
                        sb.append(br.readLine()).append("\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                sb.append("```");
            }
            System.out.println("[Target Channel] " + event.getChannel());
            event.reply(sb.toString()).setEphemeral(false).queue();
        }

        if (event.getName().equals("add_keep")) {
            System.out.println("[DEBUG] add keep note");
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
}
