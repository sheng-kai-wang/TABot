package ntou.soselab.tabot.Service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ntou.soselab.tabot.Entity.Rasa.Intent;
import ntou.soselab.tabot.repository.SheetsHandler;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class IntentHandleService {

    @Autowired
    public IntentHandleService(Environment env){
    }

    /**
     * declare what bot should do with each intent
     * @param intent incoming intent
     */
    public Message checkIntent(Intent intent){
        String intentName = intent.getCustom().getIntent();
        // todo: check intent and do stuff
        switch (intentName){
            case "greet":
                break;
            case "classmap_search":
                // todo: normal asking, query class map
                break;
            case "classmap_ppt":
                // todo: query class map
                break;
            case "classmap_suggest":
                // todo: suggest function
                break;
            default:
                if(intentName.startsWith("confirm"))
                    return confirmHandler(intent);
                else if(intentName.startsWith("faq"))
                    return faqHandle(intent);
                else if(intentName.startsWith("personal"))
                    return personalFuncHandler(intent);
                else
                    System.out.printf("[DEBUG][intent analyze]: '%s' detected, no correspond result found.", intentName);
                break;
        }
        return null;
    }

    public Message faqHandle(Intent faqIntent){
        // todo: handle faq-related intent
        String faqName = faqIntent.getCustom().getIntent().replace("faq/", "");
        // call google sheet api to query correspond result of faq
        JSONObject searchResult = new SheetsHandler("Java").readContentByKey("FAQ", faqName);
        String response = searchResult.getJSONArray("answer").get(0).toString();
        return new MessageBuilder().append(response).build();
    }

    public Message confirmHandler(Intent confirmIntent){
        // todo: handle intent response with confirm
        /*
        classmap_search, personal_score_query, classmap_ppt_query
         */
        String response = confirmIntent.getCustom().getResponseMessage();
        return new MessageBuilder().append(response).build();
    }

    public Message personalFuncHandler(Intent personalIntent){
        // todo: handle personal function
        String intentName = personalIntent.getCustom().getIntent();
        String scoreQueryTarget;
        switch(intentName){
            case "personal_score_query":
                scoreQueryTarget = personalIntent.getCustom().getEntity();
                if(scoreQueryTarget.equals("None")){
                    // error with entity extraction
                    return sendErrorMessage(personalIntent);
                }

                break;
            case "personal_textbook_query":
                break;
            case "personal_quiz_query":
                break;
        }
        return null;
    }

    public Message getPersonalQuiz(){
        // todo: get personal quiz, create button for quiz, Problem: id generate
        return null;
    }

    /**
     * throw an error message back if
     * @param intent
     * @return
     */
    private Message sendErrorMessage(Intent intent){
        String user = intent.getRecipient_id();
        MessageBuilder msgBuilder = new MessageBuilder();
        msgBuilder.mentionUsers(user);
        msgBuilder.append("Sorry, i'm not sure what do you mean, can you please say it again in another way ? :pray:");
//        EmbedBuilder builder  = new EmbedBuilder();
//        builder.setDescription("<@" + user + "> Sorry, i'm not sure what do you mean, can you please say it again in another way ? :(");
//        builder.setColor(Color.orange);
//        return builder.build();
        return msgBuilder.build();
    }

    private Message suggestPassed(){
        return null;
    }
    private Message suggestFailed(){
        return null;
    }


}
