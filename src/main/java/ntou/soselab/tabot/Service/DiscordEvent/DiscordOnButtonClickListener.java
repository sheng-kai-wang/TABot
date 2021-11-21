package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
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
        testButton(event);
    }

    private void testButton(ButtonClickEvent event){
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
    }
}
