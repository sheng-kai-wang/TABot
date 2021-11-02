package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.tabot.Service.IntentHandleService;
import ntou.soselab.tabot.Service.JDAMessageHandleService;
import ntou.soselab.tabot.Service.RasaService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * define what bot should do when message received from discord chatroom, include both public and private message
 * let this event have personal java class since there might be a lot of thing to do here, put it back to DiscordGeneralEventListener.java if necessary
 */
@Service
public class DiscordOnMessageListener extends ListenerAdapter {

    private final String adminChannelId;
    private final String botId;
    private JDAMessageHandleService jdaMsgHandleService;

    @Autowired
    public DiscordOnMessageListener(RasaService rasa, IntentHandleService intentHandle, JDAMessageHandleService jdaMsgHandle, Environment env){
        this.adminChannelId = env.getProperty("discord.admin.channel.id");
        this.botId = env.getProperty("discord.application.id");
        this.jdaMsgHandleService = jdaMsgHandle;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return; // ignore all message from bot

        // check if message come from manager channel (used to send message as bot by administrator)
        if(isFromAdmin(event.getChannel())){
            // todo: add prefix to check if admin want to send message by adding id himself ?
            // message come from admin channel, try to send direct message
            // todo: maybe check private or not ?, use private as default for now
            jdaMsgHandleService.sendPrivateMessageOnReply(extractStudentIdFromMessageLog(event.getMessage()), event);
        }

        // only react to stuff if bot got mentioned in general channels (student user available channel, to be specific)
        if(isBotMentioned(event)) {
            if (event.isFromType(ChannelType.PRIVATE)) {
                // todo: handle private message
            } else {
                // todo: handle public message
            }
        }
    }

    /**
     * check if bot itself (TABot in this case) got mentioned
     * @param evt message received event
     * @return true if mentioned, otherwise false
     */
    private boolean isBotMentioned(MessageReceivedEvent evt){
        if(evt.getMessage().getMentionedMembers().size() > 0) {
            return evt.getMessage().getMentionedMembers().get(0).getId().equals(botId);
        }
        return false;
    }

    /**
     * check if message comes from administrator's channel
     * compare incoming channel's id with admin-channel's id in application properties
     * @param channel detected channel
     * @return true if administrator's channel, otherwise false
     */
    private boolean isFromAdmin(MessageChannel channel){
        return channel instanceof TextChannel && channel.getId().equals(adminChannelId);
    }

    /**
     * extract student id from chat log and return correspond discord id
     * note: need to search database if discord id is not available in replied message log
     * this method should ONLY be used when REPLYING message (this message MUST have a reference message)
     * @param messageObj replied message
     * @return student discord id
     */
    private String extractStudentIdFromMessageLog(Message messageObj){
        // expect '[student id][student name] bla bla bla'
        // todo : maybe add discord id in log message to ?
        Message referenceMsg = messageObj.getReferencedMessage();
        System.out.println(referenceMsg);
        String targetStudentId = "nothing here dude";
        Pattern idPattern = Pattern.compile("^\\[([0-9]+)\\]\\[.*\\] .*$");
        Matcher matcher = idPattern.matcher(referenceMsg.getContentRaw());
        // todo: do stuff to retrieve student discord id, maybe search database or something
        if(matcher.find())
            targetStudentId = matcher.group(1);
        return targetStudentId;
    }
}
