package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.tabot.Entity.ChatStatus;
import ntou.soselab.tabot.Entity.Rasa.Intent;
import ntou.soselab.tabot.Exception.NoAccountFoundError;
import ntou.soselab.tabot.Service.IntentHandleService;
import ntou.soselab.tabot.Service.JDAMessageHandleService;
import ntou.soselab.tabot.Service.RasaService;
import ntou.soselab.tabot.Service.UserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;


import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * define what bot should do when message received from discord chatroom, include both public and private message
 * let this event have personal java class since there might be a lot of thing to do here, put it back to DiscordGeneralEventListener.java if necessary
 */
@Service
public class DiscordOnMessageListener extends ListenerAdapter {

    private final String adminChannelId;
    private final String adminCategoryId;
    private final String adminSuggestChannelId;
    private final String botId;
    private final String adminRoleId;
    private final String miscLogPath;
    private JDAMessageHandleService jdaMsgHandleService;
    private IntentHandleService intentHandleService;
    private UserService userService;
    // rasa service
    private final RasaService rasa;

    // current chatting status
    private HashMap<String, ChatStatus> chatStatusMap;

    @Autowired
    public DiscordOnMessageListener(RasaService rasa, IntentHandleService intentHandle, JDAMessageHandleService jdaMsgHandle, UserService userService, Environment env){
        this.adminCategoryId = env.getProperty("discord.admin.category.id");
        this.adminChannelId = env.getProperty("discord.admin.channel.id");
        this.adminSuggestChannelId = env.getProperty("discord.admin.channel.suggest");
        this.botId = env.getProperty("discord.application.id");
        this.adminRoleId = env.getProperty("discord.admin.role.id");
        this.miscLogPath = env.getProperty("discord.log.path");
        this.jdaMsgHandleService = jdaMsgHandle;
        this.intentHandleService = intentHandle;
        this.rasa = rasa;
        this.userService = userService;
        this.chatStatusMap = new HashMap<>();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return; // ignore all message from bot

        // print received message
        System.out.println(" ================ ");
        System.out.println("[onMessage]: try to print received message.");
        System.out.println("> [author] " + event.getAuthor().getId());
        if(event.isFromGuild()) System.out.println("> [role] " + event.getMember().getRoles());
        else System.out.println("> [role] from private channel, no role found");
        System.out.println("> [message id] " + event.getMessage().getId());
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
        /* check if user account has assigned role */
        if(event.isFromGuild()){
            if(!hasRole(event.getMember())){
                // send register notification and return
                jdaMsgHandleService.sendPrivateMessage(event.getAuthor(), generateRegisterNotifyMsg());
                return;
            }
        }else{
            if(!hasRole(event.getAuthor().getId())){
                // send register notification and return
                jdaMsgHandleService.sendPrivateMessage(event.getAuthor(), generateRegisterNotifyMsg());
                return;
            }
        }

        if(event.isFromType(ChannelType.PRIVATE)){
            System.out.println("[DEBUG] private message received.");
            /* handle message from private channel: student personal message */
            // todo: log this message anyway
            jdaMsgHandleService.addMessageLog(event.getMessage(), event.getAuthor());
            recordBotLog(miscLogPath, event);
            // do nothing if no message content found (include slash command)
            if(event.getMessage().getContentRaw().length() > 0){
                // check if TA got mentioned
                // if true, send direct message to TA
                // if false, treat as normal message (rasa analyze)
                if(isPrivateMessageMentionedAdmin(event.getMessage().getContentDisplay())) {
                    jdaMsgHandleService.addAdminMentionedMessageList(event.getMessage(), event.getAuthor());
                }else {
//                if(event.getMessage().isMentioned(DiscordGeneralEventListener.guild.getMemberById(botId))){
//                    // direct message to TA
//                    jdaMsgHandleService.addAdminMentionedMessageList(event.getMessage(), event.getAuthor());
//                }

                    // todo: normal message handle (rasa), from private channel
                    System.out.println(">>> trigger normal handle (private)");
                    handleNormalMessage(event);
                }
            }
        }else{
            System.out.println("[DEBUG] public message received.");
            // handle message from public channel: public server channel, admin channel
            if(isFromAdminCategory(event.getChannel())) {
                System.out.println("[DEBUG] triggered from admin channel.");
                handleAdminMessage(event);
                /* reply private message to private channel from manager channel */
//            jdaMsgHandleService.sendPrivateMessageOnReply(extractStudentIdFromMessageLog(event.getMessage()), event);
            }else{
                /* general channel (public) */
                System.out.println("[DEBUG] received from general channel.");
                // only react to incoming message if bot got mentioned in general channels (student user available channel, to be specific)
                if(isBotMentioned(event)) {
                    // normal function
                    System.out.println(">>> trigger normal handle (public)");
                    // todo: log message
                    jdaMsgHandleService.addMessageLog(event.getMessage(), event.getAuthor());
                    recordBotLog(miscLogPath, event);
                    handleNormalMessage(event);
                }
                // direct message to TA, example: '@TA bla bla bla'
                if(event.getMessage().isMentioned(DiscordGeneralEventListener.guild.getRoleById(adminRoleId))){
                    // send same message to admin channel
                    // todo: log message
                    jdaMsgHandleService.addMessageLog(event.getMessage(), event.getAuthor());
                    recordBotLog(miscLogPath, event);
                    jdaMsgHandleService.addAdminMentionedMessageList(event.getMessage(), event.getAuthor());
//                    jdaMsgHandleService.sendPublicMessageWithReference();
                }
            }
            System.out.println("<< [DEBUG][onMessage] end of current onMessage event.");
        }

    }

