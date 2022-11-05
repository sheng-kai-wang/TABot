package ntou.soselab.tabot.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/commitment")
public class CommitmentRetrievalController {

    @GetMapping(value = "/status/{message}")
    public ResponseEntity<String> printStatus(@PathVariable String message){
        System.out.println("[DEBUG][CommitmentRetrievalController] " + message);
        return ResponseEntity.ok("ok");
    }
}
