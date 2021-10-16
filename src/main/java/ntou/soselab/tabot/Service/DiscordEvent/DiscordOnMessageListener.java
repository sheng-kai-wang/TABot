package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * define what bot should do when message received from discord chatroom, include both public and private message
 * let this event have personal java class since there might be a lot of thing to do here, put it back to DiscordGeneralEventListener.java if necessary
 */
@Service
public class DiscordOnMessageListener extends ListenerAdapter {

    @Autowired
    public DiscordOnMessageListener(Environment env){
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return; // ignore all message from bot

        if(event.isFromType(ChannelType.PRIVATE)){
            // todo: handle private message
        }else{
            // todo: handle public message
        }
    }
}
