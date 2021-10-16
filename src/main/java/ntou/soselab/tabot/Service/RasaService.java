package ntou.soselab.tabot.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * call Rasa endpoint api
 */
@Service
public class RasaService {

    @Autowired
    public RasaService(Environment env){
    }
}
