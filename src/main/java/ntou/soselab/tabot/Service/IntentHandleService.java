package ntou.soselab.tabot.Service;

import ntou.soselab.tabot.Entity.Rasa.Intent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class IntentHandleService {

    @Autowired
    public IntentHandleService(Environment env){
    }

    /**
     * declare what bot should do with each intent
     * @param intent incoming intent
     */
    public void checkIntent(Intent intent){
        // todo: check intent and do stuff
    }
}
