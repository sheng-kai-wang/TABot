package ntou.soselab.tabot.Service.DiscordEvent;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import ntou.soselab.tabot.Entity.Student.StudentDiscordProfile;
import ntou.soselab.tabot.Service.UserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * define what bot should do on each event received
 */
@Service
public class DiscordGeneralEventListener extends ListenerAdapter {

    private final String testUserId = "286145047169335298";
    private final String serverId;
    private final String studentRoleId;
    private final String adminCategoryId;
    public static Guild guild;
    public static HashMap<String, MessageChannel> channelMap;
    public static HashMap<String, MessageChannel> adminChannelMap;

    private final UserService userService;

    @Autowired
    public DiscordGeneralEventListener(Environment env, UserService userService){
        this.serverId = env.getProperty("discord.server.id");
        this.studentRoleId = env.getProperty("discord.role.student");
        this.adminCategoryId = env.getProperty("discord.admin.category.id");
        this.userService = userService;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        // all jda entity loaded successfully
        System.out.println(">> onReady");
        /*
        * load channel setting from property and create map instance,
        * all existed setting should be TextChannel,
        * assume only one guild exist in this server/application for now
        */
        System.out.println("[JDA onReady]: try to initialize channel map.");
        channelMap = new HashMap<>();
        adminChannelMap = new HashMap<>();
        for(TextChannel channel: event.getJDA().getGuildById(serverId).getTextChannels()){
            channelMap.put(channel.getName(), channel);
        }
//        System.out.println(event.getJDA().getGuildById(serverId).getCategoryById(adminCategoryId).getTextChannels());
        for(TextChannel channel: event.getJDA().getGuildById(serverId).getCategoryById(adminCategoryId).getTextChannels()){
            adminChannelMap.put(channel.getName(), channel);
        }
        guild = event.getJDA().getGuildById(serverId);
        System.out.print("[admin channel map] ");
        System.out.println(adminChannelMap);
        System.out.print("[channel map] ");
        System.out.println(channelMap);
        System.out.println("[JDA onReady]: channel map init complete.");

        // create global slash command
        event.getJDA().upsertCommand("global_test", "global command test").queue();
        event.getJDA().upsertCommand("contact_ta", "direct contact ta")
                .addOption(OptionType.STRING, "msg", "message content", true)
                .queue();
        // create guild slash command
        event.getJDA().getGuildById(serverId).upsertCommand("guild_test", "testing guild command")
                .addOption(OptionType.STRING, "msg", "test option", true)
                .queue();
        event.getJDA().getGuildById(serverId).upsertCommand("send_public_as_bot", "send message as bot")
                .addOption(OptionType.CHANNEL, "channel", "target channel", true)
                .addOption(OptionType.STRING, "message", "message content", true)
                .queue();
        event.getJDA().getGuildById(serverId).upsertCommand("send_private_as_bot", "send private message as bot")
                .addOption(OptionType.USER, "user", "target user", true)
                .addOption(OptionType.STRING, "message", "message content", true)
                .queue();
        event.getJDA().getGuildById(serverId).upsertCommand("suggest_material", "suggest material")
                .addOption(OptionType.STRING, "section", "suggest section", true)
                .addOption(OptionType.STRING, "title", "suggest title", true)
                .addOption(OptionType.STRING, "content", "suggestion content", true)
                .addOption(OptionType.STRING, "note", "suggestion remark", true)
                .queue();
        /* print current slash command */
        event.getJDA().getGuildById(serverId).retrieveCommands().queue(commands -> {
            System.out.println("[DEBUG][onReady] available command: " + commands);
        });
        System.out.println("<< [DEBUG] all role: " + guild.getRoles());
        System.out.println("<< [DEBUG] bot role: " + guild.getBotRole());
        /* testing: send hyperlink in message */
//        channelMap.get("test").sendMessage(new MessageBuilder().setEmbeds(new EmbedBuilder().addField("link", ":smile: [test](https://google.com)", false).build()).build()).queue();
        // testing: send message with action row
//        channelMap.get("test").sendMessage(new MessageBuilder().append("testing content").setActionRows(ActionRow.of(Button.primary("optB", "b"), Button.success("optA", "a"))).build()).queue();
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
        String currentNickname = event.getNewNickname();

        // check user's current role, return if already registered
        if(event.getMember().getRoles().size() > 0){
            System.out.println("[DEBUG][nickname update]" + event.getNewNickname() + " already has role. do nothing.");
            return;
        }

        // check new nickname, if nickname fits specific format, assign role to user and store their id and name in database
        if(UserService.verifyNickNameFormat(currentNickname)) {
            String userName = UserService.getNameByNickName(currentNickname);
            String userStudentId = UserService.getStudentIdByNickName(currentNickname);
            // create profile for current user
            StudentDiscordProfile studentDiscordProfile = new StudentDiscordProfile(userName, userStudentId, event.getUser().getId());

            // check if user is trying to change application content
            if(UserService.verifyList.entrySet().stream().anyMatch(user -> user.getValue().getDiscordId().equals(event.getUser().getId()))){
                System.out.println("[DEBUG][nickname update] remove previous application for " + event.getNewNickname());
                UserService.verifyList.values().removeIf(profile -> profile.getDiscordId().equals(event.getUser().getId()));
            }

            /* send verify mail, store correspond uuid and userProfile */
            String uuid = userService.sendVerificationMail(userStudentId);
            UserService.verifyList.put(uuid, studentDiscordProfile);
//            /* assign role to user */
//            System.out.println("[GuildMemberUpdateNicknameEvent]: try to assign role.");
//            event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(studentRoleId)).queue();
//            System.out.println("[GuildMemberUpdateNicknameEvent]: role assigned, try to store user info in database.");
//            storeUserIdentity(event.getUser(), event.getNewNickname());
        }else{
            System.out.println("[GuildMemberUpdateNicknameEvent] Nickname update detected with wrong format, do nothing.");
        }
    }

    /**
     * verify user and assign role to user
     * @param uuid target user's uuid
     */
    public void verifyUserAndAssignRole(String uuid) throws Exception {
        // todo: try to assign role to user
        /* get profile from verify list and remove it */
        StudentDiscordProfile profile = UserService.verifyList.get(uuid);
        UserService.verifyList.remove(uuid);
        System.out.println("[DEBUG][UserService] try to assign role to " + profile.getStudentId() + ".");
        /* get user from userProfile and try to assign role to user */
        // retrieve user from jda
        guild.retrieveMemberById(profile.getDiscordId()).queue(member -> {
            // try to assign role to this member
            guild.addRoleToMember(member, guild.getRoleById(studentRoleId)).queue();
        });
        /* add user into current user list */
        userService.registerStudent(profile);
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
        // maybe do something here
    }

    @Override
    public void onGuildMemberUpdate(@NotNull GuildMemberUpdateEvent event) {
//        super.onGuildMemberUpdate(event);
    }
}
