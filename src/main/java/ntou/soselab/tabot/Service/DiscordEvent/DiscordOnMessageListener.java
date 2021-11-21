package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.tabot.Entity.ChatStatus;
import ntou.soselab.tabot.Entity.Rasa.Intent;
import ntou.soselab.tabot.Service.IntentHandleService;
import ntou.soselab.tabot.Service.JDAMessageHandleService;
import ntou.soselab.tabot.Service.RasaService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;


import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * define what bot should do when message received from discord chatroom, include both public and private message
 * let this event have personal java class since there might be a lot of thing to do here, put it back to DiscordGeneralEventListener.java if necessary
 */
@Service
public class DiscordOnMessageListener extends ListenerAdapter {

    private final String adminChannelId;
    private final String adminSuggestChannelId;
    private final String botId;
    private final String adminRoleId;
    private JDAMessageHandleService jdaMsgHandleService;
    private IntentHandleService intentHandleService;
    // rasa service
    private final RasaService rasa;

    // current chatting status
    private HashMap<String, ChatStatus> chatStatusMap;

    @Autowired
    public DiscordOnMessageListener(RasaService rasa, IntentHandleService intentHandle, JDAMessageHandleService jdaMsgHandle, Environment env){
        this.adminChannelId = env.getProperty("discord.admin.channel.id");
        this.adminSuggestChannelId = env.getProperty("discord.admin.channel.suggest");
        this.botId = env.getProperty("discord.application.id");
        this.adminRoleId = env.getProperty("discord.admin.role.id");
        this.jdaMsgHandleService = jdaMsgHandle;
        this.intentHandleService = intentHandle;
        this.rasa = rasa;
        this.chatStatusMap = new HashMap<>();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return; // ignore all message from bot

        // print received message
        System.out.println("[onMessage]: try to print received message.");
        System.out.println("> [content raw] " + event.getMessage().getContentRaw());
        System.out.println("> [content display] " + event.getMessage().getContentDisplay());
        System.out.println("> [content strip] " + event.getMessage().getContentStripped());
        System.out.println("> [embed] " + event.getMessage().getEmbeds());
        System.out.println("> [attachment] " + event.getMessage().getAttachments());
        if(event.getMessage().getAttachments().size() > 0){
            for(Message.Attachment attachment: event.getMessage().getAttachments()){
                System.out.println("  > [attach id] " + attachment.getId());
                System.out.println("  > [attach type] " + attachment.getContentType());
                System.out.println("  > [attach url] " + attachment.getUrl());
                System.out.println("  > [attach proxy url] " + attachment.getProxyUrl());
            }
        }
//        System.out.println("---");
//        System.out.println("[TEST] try to send received message out.");
//        MessageBuilder builder = new MessageBuilder();
//        builder.append(event.getMessage().getContentRaw());
//        builder.append(event.getMessage().getAttachments().get(0).getUrl());
//        jdaMsgHandleService.sendPublicMessageWithReference(builder.build(), event.getMessageId(), "test-channel", event);
        // check if admin role got mentioned, in this case, admin role is 'TA'

        if(event.isFromType(ChannelType.PRIVATE)){
            System.out.println("[DEBUG] private message received.");
            /* handle message from private channel: student personal message */
            // do nothing if no message content found (include slash command)
            if(event.getMessage().getContentRaw().length() > 0){
                if(event.getMessage().isMentioned(DiscordGeneralEventListener.guild.getMemberById(botId))){
                    // direct message to TA
                    jdaMsgHandleService.addAdminMentionedMessageList(event.getMessage(), event.getAuthor());
                }
                // todo: normal message handle (rasa), from private channel
                System.out.println(">>> trigger normal handle (private)");
                handleNormalMessage(event);
            }
        }else{
            System.out.println("[DEBUG] public message received.");
            // handle message from public channel: public server channel, admin channel
            if(isFromAdminCategory(event.getChannel())) {
                System.out.println("[DEBUG] triggered from admin channel.");
                handleAdminMessage(event);
                /* reply private message to private channel from manager channel */
//            jdaMsgHandleService.sendPrivateMessageOnReply(extractStudentIdFromMessageLog(event.getMessage()), event);
                /* button testing block */
//            MessageBuilder msgBuilder = new MessageBuilder();
//            msgBuilder.append("special test message");
//            msgBuilder.setActionRows(ActionRow.of(Button.primary("yes", "Yes"), Button.danger("no", "No")));
//            event.getTextChannel().sendMessage(msgBuilder.build()).queue();
            }else{
                /* general channel (public) */
                // only react to incoming message if bot got mentioned in general channels (student user available channel, to be specific)
                if(isBotMentioned(event)) {
                    // normal function
                    // todo: normal message handle (rasa), from public channel
                    System.out.println(">>> trigger normal handle (public)");
                    handleNormalMessage(event);
                }
                // direct message to TA
                if(event.getMessage().isMentioned(DiscordGeneralEventListener.guild.getRoleById(adminRoleId))){
                    // send same message to admin channel
                    jdaMsgHandleService.addAdminMentionedMessageList(event.getMessage(), event.getAuthor());
//                    jdaMsgHandleService.sendPublicMessageWithReference();
                }
            }
        }

    }

