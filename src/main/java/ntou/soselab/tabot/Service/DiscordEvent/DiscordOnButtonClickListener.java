package ntou.soselab.tabot.Service.DiscordEvent;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.tabot.Service.IntentHandleService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * define what bot should do whenever somebody clicked button
 * implement personal question and stuff
 */
@Service
public class DiscordOnButtonClickListener extends ListenerAdapter {

    @Autowired
    public DiscordOnButtonClickListener(Environment env){
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        handleQuizButton(event);
    }

    private void handleQuizButton(ButtonClickEvent event){
        String componentId = event.getComponentId();
//        if (event.getComponentId().equals("yes")){
//            event.reply("Yes Button clicked.").queue();
//        } else if (event.getComponentId().equals("no")){
////            event.editMessage("no, Update stuff.").queue();
//            event.replyEmbeds(new EmbedBuilder().setDescription("You clicked no.").build()).queue();
////            event.editButton(event.getButton().asDisabled()).queue();
//            event.editButton(null).queue();
////            event.editButton(event);
////            event.editMessage(new MessageBuilder(event.getMessage()).setActionRows().build()).queue();
//        }
        /* personal quiz handle */
        // todo: maybe need to check student id from discord id
        // get full quiz data from previous event
        System.out.println("--- [DEBUG][onButton] triggered button id: " + componentId);
        String studentId = event.getUser().getId();
        /* --- test block: change id --- */
        System.out.println("##### change detected user id for testing purpose #####");
        if(studentId.equals("286145047169335298"))
            studentId = "0076D053";
        /* --- end of test block --- */
        System.out.println("--- [DEBUG][onButton] triggered student id: " + studentId);
        JsonObject quiz = IntentHandleService.ongoingQuizMap.get(studentId);
        System.out.println("--- [DEBUG][onButton] retrieve quiz: " + quiz);
        String ansOpt = quiz.get("ans").getAsString();
        String ansContent = quiz.get("opt" + ansOpt).getAsString();
        if(componentId.equals(ansOpt)){
            // correct
            event.reply("Correct !").queue();
        }else{
            // wrong
            event.reply("Wrong. Correct answer is `" + ansContent + "`").queue();
        }
//        switch(componentId){
//            case "A":
//                break;
//            case "B":
//                break;
//            case "C":
//                break;
//            case "D":
//                break;
//            default:
//                event.reply("Something goes wrong. please report this to TA, thanks.").queue();
//                System.out.println("[DEBUG][onButton click] unavailable option.");
//        }
        // remove quiz from ongoing quiz map
        IntentHandleService.ongoingQuizMap.remove(studentId);
    }
}
