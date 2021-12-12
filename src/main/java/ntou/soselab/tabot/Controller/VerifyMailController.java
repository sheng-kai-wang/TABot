package ntou.soselab.tabot.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verify")
public class VerifyMailController {

    @GetMapping(value = "/{id}")
    public ResponseEntity<String> verify(@PathVariable String id){
        System.out.println("[DEBUG][mail verify] received from " + id);
        /* add user in to user list */
        // todo: add user in user list
        return ResponseEntity.ok("You can close this page now, thanks.");
    }
}
