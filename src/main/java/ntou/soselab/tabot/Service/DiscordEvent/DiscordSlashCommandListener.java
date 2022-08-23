package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import ntou.soselab.tabot.repository.SheetsHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DiscordSlashCommandListener extends ListenerAdapter {

    private final String adminChannelId;
    private final String adminRoleId;
    private final String suggestChannelName;

    public DiscordSlashCommandListener(Environment env){
        this.adminChannelId = env.getProperty("discord.admin.channel.id");
        this.adminRoleId = env.getProperty("discord.admin.role.id");
        this.suggestChannelName = env.getProperty("discord.admin.channel.suggest.name");
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        /* global command */
        if(event.getName().equals("global_test")){
            System.out.println("[DEBUG] global slash command.");
            event.deferReply().queue();
            event.getHook().sendMessage("hello from global command").setEphemeral(true).queue();
        }
        if(event.getName().equals("contact_ta")){
            String msg = event.getOption("msg").getAsString();
            Member author = event.getMember();
            event.deferReply().queue();
            event.getHook().sendMessage("ok").setEphemeral(false).queue();
            // generate message
            MessageBuilder builder = new MessageBuilder();
            builder.append("[Sender ID] " + author.getId());
//            builder.append("[Ref] " + event.getChannel());
            if(!event.isFromGuild())
                builder.append("[Channel] private\n");
            else
                builder.append("[Channel] " + event.getChannel().getName() + "\n");
            builder.append("[RawContent] " + msg);
//            DiscordGeneralEventListener.guild.getTextChannelById(adminChannelId).sendMessage().queue();
        }
        /* guild command */
        if(event.getName().equals("guild_test")){
            event.reply("yaas, i hear [" + event.getOption("msg").getAsString() + "]").queue();
        }
        if(event.getName().equals("send_public_as_bot")){
            // check user identity
            if(event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(adminRoleId))){
                MessageChannel targetChannel = event.getOption("channel").getAsMessageChannel();
                event.reply("ok, got it").setEphemeral(true).queue();
                targetChannel.sendMessage(event.getOption("message").getAsString()).queue();
//                event.reply("ok send send").setEphemeral(true).queue();
            }else
                event.reply("not enough permission to access").setEphemeral(true).queue();
        }
        if(event.getName().equals("send_private_as_bot")){
            // check user identity
            if(event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(adminRoleId))){
                event.reply("ok, got it").setEphemeral(true).queue();
                event.getOption("user").getAsUser().openPrivateChannel().queue(channel -> {
                    channel.sendMessage(event.getOption("message").getAsString()).queue();
                });
            }else
                event.reply("not enough permission to access").setEphemeral(true).queue();
        }
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
