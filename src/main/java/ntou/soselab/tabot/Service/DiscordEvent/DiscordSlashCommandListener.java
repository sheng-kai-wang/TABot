package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class DiscordSlashCommandListener extends ListenerAdapter {

    //    private final String adminChannelId;
    private final String taRoleId;
    private final String anonymousQuestionChannelName;
//    private final String suggestChannelName;

    public DiscordSlashCommandListener(Environment env) {
//        this.adminChannelId = env.getProperty("discord.admin.channel.id");
        this.taRoleId = env.getProperty("discord.role.ta");
//        this.suggestChannelName = env.getProperty("discord.admin.channel.suggest.name");
        this.anonymousQuestionChannelName = env.getProperty("discord.channel.anonymous-question.name");
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        /* global command */
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
        /* guild command */
//        if(event.getName().equals("guild_test")){
//            event.reply("yaas, i hear [" + event.getOption("msg").getAsString() + "]").queue();
//        }
        if (event.getName().equals("anonymous_question")) {
            // check user identity
//            if(event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(adminRoleId))){
//            }else {
//                event.reply("not enough permission to access").setEphemeral(true).queue();
////                event.reply("ok send send").setEphemeral(true).queue();
//            }
            MessageChannel targetChannel = DiscordGeneralEventListener.channelMap.get(anonymousQuestionChannelName);
            String question = event.getOption("question").getAsString();
            MessageBuilder mb = new MessageBuilder();
            mb.append("ok, got it.\n");
            mb.append("Your question is `" + question + "`.\n");
            mb.append("It will be show on the \"anonymous_question\" channel.");
            event.reply(mb.build()).setEphemeral(true).queue();
            targetChannel.sendMessage("[Question] " + question).queue();
        }
        if (event.getName().equals("user_requirements")) {
             event.getChannel();
//            event.reply().setEphemeral(false).queue();
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
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
//        super.onSelectionMenu(event);
    }
}
