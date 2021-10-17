package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * define what bot should do on each event received
 */
@Service
public class DiscordGeneralEventListener extends ListenerAdapter {

    private final String testUserId = "286145047169335298";
    private final String serverId;

    @Autowired
    public DiscordGeneralEventListener(Environment env){
        this.serverId = env.getProperty("discord.server.id");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        // all jda entity loaded successfully
        System.out.println(">> onReady");
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        // jda will start dispatching events related to this guild, initialize service depend on this guild
        System.out.println(">> onGuildReady");
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        // todo: give role when correct nickname format detected on guild-member nickname change
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        // todo: store all user id when new user join server
    }
}
