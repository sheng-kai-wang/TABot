package ntou.soselab.tabot.Controller;

import ntou.soselab.tabot.Service.DiscordEvent.DiscordGeneralEventListener;
import ntou.soselab.tabot.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * @param uuid
     * @return
     */
    @GetMapping(value = "/{uuid}")
    public ResponseEntity<String> verify(@PathVariable String uuid){
        System.out.println("[DEBUG][mail verify] received from " + uuid);
        /* add user in to user list and assign role to user */
        generalEventListener.verifyUserAndAssignRole(uuid);
        return ResponseEntity.ok("You can close this page now, thanks.");
    }
}
