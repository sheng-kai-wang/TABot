package ntou.soselab.tabot.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/commitment")
public class CommitmentRetrievalController {

    @GetMapping(value = "/status")
    public ResponseEntity<String> printStatus(@RequestParam String message){
        System.out.println("[DEBUG][CommitmentRetrievalController] " + message);
        return ResponseEntity.ok("ok");
    }
}