    /**
     * export log
     * @param logPath output log file path
     * @param event message received event
     */
    public void recordBotLog(String logPath, MessageReceivedEvent event){
        Logger logger = Logger.getLogger("botLogger");
        FileHandler fileHandler;
        try{
            Date date = new Date();
            SimpleFormatter simpleFormatter = new SimpleFormatter();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            String filePath = logPath + "/" + dateFormat.format(date) + ".log";
            fileHandler = new FileHandler(filePath, true);
            logger.addHandler(fileHandler);
            fileHandler.setFormatter(simpleFormatter);

            // log received message
            Message originalMsg = event.getMessage();
            String rawMsg = originalMsg.getContentDisplay().replace("@TA", "").replace("```", "").strip();
            if(!originalMsg.isFromGuild() && rawMsg.strip().startsWith("Bot"))
                rawMsg = rawMsg.replaceFirst("Bot", "").strip();
            logger.info("[Sender discord ID] " + event.getAuthor().getId());
            logger.info("[Sender Identity] " + userService.getFullNameFromDiscordId(event.getAuthor().getId()));
            logger.info("[Message Reference] " + originalMsg.getJumpUrl());
            if(originalMsg.isFromType(ChannelType.PRIVATE))
                logger.info("[Channel] private");
            else
                logger.info("[Channel] " + originalMsg.getTextChannel().getName());
            logger.info("[Message Id] " + originalMsg.getId());
            logger.info("[RawContent] " + rawMsg);
            if(originalMsg.getAttachments().size() > 0)
                logger.info("[Attachment] " + originalMsg.getAttachments().get(0).getUrl());
            else
                logger.info("[Attachment] none");
        }catch (SecurityException | IOException se){
            se.printStackTrace();
        }catch (NoAccountFoundError ae){
            System.out.println("[WARNING] error retrieving user id when logging message.");
            logger.log(Level.WARNING, "failed to retrieve user name by discord id");
            ae.printStackTrace();
        }
    }

