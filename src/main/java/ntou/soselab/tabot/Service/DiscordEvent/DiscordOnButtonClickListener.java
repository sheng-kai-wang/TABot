package ntou.soselab.tabot.Service.DiscordEvent;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.tabot.Exception.NoAccountFoundError;
import ntou.soselab.tabot.Service.IntentHandleService;
import ntou.soselab.tabot.Service.UserService;
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

    private final UserService userService;

    @Autowired
    public DiscordOnButtonClickListener(UserService userService, Environment env){
        this.userService = userService;
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        try {
            handleQuizButton(event);
        } catch (NoAccountFoundError e) {
            System.out.println("[DEBUG][onButtonEvent] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleQuizButton(ButtonClickEvent event) throws NoAccountFoundError {
        event.deferReply().queue();
        System.out.println("[DEBUG][onButton] component: " + event.getComponent());
        String componentId = event.getComponentId();
        /* personal quiz handle */
        // get full quiz data from previous event
        System.out.println("--- [DEBUG][onButton] triggered button id: " + componentId);
        // get user's student id from discord id
        String studentId = userService.getStudentIdFromDiscordId(event.getUser().getId());
        System.out.println("--- [DEBUG][onButton] triggered student id: " + studentId);
        /* --- test block: change id --- */
        System.out.println("##### change detected user id for testing purpose #####");
//        if(studentId.equals("286145047169335298"))
        studentId = "00000000";
        /* --- end of test block --- */
        JsonObject quiz = IntentHandleService.ongoingQuizMap.get(studentId);
        System.out.println("--- [DEBUG][onButton] retrieve quiz: " + quiz);
        String ansOpt = quiz.get("ans").getAsString();
        String ansContent = quiz.get("opt" + ansOpt).getAsString();
        if(componentId.equals(ansOpt)){
            // correct
//            event.reply("Correct !").queue();
            event.getHook().sendMessage("Correct !").queue();
        }else{
            // wrong
//            event.reply("Wrong. Correct answer is `" + ansContent + "`").queue();
            event.getHook().sendMessage("Wrong. Correct answer is `" + ansContent + "`").queue();
        }
        // remove quiz from ongoing quiz map
        IntentHandleService.ongoingQuizMap.remove(studentId);
    }
}
