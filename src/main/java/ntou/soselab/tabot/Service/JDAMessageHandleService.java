package ntou.soselab.tabot.Service;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import ntou.soselab.tabot.Service.DiscordEvent.DiscordGeneralEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JDAMessageHandleService {

    private final String botId;
    private final String adminChannel;
    private final String suggestAdminChannel;
    private final String suggestPassedChannel;

    @Autowired
    public JDAMessageHandleService(Environment env){
        this.botId = env.getProperty("discord.application.id");
        this.adminChannel = env.getProperty("discord.admin.channel.name");
        this.suggestAdminChannel = env.getProperty("discord.admin.channel.suggest.name");
        this.suggestPassedChannel = env.getProperty("discord.admin.channel.suggest.pass");
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
     * @param replyContent reply message
     * @param receiverId assigned student discord id
     * @param messageId original message id
     */
    public void replyPrivateMessage(Message replyContent, String receiverId, String messageId){
        DiscordGeneralEventListener.guild.retrieveMemberById(receiverId).queue(member -> {
            member.getUser().openPrivateChannel().queue(channel -> {
                channel.retrieveMessageById(messageId).queue(msg -> {
                    msg.reply(replyContent).queue();
                });
            });
        });
    }

    /**
     * send message to assigned channel with reference
     * note: target channel must be IN the server
     * @param replyContent content
     * @param referenceId referenced message
     * @param channelName target channel name
     */
    public void replyPublicMessage(Message replyContent, String referenceId, String channelName){
        if(DiscordGeneralEventListener.channelMap.containsKey(channelName)){
            DiscordGeneralEventListener.channelMap.get(channelName).sendMessage(replyContent).referenceById(referenceId).queue();
        }else{
            System.out.println("[sendPublicMessageWithReference] assigned channel not found.");
        }
    }

    /**
     * send suggestion to suggestion list channel (wait to check by channel manager)
     * message format '[referrer name][referrer id][source message url]: message content'
     * @param suggestion
     */
    public void addSuggestList(Message suggestion, Member author){
        Message ref = null;
        if(suggestion.getReferencedMessage() != null)
            ref = suggestion.getReferencedMessage();
        MessageBuilder builder = new MessageBuilder();
        builder.append("[" + author.getNickname() + "][" + author.getId() + "][" + suggestion.getJumpUrl() + "]:\n " + suggestion.getContentRaw());
        if(suggestion.getAttachments().size() > 0){
            for(Message.Attachment attach: suggestion.getAttachments())
                builder.append(attach.getUrl());
        }
        DiscordGeneralEventListener.adminChannelMap.get(suggestAdminChannel).sendMessage(builder.build()).queue();
    }

    /**
     * send PASSED suggestion to pending list channel
     * @param passedMsg
     */
    public void sendSuggestPassMsg(Message passedMsg){
        MessageBuilder builder = new MessageBuilder();
        builder.append(passedMsg.getContentRaw());
        if(passedMsg.getAttachments().size() > 0)
            for(Message.Attachment attachment: passedMsg.getAttachments())
                builder.append(attachment.getUrl());
        DiscordGeneralEventListener.adminChannelMap.get(suggestPassedChannel).sendMessage(builder.build()).queue();
    }
    public void sendSuggestPassMsg(MessageEmbed embedMsg){
        DiscordGeneralEventListener.adminChannelMap.get(suggestPassedChannel).sendMessage(embedMsg).queue();
    }

    /**
     * send same message to admin channel if student want to talk to admin directly (mention TA)
     * expect message from public channel looks like this:
     * '@TA bla bla bla'
     * note that message may have attachment object
     * @param originalMsg
     * @param author
     */
    public void addAdminMentionedMessageList(Message originalMsg, User author){
        String rawMsg = originalMsg.getContentDisplay().replace("@TA", "").replace("```", "").strip();
        if(!originalMsg.isFromGuild() && rawMsg.strip().startsWith("Bot"))
            rawMsg = rawMsg.replaceFirst("Bot", "").strip();
        System.out.println(rawMsg);
        MessageBuilder builder = new MessageBuilder();
        builder.append("[Sender ID] " + author.getId() + "\n")
                .append("[Ref] " + originalMsg.getJumpUrl() + "\n");
        if(originalMsg.isFromType(ChannelType.PRIVATE))
            builder.append("[Channel] private\n");
        else
            builder.append("[Channel] " + originalMsg.getTextChannel().getName() + "\n");
        builder.append("[Message ID] " + originalMsg.getId() + "\n");
        builder.append("[RawContent] " + rawMsg + "\n");
        if(originalMsg.getAttachments().size() > 0)
            builder.append("[Attachment] " + originalMsg.getAttachments().get(0).getUrl());
        else
            builder.append("[Attachment] ");
        DiscordGeneralEventListener.adminChannelMap.get(adminChannel).sendMessage(builder.build()).queue();
        DiscordGeneralEventListener.adminChannelMap.get(adminChannel).sendMessage("-----").queue();
    }

    public void addAdminMentionedMessageList(String msg, Member author){
    }
}