    /**
     * handle message from non-admin channel, include both public and private
     * send message to rasa and execute correspond actions
     * note: message from public channel might have bot-mention on start of message, remove before rasa analyze
     * @param event message received event
     */
    private void handleNormalMessage(MessageReceivedEvent event){
        System.out.println("[DEBUG][onMsg][handle normal] triggered.");
        try{
            Message received = event.getMessage();
            String rawMsg = received.getContentRaw();
            /* set received msg id as author's discord id in case message came from private channel */
            String receivedMsgId = received.getId();
            String senderDiscordId = event.getAuthor().getId();
            String senderStudentId = userService.getStudentIdFromDiscordId(senderDiscordId);
            if(received.isFromGuild()) {
                rawMsg = received.getContentDisplay().strip().replace("@TABot", "").strip();
//                receivedMsgId = received.getId();
            }
//            /* ----- testing block: change id into testing id ----- */
//            String testDiscordId = "286145047169335298";
//            String testStudentId = "0076D053";
//            senderStudentId = testStudentId;
//            /* ----- end of testing block ----- */
            System.out.println("[DEBUG][normal handle] " + rawMsg);
            // send message to rasa
            Intent intent = rasa.analyze(senderDiscordId, rawMsg);
            System.out.println(intent);
            // store current chatting status
            // todo: save/remove chat status
            // check sender id
            // todo: add function to get student id by discord id
            // get intent response message
            Message result = intentHandleService.checkIntent(senderStudentId, intent);
            // reply message
            if(received.isFromGuild()) {
                System.out.println("+++ [DEBUG][handle normal] sender : " + userService.getFullNameFromDiscordId(senderDiscordId));
                System.out.println("+++ [DEBUG][handle normal] dc id : " + senderDiscordId);
                System.out.println("+++ [DEBUG][handle normal] channel: " + received.getTextChannel().getName());
                jdaMsgHandleService.replyPublicMessage(result, receivedMsgId, received.getTextChannel().getName());
            } else {
                jdaMsgHandleService.replyPrivateMessage(result, senderDiscordId, receivedMsgId);
//            jdaMsgHandleService.replyPrivateMessage(result, testDiscordId, received.getId()); /* test */
            }
        }catch (NoAccountFoundError e){
            System.out.println("[DEBUG][handleNormalMsg] " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * handle message from admin channel, include student-feedback, suggest-warehouse, etc
     * @param event message received event
     */
    private void handleAdminMessage(MessageReceivedEvent event){
        System.out.println("[DEBUG][onMsg][handle admin] triggered.");
        Message received = event.getMessage();

        /* handle suggest */
        if(fromSuggestList(event)){
            // only do stuff on reply (check suggestion)
            if(received.getReferencedMessage() != null){
                Message ref = received.getReferencedMessage();
                Intent intent = rasa.analyze(event.getMember().getId(), received.getContentRaw());
                System.out.println("[DEBUG][handle Admin][detected intent] " + intent);
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
                }
            }
        }

        /* replying to message log */
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
//            if(isBotMentioned(event)){
//                // send message as bot by chatting, problem: send to where
//                // use slash command for now
//            }
            /* [send message as bot] check if channel or user mentioned */
            if(received.getContentRaw().strip().startsWith("<#")) {
                System.out.println("[DEBUG][admin] try to send public message.");
                // try to send direct message
                MessageBuilder builder = new MessageBuilder();
                String content = received.getContentRaw().replaceAll("<#[0-9]{18}>", "").strip();
                String originalMessageId = extractMentionedMessageId(content);
                System.out.println("[DEBUG][admin] msg id: " + originalMessageId);
                if(originalMessageId.isEmpty()) {
                    builder.append(content);
                    if(received.getAttachments().size() > 0) {
                        for(Message.Attachment attachment: received.getAttachments())
                            builder.append("\n").append(attachment.getUrl());
                    }
                    jdaMsgHandleService.sendMessage(received.getMentionedChannels(), builder.build());
                }else{
                    builder.append(content.replaceAll("~[0-9]{18} ", "").strip());
                    if(received.getAttachments().size() > 0) {
                        for(Message.Attachment attachment: received.getAttachments())
                            builder.append("\n").append(attachment.getUrl());
                    }
                    jdaMsgHandleService.replyPublicMessage(builder.build(), originalMessageId, received.getMentionedChannels());
                }
            }
            if(received.getContentDisplay().strip().startsWith("@")){
                System.out.println("[DEBUG][admin] try to send private message.");
                String id = extractMentionedStudentId(received.getContentRaw().strip());
                System.out.println("[DEBUG][admin] catch id: " + id);
                if(!id.isEmpty()){
                    MessageBuilder builder = new MessageBuilder();
                    builder.append(received.getContentRaw().replaceAll("<[@#&]{0,2}[0-9a-zA-Z]{8}>", "").strip());
                    if(received.getAttachments().size() > 0) {
                        for(Message.Attachment attachment: received.getAttachments())
                            builder.append("\n").append(attachment.getUrl());
                    }
                    jdaMsgHandleService.sendPrivateMessage(id, builder.build());
                }
            }
        }
//        if(fromAdminMainChannel(event)){
//        }
    }

    /**
     * extract discord message id from start of message, expect snowflake format id
     * @param message raw message
     * @return discord message id, empty string if nothing found
     */
    private String extractMentionedMessageId(String message){
        Pattern idPattern = Pattern.compile("^~([0-9]{18}) .*$");
        Matcher matcher = idPattern.matcher(message);
        if(matcher.find())
            return matcher.group(1);
        return "";
    }

    /**
     * extract student id from message header
     * @param message raw message
     * @return student id, empty string if nothing found
     */
    private String extractMentionedStudentId(String message){
        System.out.println("[DEBUG][id extract] raw: " + message);
        Pattern idPattern = Pattern.compile("^<[@#&!]{0,3}([0-9a-zA-Z]{8})> .*$");
        Matcher matcher = idPattern.matcher(message);
        if(matcher.find())
            return matcher.group(1);
        return "";
    }

    /**
     * check if message sender has assigned role, assume message is send from public channel
     * @param sender message sender
     * @return true if sender has valid role, otherwise false
     */
    private boolean hasRole(Member sender){
        List<Role> availableRoles = sender.getRoles();
        return availableRoles.size() >= 1;
    }

    /**
     * check if message sender has assigned role, assume message is send from private channel
     * @param discordId sender's discord id
     * @return true if sender has valid role, otherwise false
     */
    private boolean hasRole(String discordId){
        // check current user list
        return UserService.currentUserList.stream().anyMatch(profile -> profile.getDiscordId().equals(discordId));
    }

    /**
     * generate a register notification message
     * @return register notification message
     */
    private Message generateRegisterNotifyMsg(){
        MessageBuilder builder = new MessageBuilder();
        builder.append("You need to verify your identity first.\n");
        builder.append("Change your nickname to `<your student id>-<your name>` to get a student role.");
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.addField("Example", "00000000-Bat Man", false);
        embedBuilder.setColor(Color.orange);
        builder.setEmbeds(embedBuilder.build());
        return builder.build();
    }

    /**
     * check if private message mentioned TA by adding prefix '@TA'
     * @param message
     * @return
     */
    private boolean isPrivateMessageMentionedAdmin(String message){
        return message.strip().startsWith("@TA");
    }

    /**
     * check if message contains all component of admin message log
     * @param rawMessage
     * @return
     */
    private boolean isLegalMessageLog(String rawMessage){
        List<String> components = Arrays.asList("[Sender ID]", "[Ref]", "[Channel]", "[Message ID]", "[RawContent]", "[Attachment]");
        List<String> miscLogComponents = Arrays.asList("[Sender ID]", "[Ref]", "[Channel]", "[Message ID]", "[RawContent Display]", "[Attachment]");
        return components.stream().allMatch(rawMessage::contains) || miscLogComponents.stream().allMatch(rawMessage::contains);
    }

    /**
     * check if message sent from guild channel
     * @param rawMessage
     * @return
     */
    private boolean isMessageLogFromGuild(String rawMessage){
        String channel = Arrays.stream(rawMessage.split("\n")).filter(line -> line.strip().startsWith("[Channel] ")).findFirst().get().replace("[Channel] ", "").strip();
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

    private boolean fromAdminMainChannel(MessageReceivedEvent event){
//        System.out.println("[DEBUG] parent id: " + event.getTextChannel().getParent().getId());
        return event.getTextChannel().getParent().getId().equals(adminCategoryId);
//        return event.getTextChannel().getId().equals(adminChannelId);
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