    /**
     * handle message from non-admin channel, include both public and private
     * send message to rasa and execute correspond actions
     * note: message from public channel might have bot-mention on start of message, remove before rasa analyze
     * @param event message received event
     */
    private void handleNormalMessage(MessageReceivedEvent event){
        Message received = event.getMessage();
        String rawMsg = received.getContentRaw();
        if(received.isFromGuild())
            rawMsg = received.getContentDisplay().strip().replace("@TABot", "").strip();
        System.out.println("[DEBUG][normal handle] " + rawMsg);
        // send message to rasa
        Intent intent = rasa.analyze(event.getMember().getId(), rawMsg);
        System.out.println(intent);
        // store current chatting status
        // todo: save/remove chat status
        // check what to do next
        // todo: complete intentHandle implement
        Message result = intentHandleService.checkIntent(intent);
        if(received.isFromGuild())
            jdaMsgHandleService.replyPublicMessage(result, received.getId(), received.getTextChannel().getName());
        else
            jdaMsgHandleService.replyPrivateMessage(result, received.getAuthor().getId(), received.getId());
    }

    /**
     * handle message from admin channel, include student-feedback, suggest-warehouse, etc
     * @param event message received event
     */
    private void handleAdminMessage(MessageReceivedEvent event){
        Message received = event.getMessage();
        if(fromSuggestList(event)){
            // only do stuff on reply (check suggestion)
            if(received.getReferencedMessage() != null){
                Message ref = received.getReferencedMessage();
                Intent intent = rasa.analyze(event.getMember().getId(), received.getContentRaw());
                System.out.println(intent);
                String intentName = intent.getCustom().getIntent();
                if(intentName.equals("suggest_review_pass")){
                    // suggest passed, add suggestion in pending add list
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle("Suggest passed");
                    builder.setColor(Color.orange);
                    builder.setDescription("audited by " + received.getAuthor().getName());
                    builder.addField("", ref.getContentRaw(), false);
                    builder.setTimestamp(Instant.now());
                    MessageBuilder msgBuilder = new MessageBuilder();
                    jdaMsgHandleService.sendSuggestPassMsg(builder.build());
//                    jdaMsgHandleService.sendSuggestPassMsg(ref);
                }
                if(intentName.equals("suggest_review_failed")){
                    // todo: handle suggest failed
                    // delete suggestion
//                    event.getGuild().getTextChannelById(adminSuggestChannelId).deleteMessageById(ref.getId()).queue();
                }
            }
        }
        if(fromAdminMainChannel(event)){
            if(received.getReferencedMessage() != null && isLegalMessageLog(received.getReferencedMessage().getContentRaw())){
                MessageBuilder reply = new MessageBuilder();
                reply.append(received.getContentRaw());
                if(received.getAttachments().size() > 0)
                    reply.append(received.getAttachments().get(0).getUrl());
                // reply message, check message channel type from message content
                if(isMessageLogFromGuild(received.getReferencedMessage().getContentRaw())){
                    // from guild channel
                    jdaMsgHandleService.replyPublicMessage(reply.build(), getMessageIdFromMessageLog(received.getReferencedMessage().getContentRaw()), getChannelNameFromMessageLog(received.getReferencedMessage().getContentRaw()));
                }else{
                    // from private channel
//                    event.getJDA().retrieveUserById(getUserIdFromMessageLog(event.getMessage().getReferencedMessage().getContentRaw())).queue(user -> {});
                    jdaMsgHandleService.replyPrivateMessage(reply.build(), getUserIdFromMessageLog(received.getReferencedMessage().getContentRaw()), getMessageIdFromMessageLog(received.getReferencedMessage().getContentRaw()));
                }
            }else{
                if(isBotMentioned(event)){
                    // todo: send message as bot by chatting, problem: send to where
                    // use slash command for now
                }
            }
        }
    }

