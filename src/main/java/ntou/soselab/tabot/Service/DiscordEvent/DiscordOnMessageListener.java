package ntou.soselab.tabot.Service.DiscordEvent;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
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

    /* language detector */
    private final LanguageDetector languageDetector;

    @Autowired
    public DiscordOnMessageListener(RasaService rasa, IntentHandleService intentHandle, JDAMessageHandleService jdaMsgHandle, Environment env){
        this.adminChannelId = env.getProperty("discord.admin.channel.id");
        this.botId = env.getProperty("discord.application.id");
        this.jdaMsgHandleService = jdaMsgHandle;

        // initialize language detector with english and chinese
        this.languageDetector = LanguageDetectorBuilder.fromLanguages(Language.ENGLISH, Language.CHINESE).build();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return; // ignore all message from bot

        // print received message
        System.out.println("[onMessage]: try to print received message.");
        System.out.println("> [content raw] " + event.getMessage().getContentRaw());
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
        System.out.println("---");
        System.out.println("[TEST] try to send received message out.");
        MessageBuilder builder = new MessageBuilder();
        builder.append(event.getMessage().getContentRaw());
        builder.append(event.getMessage().getAttachments().get(0).getUrl());
        jdaMsgHandleService.sendPublicMessageWithReference(builder.build(), event.getMessageId(), "test-channel", event);

        // check if message come from manager channel (used to send message as bot by administrator)
        if(isFromAdmin(event.getChannel())) {
            // todo: add prefix to check if admin want to send message by adding id himself ?
            // message come from admin channel, try to send direct message
            // todo: maybe check private or not ?, use private as default for now
            // todo: reply message
            /* reply private message to private channel from manager channel */
//            jdaMsgHandleService.sendPrivateMessageOnReply(extractStudentIdFromMessageLog(event.getMessage()), event);
            /* button testing block */
//            MessageBuilder msgBuilder = new MessageBuilder();
//            msgBuilder.append("special test message");
//            msgBuilder.setActionRows(ActionRow.of(Button.primary("yes", "Yes"), Button.danger("no", "No")));
//            event.getTextChannel().sendMessage(msgBuilder.build()).queue();
        }

        // only react to incoming message if bot got mentioned in general channels (student user available channel, to be specific)
        if(isBotMentioned(event)) {
            if (event.isFromType(ChannelType.PRIVATE)) {
                // todo: handle private message
            } else {
                // todo: handle public message
            }
        }

        if(event.isFromType(ChannelType.PRIVATE)){
            /* this message is send from private channel */
        }else{
            /* this message is send from general channel, in our case, this should only be text-channel */
        }
    }

    /**
     * detect input message is in chinese or english
     * @param msg input message
     * @return 'zh' if chinese (default), 'en' if english
     */
    private String checkLanguage(String msg){
        Language detectedLang = languageDetector.detectLanguageOf(msg);
        System.out.println("[onMessage][lang detect][conf]:" + languageDetector.computeLanguageConfidenceValues(msg));
        if(detectedLang == Language.CHINESE){
            System.out.println("[onMessage][lang detect]: chinese.");
            return "zh";
        }
        if(detectedLang == Language.ENGLISH){
            System.out.println("[onMessage][lang detect]: english.");
            return "en";
        }
        /* return chinese by default */
        return "zh";
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
}
