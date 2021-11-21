package ntou.soselab.tabot.Service;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ntou.soselab.tabot.Entity.Rasa.Intent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * call Rasa endpoint api
 */
@Service
public class RasaService {

    private final String rasaChinese;
    private final String rasaEnglish;
    /* language detector */
    private final LanguageDetector languageDetector;

    @Autowired
    public RasaService(Environment env){
        this.rasaChinese = env.getProperty("env.setting.rasa.zh");
        this.rasaEnglish = env.getProperty("env.setting.rasa.en");
        // initialize language detector with english and chinese
        this.languageDetector = LanguageDetectorBuilder.fromLanguages(Language.ENGLISH, Language.CHINESE).build();
    }

    /**
     * send GET request to Rasa endpoint
     * should receive something like this:
     * [{"recipient_id":'sender_name',"text":"{'intent': 'intent_name', 'entity': 'entity_name', 'endOfChat': 'True/False'}"}]
     * note that response from rasa might not be legal json format, change removeBackSlash() to fix this issue
     * @param author chat sender's name
     * @param msg message content
     * @return user intent
     */
    public Intent analyze(String author, String msg){
        Gson gson = new Gson();
        RestTemplate template = new RestTemplate();
        String path;
        // detect language type
        String language = checkLanguage(msg);
        // switch pipeline with different language
        if(language.equals("zh"))
            path = rasaChinese + "/webhooks/rest/webhook";
        else
            path = rasaEnglish + "/webhooks/rest/webhook";

        // setup request parameter
        JsonObject content = new JsonObject();
        content.addProperty("sender", author);
        content.addProperty("message", msg);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> entity = new HttpEntity<>(content.toString(), headers);
        ResponseEntity<String> response = template.exchange(path, HttpMethod.POST, entity, String.class);
//        System.out.println(response);

        String raw = removeBackSlash(response.getBody());
        System.out.println("[Rasa analyze][raw]: " + raw);

        // parse response by gson
        Intent result = gson.fromJson(raw, Intent.class);
        System.out.println(result);
        return result;
    }

    /**
     * make received message from rasa a legal json string format
     * @param raw raw message
     * @return processed message
     */
    private String removeBackSlash(String raw){
        String[] token = raw.split("");
        StringBuilder result = new StringBuilder();
        for(String t: token){
            if(t.equals("\\")) continue;
            if(t.equals("'")){
                result.append("\"");
                continue;
            }
            result.append(t);
        }
        result.deleteCharAt(result.length()-1);
        result.deleteCharAt(0);

        String temp = result.toString();
        StringBuilder output = new StringBuilder("{");
        String[] second = temp.split("");
        for(int i=1; i<second.length; i++){
//            System.out.println("current=[" + second[i] + "], i=" + i + ", result=[" + result + "]");
            int open = StringUtils.countOccurrencesOf(temp.substring(0, i), "{");
            int close = StringUtils.countOccurrencesOf(temp.substring(0, i), "}");
            if(second[i].equals("\"")){
                if(open - close == 1){
                    if(second[i+1].equals("{") || second[i-1].equals("}")) {
                        continue;
                    }
                }
            }
            output.append(second[i]);
        }
        return output.toString();
    }

    /**
     * detect input message is in chinese or english
     * @param msg input message
     * @return 'zh' if chinese (default), 'en' if english
     */
    private String checkLanguage(String msg){
        Language detectedLang = languageDetector.detectLanguageOf(msg);
        System.out.println("[lang detect][conf]:" + languageDetector.computeLanguageConfidenceValues(msg));
        if(detectedLang == Language.CHINESE){
            System.out.println("[lang detect]: chinese.");
            return "zh";
        }
        if(detectedLang == Language.ENGLISH){
            System.out.println("[lang detect]: english.");
            return "en";
        }
        /* return chinese by default */
        return "zh";
    }
}
