package ntou.soselab.tabot.Service;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import ntou.soselab.tabot.Exception.NoAccountFoundError;
import ntou.soselab.tabot.Service.DiscordEvent.DiscordGeneralEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class JDAMessageHandleService {

    private final String botId;
    private final String officeChannel;
    private final String miscLogChannel;
    private final String suggestAdminChannel;
    private final String suggestPassedChannel;

    private final UserService userService;

    @Autowired
    public JDAMessageHandleService(Environment env, UserService userService) {
        this.botId = env.getProperty("discord.application.id");
        this.officeChannel = env.getProperty("discord.channel.office.name");
        this.suggestAdminChannel = env.getProperty("discord.admin.channel.suggest.name");
        this.suggestPassedChannel = env.getProperty("discord.admin.channel.suggest.pass");
        this.miscLogChannel = env.getProperty("discord.channel.misc-log.name");

        this.userService = userService;
    }

    /**
     * send message to assigned channel
     *
     * @param channel target channel
     * @param msg     message
     */
    public void sendMessage(TextChannel channel, String msg) {
        channel.sendMessage(msg).queue();
    }

    /**
     * send embed message to assigned channel
     *
     * @param channel  assigned channel
     * @param embedMsg message
     */
    public void sendMessage(TextChannel channel, MessageEmbed embedMsg) {
        channel.sendMessage(embedMsg).queue();
    }

    public void sendMessage(List<TextChannel> channelList, Message message) {
        for (TextChannel channel : channelList) {
            channel.sendMessage(message).queue();
        }
    }

    public void sendPrivateMessage(User receiver, Message msg) {
        receiver.openPrivateChannel().queue(channel -> {
            channel.sendMessage(msg).queue();
        });
    }

    public void sendPrivateMessage(String studentId, Message msg) {
        try {
            System.out.println("[DEBUG][send private] triggered.");
            String studentDiscordId = UserService.currentUserList.stream().filter(profile -> profile.getStudentId().equals(studentId)).findFirst().get().getDiscordId();
            DiscordGeneralEventListener.guild.retrieveMemberById(studentDiscordId).queue(member -> {
                member.getUser().openPrivateChannel().queue(privateChannel -> {
                    privateChannel.sendMessage(msg).queue();
                });
            });
        } catch (NoSuchElementException e) {
            System.out.println("[DEBUG][send private by student id] no such user.");
        }
    }

    /**
     * send private message to assigned user
     * message content will be the raw message content from user input
     *
     * @param replyContent reply message
     * @param receiverId   assigned student discord id
     * @param messageId    original message id
     */
    public void replyPrivateMessage(Message replyContent, String receiverId, String messageId) {
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
     *
     * @param replyContent content
     * @param referenceId  referenced message
     * @param channelName  target channel name
     */
    public void replyPublicMessage(Message replyContent, String referenceId, String channelName) {
        if (DiscordGeneralEventListener.channelMap.containsKey(channelName)) {
            DiscordGeneralEventListener.channelMap.get(channelName).sendMessage(replyContent).referenceById(referenceId).queue();
        } else {
            System.out.println("[sendPublicMessageWithReference] assigned channel not found.");
        }
    }

    public void replyPublicMessage(Message replyContent, String originalMessageId, List<TextChannel> channelList) {
        for (TextChannel channel : channelList) {
            channel.retrieveMessageById(originalMessageId).queue(message -> {
                message.reply(replyContent).queue();
            });
        }
    }

    /**
     * send suggestion to suggestion list channel (wait to check by channel manager)
     * message format '[referrer name][referrer id][source message url]: message content'
     *
     * @param suggestion
     */
    public void addSuggestList(Message suggestion, Member author) {
        Message ref = null;
        if (suggestion.getReferencedMessage() != null)
            ref = suggestion.getReferencedMessage();
        MessageBuilder builder = new MessageBuilder();
        builder.append("[" + author.getNickname() + "][" + author.getId() + "][" + suggestion.getJumpUrl() + "]:\n " + suggestion.getContentRaw());
        if (suggestion.getAttachments().size() > 0) {
            for (Message.Attachment attach : suggestion.getAttachments())
                builder.append(attach.getUrl());
        }
        DiscordGeneralEventListener.adminChannelMap.get(suggestAdminChannel).sendMessage(builder.build()).queue();
    }

    /**
     * send PASSED suggestion to pending list channel
     *
     * @param passedMsg
     */
    public void sendSuggestPassMsg(Message passedMsg) {
        MessageBuilder builder = new MessageBuilder();
        builder.append(passedMsg.getContentRaw());
        if (passedMsg.getAttachments().size() > 0)
            for (Message.Attachment attachment : passedMsg.getAttachments())
                builder.append(attachment.getUrl());
        DiscordGeneralEventListener.adminChannelMap.get(suggestPassedChannel).sendMessage(builder.build()).queue();
    }

    public void sendSuggestPassMsg(MessageEmbed embedMsg) {
        DiscordGeneralEventListener.adminChannelMap.get(suggestPassedChannel).sendMessage(embedMsg).queue();
    }

    /**
     * send same message to admin channel (mention TABot)
     * expect message from public channel looks like this:
     * '@TABot bla bla bla'
     * note that message may have attachment object
     *
     * @param originalMsg
     * @param author
     */
    public void addAdminMentionedMessageList(Message originalMsg, User author) {
        String rawMsg = originalMsg.getContentDisplay().replace("@TABot", "").replace("```", "").strip();
//        if(!originalMsg.isFromGuild() && rawMsg.strip().startsWith("Bot"))
//            rawMsg = rawMsg.replaceFirst("Bot", "").strip();
        System.out.println("[DEBUG][jdaMsgHandle] raw message: " + rawMsg);
        /* create log message */
        MessageBuilder builder = new MessageBuilder();
        builder.append("[Sender ID] " + author.getId() + "\n");
        try {
            builder.append("[Sender Identity] " + userService.getFullNameFromDiscordId(author.getId()) + "\n");
        } catch (NoAccountFoundError e) {
            System.out.println("[DEBUG][add admin msg list] " + e.getMessage());
            e.printStackTrace();
        }
        builder.append("[Ref] " + originalMsg.getJumpUrl() + "\n");
        if (originalMsg.isFromType(ChannelType.PRIVATE)) {
            builder.append("[Category] private\n");
            builder.append("[Channel] private\n");
        } else {
            builder.append("[Category] " + originalMsg.getCategory().getName() + "\n");
            builder.append("[Channel] " + originalMsg.getTextChannel().getName() + "\n");
        }
        builder.append("[Message ID] " + originalMsg.getId() + "\n");
        builder.append("[RawContent] " + rawMsg + "\n");
        if (originalMsg.getAttachments().size() > 0)
            builder.append("[Attachment] " + originalMsg.getAttachments().get(0).getUrl());
        else
            builder.append("[Attachment] ");
        /* send log message */
        DiscordGeneralEventListener.adminChannelMap.get(officeChannel).sendMessage(builder.build()).queue();
        DiscordGeneralEventListener.adminChannelMap.get(officeChannel).sendMessage("-----").queue();
    }

    /**
     * add message log when @TABot triggered
     *
     * @param originalMsg
     * @param author
     */
    public void addMessageLog(Message originalMsg, User author) {
        System.out.println("[DEBUG][jdaMsgHandle] raw message: " + originalMsg.getContentDisplay());
        /* create log message */
        MessageBuilder builder = new MessageBuilder();
        builder.append("[Sender ID] " + author.getId() + "\n");
        try {
            builder.append("[Sender Identity] " + userService.getFullNameFromDiscordId(author.getId()) + "\n");
        } catch (NoAccountFoundError e) {
            System.out.println("[DEBUG][add misc log list] " + e.getMessage());
            e.printStackTrace();
        }
        builder.append("[Ref] " + originalMsg.getJumpUrl() + "\n");
        if (originalMsg.isFromType(ChannelType.PRIVATE)) {
            builder.append("[Category] private\n");
            builder.append("[Channel] private\n");
        } else {
            builder.append("[Category] " + originalMsg.getCategory().getName() + "\n");
            builder.append("[Channel] " + originalMsg.getTextChannel().getName() + "\n");
        }
        builder.append("[Message ID] " + originalMsg.getId() + "\n");
        builder.append("[RawContent Display] " + originalMsg.getContentDisplay() + "\n");
        if (originalMsg.getAttachments().size() > 0)
            builder.append("[Attachment] " + originalMsg.getAttachments().get(0).getUrl());
        else
            builder.append("[Attachment] ");
        /* send log message */
        DiscordGeneralEventListener.adminChannelMap.get(miscLogChannel).sendMessage(builder.build()).queue();
        DiscordGeneralEventListener.adminChannelMap.get(miscLogChannel).sendMessage("-----").queue();
    }
}
