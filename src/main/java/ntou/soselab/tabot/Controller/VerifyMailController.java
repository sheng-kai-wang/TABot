package ntou.soselab.tabot.Controller;

import ntou.soselab.tabot.Entity.UserProfile;
import ntou.soselab.tabot.Service.DiscordEvent.DiscordGeneralEventListener;
import ntou.soselab.tabot.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/verify")
public class VerifyMailController {

    private final DiscordGeneralEventListener generalEventListener;

    public VerifyMailController(DiscordGeneralEventListener generalListener){
        this.generalEventListener = generalListener;
    }

    @GetMapping(value = "/test")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok("hello from mail verify endpoint.");
    }

    /**
     * verify user when user click verify link in verification mail
     * @param uuid temporary uuid of user profile
     * @return 200 if user update complete, 500 if anything goes wrong
     */
    @GetMapping(value = "/{uuid}")
    public ResponseEntity<String> verify(@PathVariable String uuid){

        // check if received string is correct uuid
        try{
            UUID testUUID = UUID.fromString(uuid);
        }catch (IllegalArgumentException e){
            System.out.println("[DEBUG][mail verify] illegal uuid format detected.");
            return ResponseEntity.status(203).body("Wrong id.");
        }

        String studentId = UserService.verifyList.get(uuid).getStudentId();
        System.out.println("[DEBUG][mail verify] confirmed link clicked from " + studentId);
        /* add user in to user list and assign role to user */
        try {
            generalEventListener.verifyUserAndAssignRole(uuid);
        } catch (Exception e) {
            System.out.println("[DEBUG][mail verify] error occurs while trying to assign role and update user list.");
            e.printStackTrace();
            return ResponseEntity.status(500).body("Something goes wrong, please report TA about this situation to manager.");
        }
        return ResponseEntity.ok("You can close this page now, thanks.");
    }
}