    /**
     * check if message contains all component of admin message log
     * @param rawMessage
     * @return
     */
    private boolean isLegalMessageLog(String rawMessage){
        List<String> components = Arrays.asList("[Sender ID]", "[Ref]", "[Channel]", "[Message ID]", "[RawContent]", "[Attachment]");
        return components.stream().allMatch(rawMessage::contains);
    }

    /**
     * check if message sent from guild channel
     * @param rawMessage
     * @return
     */
    private boolean isMessageLogFromGuild(String rawMessage){
        String channel = Arrays.stream(rawMessage.split("\n")).filter(line -> line.strip().startsWith("[Channel] ")).findFirst().get().replace("[Channel] ", "").strip();
//        String messageId = Arrays.stream(rawMessage.split("\n")).filter(line -> line.strip().startsWith("[Message ID] ")).findFirst().get().replace("[Message ID] ", "").strip();
        return !channel.equals("private");
    }

    /**
     * extract sender id from message log
     * @param rawMessage
     * @return
     */
    private String getUserIdFromMessageLog(String rawMessage){
        String userID = Arrays.stream(rawMessage.split("\n")).filter(line -> line.strip().startsWith("[Sender ID] ")).findFirst().get().replace("[Sender ID] ", "").strip();
        return userID;
    }

    /**
     * extract message id from message log
     * @param rawMessage
     * @return
     */
    private String getMessageIdFromMessageLog(String rawMessage){
        String messageId = Arrays.stream(rawMessage.split("\n")).filter(line -> line.strip().startsWith("[Message ID] ")).findFirst().get().replace("[Message ID] ", "").strip();
        return messageId;
    }

    /**
     * extract source channel name from message log
     * @param rawMessage
     * @return
     */
    private String getChannelNameFromMessageLog(String rawMessage){
        String channelName = Arrays.stream(rawMessage.split("\n")).filter(line -> line.strip().startsWith("[Channel] ")).findFirst().get().replace("[Channel] ", "").strip();
        return channelName;
    }

    // todo: check if message is from suggest list channel
    private boolean fromSuggestList(MessageReceivedEvent evt){
        if(evt.getTextChannel().getId().equals(adminSuggestChannelId)){
            // this message comes from suggest list channel
            return true;
        }
        return false;
    }

    private boolean fromAdminMainChannel(MessageReceivedEvent evt){
        return evt.getTextChannel().getId().equals(adminChannelId);
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
    private boolean isFromAdminCategory(MessageChannel channel){
        if(DiscordGeneralEventListener.adminChannelMap.containsKey(channel.getName()))
            return true;
        return false;
//        return channel instanceof TextChannel && channel.getId().equals(adminChannelId);
    }

    /**
     * extract student id from chat log and return correspond discord id
     * expect chat log has format like this: '[student id][student name] bla bla bla'
     * note: need to search database if discord id is not available in replied message log
     * this method should ONLY be used when REPLYING message (this message MUST have a reference message)
     * @param messageObj replied message
     * @return student discord id
     */
    private String extractStudentIdFromMessageLog(Message messageObj){
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

    /**
     * check if previous chat exist
     * mainly used to check class map suggest
     * other types of chat with be expired by rasa if chatting flow be suspended for too long
     * @param userId user id
     * @return True if previous chatting exist, otherwise False
     */
    public boolean hasPreviousStatus(String userId){
        long currentTime = System.currentTimeMillis();
        ChatStatus status;
        // check if current user has
        if(!chatStatusMap.containsKey(userId))
            return false;
        status = chatStatusMap.get(userId);
        if(TimeUnit.MILLISECONDS.toMinutes(currentTime - status.getTimestamp()) >= 1){
            // expire previous chat status
            chatStatusMap.remove(status.getUserId());
            return false;
        }
        return true;
    }

    /**
     * send message to check if everything is fine, for example: send 'do this helps?'
     */
    public void handleEndOfChat(){
        // todo: send end of chat message
    }
}
