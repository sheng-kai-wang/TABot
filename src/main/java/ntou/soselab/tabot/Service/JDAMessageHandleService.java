package ntou.soselab.tabot.Service;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import ntou.soselab.tabot.Service.DiscordEvent.DiscordGeneralEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JDAMessageHandleService {

    @Autowired
    public JDAMessageHandleService(Environment env){
    }

    /**
     * send message to assigned channel
     * @param channel target channel
     * @param msg message
     */
    public void sendMessage(TextChannel channel, String msg){
        channel.sendMessage(msg).queue();
    }

    /**
     * send embed message to assigned channel
     * @param channel assigned channel
     * @param embedMsg message
     */
    public void sendMessage(TextChannel channel, MessageEmbed embedMsg){
        channel.sendMessage(embedMsg).queue();
    }

    public void sendPrivateMessage(String id, String msg){
        // todo: send normal text message to assigned private channel with user id, this may require jda instance
    }

    public void sendPrivateMessage(String id, MessageEmbed embedMsg){
        // todo: send embed message to assigned private channel with user id, this may require jda instance
    }

    /**
     * send private message to assigned user
     * message content will be the raw message content from user input
     * @param senderId assigned student discord id
     * @param evt message received event
     */
    public void sendPrivateMessageOnReply(String senderId, MessageReceivedEvent evt){
        evt.getGuild().retrieveMemberById(senderId).queue(member -> {
            member.getUser().openPrivateChannel().queue(channel -> {
                channel.sendMessage(evt.getMessage()).queue();
            });
        });
    }

    /**
     * send message to assigned channel with reference
     * note: target channel must be IN the server
     * @param replyContent content
     * @param referenceId referenced message
     * @param channelName target channel name
     * @param evt message received event
     */
    public void sendPublicMessageWithReference(Message replyContent, String referenceId, String channelName, MessageReceivedEvent evt){
        if(DiscordGeneralEventListener.channelMap.containsKey(channelName)){
            DiscordGeneralEventListener.channelMap.get(channelName).sendMessage(replyContent).referenceById(referenceId).queue();
        }else{
            System.out.println("[sendPublicMessageWithReference] assigned channel not found.");
        }
    }
}
