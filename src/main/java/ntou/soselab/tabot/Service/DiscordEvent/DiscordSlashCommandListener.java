package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.tabot.Exception.NoAccountFoundError;
import ntou.soselab.tabot.Service.SlashCommandHandleService;
import ntou.soselab.tabot.Service.UserService;
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
    @Autowired
    UserService userService;
    private final String anonymousQuestionChannelName;
    private final String groupWorkspaceChannelNamePrefix;
    private final Map<String, String> groupTopicMap;
    private final String groupNamePrefix;

    @Autowired
    public DiscordSlashCommandListener(Environment env) {
        this.anonymousQuestionChannelName = env.getProperty("discord.channel.anonymous-question.name");
        this.groupWorkspaceChannelNamePrefix = env.getProperty("discord.channel.group-workspace.name.prefix");

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
        String userName = event.getUser().getName();
        System.out.println("[User Name] " + userName);
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
            MessageChannel targetChannel = DiscordGeneralEventListener.channelMap.get(anonymousQuestionChannelName);
            String question = event.getOption("question").getAsString();
            System.out.println("[Question] " + question);
            Message response = slashCommandHandleService.getAnonymousQuestionResponse(question);
            event.reply(response).setEphemeral(true).queue();
            targetChannel.sendMessage("```yaml" + "\nQuestion: " + question + "```").queue();
        }

        if (event.getName().equals("read_ppt")) {
            System.out.println("[Chapter Number] all");
            Message response = slashCommandHandleService.readPpt();
            event.reply(response).setEphemeral(false).queue();
        }

        if (event.getName().equals("personal_quiz")) {
//            event.reply("Set a question...").setEphemeral(true).queue();
            event.deferReply().setEphemeral(true).queue();
            String studentId = judgeStudentId(event);
            System.out.println("[Student ID] " + studentId);
            Message response = slashCommandHandleService.personalQuiz(studentId);
            event.getHook().sendMessage(response).setEphemeral(true).queue();
        }

        if (event.getName().equals("personal_score")) {
//            event.reply("Loading...").setEphemeral(true).queue();
            event.deferReply().setEphemeral(true).queue();
            String studentId = judgeStudentId(event);
            System.out.println("[Student ID] " + studentId);
            Message response = slashCommandHandleService.personalScore(studentId);
            event.getHook().sendMessage(response).setEphemeral(true).queue();
        }

        /* guild command */
        if (event.isFromGuild()) {
            System.out.println("[Channel] " + event.getChannel().getName());
            String groupName = judgeGroupName(event);
            System.out.println("[Group Name] " + groupName);
            if (groupName.equals(SlashCommandHandleService.NO_GROUP)) {
                System.out.println("<<< end of current slash command event");
                System.out.println();
                if (!event.isAcknowledged()) {
                    event.reply("```properties" + "\n[WARNING] Sorry, you don't have a group yet.```")
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
                String keys = event.getOption("keys").getAsString();
                System.out.println("[Keys] " + keys);
                String value = event.getOption("value").getAsString();
                System.out.println("[Value] " + value);
                Message response = slashCommandHandleService.createKeep(groupName, keys, value);
                event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
            }

            if (event.getName().equals("create_aliases_of_key")) {
                String key = event.getOption("key").getAsString();
                System.out.println("[Key] " + key);
                String aliases = event.getOption("key_aliases").getAsString();
                System.out.println("[Key Aliases] " + aliases);
                Message response = slashCommandHandleService.createAliasesOfKey(groupName, key, aliases);
                event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
            }

            if (event.getName().equals("delete_aliases_of_key")) {
                String key = event.getOption("key").getAsString();
                System.out.println("[Key] " + key);
                String aliases = event.getOption("key_aliases").getAsString();
                System.out.println("[Key Aliases] " + aliases);
                Message response = slashCommandHandleService.deleteAliasesOfKey(groupName, key, aliases);
                event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
            }

            if (event.getName().equals("read_keep")) {
                String key = null;
                try {
                    key = event.getOption("key").getAsString();
                    System.out.println("[Key] " + key);
                } catch (Exception e) {
                    System.out.println("[Key] no key");
                } finally {
                    Message response = slashCommandHandleService.readKeep(groupName, key);
                    event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
                }
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
                System.out.println("[Deleted Key] " + key);
                Message response = slashCommandHandleService.deleteKeep(groupName, key);
                event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
            }

            if (event.getName().equals("set_github_repository")) {
                String url = event.getOption("https_url").getAsString();
                System.out.println("[Https Url] " + url);
                Message response = slashCommandHandleService.setRepository(groupName, url);
                event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
            }

            if (event.getName().equals("contribution_analysis")) {
                Message response = slashCommandHandleService.contributionAnalysis(groupName, groupTopic);
                event.reply(response).setEphemeral(isOutsideTheGroup(event)).queue();
            }

            if (event.getName().equals("commitment_retrieval")) {
                event.deferReply().queue();
                String keywords = event.getOption("keywords").getAsString();
                System.out.println("[Keywords] " + keywords);
                String repository = null;
                String branch = null;
                int quantity = 5;
                try {
                    repository = event.getOption("repository_name").getAsString();
                    System.out.println("[Repository Name] " + repository);
                } catch (Exception e) {
                    System.out.println("[Repository Name] all repo");
                } finally {
                    try {
                        branch = event.getOption("branch_name").getAsString();
                        System.out.println("[Branch Name] " + branch);
                    } catch (Exception e) {
                        System.out.println("[Branch Name] all branch");
                    } finally {
                        try {
                            quantity = (int) event.getOption("quantity").getAsLong();
                            System.out.println("[Quantity] " + quantity);
                        } catch (Exception e) {
                            System.out.println("[Quantity] 5 (default value)");
                        } finally {
                            Message response = slashCommandHandleService.commitmentRetrieval(groupName, repository, branch, keywords, quantity);
                            event.getHook().sendMessage(response).setEphemeral(true).queue();
                        }
                    }
                }
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
            System.out.println("[WARNING] no group");
            return SlashCommandHandleService.NO_GROUP;
        }
    }

    private String judgeStudentId(SlashCommandEvent event) {
        try {
            return userService.getStudentIdFromDiscordId(event.getUser().getId());
        } catch (NoAccountFoundError e) {
            e.printStackTrace();
            return SlashCommandHandleService.NOT_STUDENT;
        }
//            try {
//                studentId = userName.split("-")[0];
//            } catch (Exception e) {
//                studentId = slashCommandHandleService.NOT_STUDENT;
//            }
    }

    private boolean isOutsideTheGroup(SlashCommandEvent event) {
        return !event.getChannel().getName().startsWith(groupWorkspaceChannelNamePrefix);
    }
}
