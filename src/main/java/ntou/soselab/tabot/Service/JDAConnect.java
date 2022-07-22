package ntou.soselab.tabot.Service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import ntou.soselab.tabot.Service.DiscordEvent.DiscordGeneralEventListener;
import ntou.soselab.tabot.Service.DiscordEvent.DiscordOnButtonClickListener;
import ntou.soselab.tabot.Service.DiscordEvent.DiscordOnMessageListener;
import ntou.soselab.tabot.Service.DiscordEvent.DiscordSlashCommandListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;
import java.util.List;

/**
 * JDA instance, define what discord bot could access
 */
@Service
public class JDAConnect {

    private JDA jda;
    private DiscordOnMessageListener onMessageListener;
    private DiscordOnButtonClickListener buttonListener;
    private DiscordGeneralEventListener generalEventListener;
    private DiscordSlashCommandListener slashCommandListener;
    private final String appToken;

    @Autowired
    public JDAConnect(Environment env, DiscordOnMessageListener onMessageListener, DiscordGeneralEventListener generalEventListener, DiscordOnButtonClickListener buttonEvt, DiscordSlashCommandListener slashCmdListener){
        this.onMessageListener = onMessageListener;
        this.buttonListener = buttonEvt;
        this.generalEventListener = generalEventListener;
        this.slashCommandListener = slashCmdListener;
        this.appToken = env.getProperty("discord.application.token");
    }

    /**
     * connect to discord when this class instance is created
     * this should be triggered by spring itself when this application startupConsumer
     */
    @PostConstruct
    private void init(){
        try{
            createJDAConnect(appToken);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("[JDA] initialize failed !");
        }
    }

    /**
     * create connect to discord by using server token
     * @param token server token
     * @throws LoginException if discord login failed
     */
    public void createJDAConnect(String token) throws LoginException {
        JDABuilder builder = JDABuilder.createDefault(token);

        configure(builder);
        // add customized Event Listener
        builder.addEventListeners(generalEventListener);
        // add customized MessageListener
        builder.addEventListeners(onMessageListener);
        // add customized Button onClick listener
        builder.addEventListeners(buttonListener);
        // add customized slash command listener
        builder.addEventListeners(slashCommandListener);
        jda = builder.build();
    }

    /**
     * discord bot setup
     * @param builder discord bot builder
     */
    public void configure(JDABuilder builder){
        // disable member activities (streaming / games / spotify)
//        builder.disableCache(CacheFlag.ACTIVITY);
        // disable member chunking on startup
//        builder.setChunkingFilter(ChunkingFilter.NONE);

        builder.enableIntents(GatewayIntent.GUILD_MEMBERS).setMemberCachePolicy(MemberCachePolicy.ALL);
    }

    /**
     * send message to target discord channel
     * @param channel channel name
     * @param msg message content
     */
    public void send(String channel, String msg){
        List<TextChannel> channels = jda.getTextChannelsByName(channel, true);
        for(TextChannel ch: channels){
            ch.sendMessage(msg).queue();
        }
    }

    /**
     * TESTING METHOD<br>
     * send message to target channel
     * @param serverId target server
     * @param channelId target channel
     * @param msg message
     */
    public void send(String serverId, String channelId, String msg){
        jda.getGuildById(serverId).getTextChannelById(channelId).sendMessage(msg).queue();
    }
}
