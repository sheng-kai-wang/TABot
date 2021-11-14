package ntou.soselab.tabot.Service;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JDAMessageHandleService {

    @Autowired
    public JDAMessageHandleService(Environment env){
    }

    public void sendMessage(TextChannel channel, String msg){
        // todo: send normal text message to assigned text channel
    }

    public void sendMessage(TextChannel channel, MessageEmbed embedMsg){
        // todo: send embed message to assigned text channel
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
     * @param id assigned student discord id
     * @param evt message received event
     */
    public void sendPrivateMessageOnReply(String id, MessageReceivedEvent evt){
        evt.getGuild().retrieveMemberById(id).queue(member -> {
            member.getUser().openPrivateChannel().queue(channel -> {
                channel.sendMessage(evt.getMessage()).queue();
            });
        });
    }
}
