package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.member.update.GenericGuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * define what bot should do on each event received
 */
@Service
public class DiscordGeneralEventListener extends ListenerAdapter {

    private final String serverId;
    private final String studentRoleId;

    @Autowired
    public DiscordGeneralEventListener(Environment env){
        this.serverId = env.getProperty("discord.server.id");
        this.studentRoleId = env.getProperty("discord.role.student");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        // all jda entity loaded successfully
        System.out.println(">> onReady");
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        // jda will start dispatching events related to this guild, initialize service depend on this guild
        System.out.println(">> onGuildReady");
    }

    /**
     * listen to GuildMemberUpdateNickname event, basically, do stuff when any guild member updates his/her nickname
     * @param event GuildMemberUpdateNicknameEvent
     */
    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        System.out.println("[GuildMemberUpdateNicknameEvent][User]: " + event.getUser());
        System.out.println("[GuildMemberUpdateNicknameEvent][oldName]: " + event.getOldNickname());
        System.out.println("[GuildMemberUpdateNicknameEvent][newName]: " + event.getNewNickname());

        // check new nickname, if nickname fits specific format, assign role to user and store their id and name in database
        if(checkNickname(event.getNewNickname())) {
            System.out.println("[GuildMemberUpdateNicknameEvent]: try to assign role.");
            event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(studentRoleId)).queue();
            System.out.println("[GuildMemberUpdateNicknameEvent]: role assigned, try to store user info in database.");
            storeUserIdentity(event.getUser(), event.getNewNickname());
        }else{
            System.out.println("[GuildMemberUpdateNicknameEvent] Nickname update detected with wrong format, do nothing.");
        }
    }

    /**
     * check if nickname matches specific pattern
     * @param name nickname
     * @return True if matches, False if not
     */
    private boolean checkNickname(String name){
        String format = "[0-9]{8}-.*";
        return Pattern.matches(format, name);
    }

    /**
     * try to store user's information in database, include discord id and name(student id)
     * @param user target user
     * @param nickname user nickname
     */
    private void storeUserIdentity(User user, String nickname){
        String name = user.getName();
        String discordId = user.getId();
        String studentId = "00000000";
        Pattern namePattern = Pattern.compile("^([0-9]{8})-.*$");
        Matcher matcher = namePattern.matcher(nickname);
        if(matcher.find())
            studentId = matcher.group(1);
        System.out.println("[storeUserIdentity] student id '" + studentId + "' extracted.");
        // todo: do stuff in database
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        // todo: store all user id when new user join server
//        System.out.println("[GuildMemberJoinEvent][member id]: " + event.getMember().getId());
        System.out.println("[GuildMemberJoinEvent][User]: " + event.getUser()); // represent discord user
//        System.out.println("[GuildMemberJoinEvent][user name]: " + event.getUser().getName());
//        System.out.println("[GuildMemberJoinEvent][user id]: " + event.getUser().getId());
        User user = event.getUser();
        String username = user.getName();
        String id = user.getId();
        // maybe to something here
    }

    @Override
    public void onGuildMemberUpdate(@NotNull GuildMemberUpdateEvent event) {
//        super.onGuildMemberUpdate(event);
    }
}
