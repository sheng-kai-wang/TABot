package ntou.soselab.tabot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ntou.soselab.tabot.repository.RedisHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;

@Service
@EnableScheduling
public class CommitmentRetrievalService {
    private final RedisHandler redisHandler;
    private final String commitMsgSearcherUrl;
    private final String commitMsgSearcherRegisterUrl;
    private final String commitMsgSearcherRetrievalUrl;
    public final static String NO_RESULT = "all cosine is 0";

    @Autowired
    public CommitmentRetrievalService(Environment env, RedisHandler redisHandler) {
        this.redisHandler = redisHandler;
        this.commitMsgSearcherUrl = env.getProperty("github-commit-message-searcher.url");
        this.commitMsgSearcherRegisterUrl = commitMsgSearcherUrl + env.getProperty("github-commit-message-searcher.register.path");
        this.commitMsgSearcherRetrievalUrl = commitMsgSearcherUrl + env.getProperty("github-commit-message-searcher.retrieval.path");
        registerRepository();
    }

    /**
     * execute every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void registerRepository() {
        System.out.println("[DEBUG] start to register all repos");

        // setup request payload
        JsonObject payload = new JsonObject();
        JsonArray allReposData = new JsonArray();
        JsonObject allRepos = redisHandler.getAllRepository();
        for (String groupName : allRepos.keySet()) {
            JsonObject groupData = new JsonObject();
            groupData.addProperty("projectName", groupName);
            groupData.add("repoNames", allRepos.get(groupName));
            allReposData.add(groupData);
        }

        if (allReposData.isEmpty()) {
            System.out.println("--- [DEBUG] no repos yet");
            return;
        }
        payload.add("allProjects", allReposData);

        RestTemplate template = new RestTemplate(getRequestTimeoutConfig(1000_000));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
        ResponseEntity<String> response = template.exchange(commitMsgSearcherRegisterUrl, HttpMethod.POST, entity, String.class);
        JsonObject responseMsg = new Gson().fromJson(response.getBody(), JsonObject.class);
        System.out.println("[DEBUG] " + responseMsg.get("status").getAsString());
    }

    public JsonArray retrieveCommitMsg(String projectName, String keywords, JsonArray range, int quantity) {
        RestTemplate template = new RestTemplate();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(commitMsgSearcherRetrievalUrl)
                .queryParam("projectName", projectName)
                .queryParam("keywords", keywords)
                .queryParam("range", range)
                .queryParam("quantity", quantity);
        URI uri;
        try {
            uri = new URI(uriBuilder.toUriString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        ResponseEntity<String> response = template.getForEntity(uri, String.class);
        JsonObject responseMsg = new Gson().fromJson(response.getBody(), JsonObject.class);
        System.out.println("[DEBUG] " + responseMsg.get("status").getAsString());
        if (responseMsg.get("rank").isJsonNull()) return null;
        if (responseMsg.get("rank").toString().equals(NO_RESULT)) return new JsonArray();
        return responseMsg.get("rank").getAsJsonArray();
    }

    /**
     * Override timeouts in request factory
     *
     * @param time millisecond
     * @return request timeout config
     */
    private SimpleClientHttpRequestFactory getRequestTimeoutConfig(int time) {
        SimpleClientHttpRequestFactory config = new SimpleClientHttpRequestFactory();
        config.setReadTimeout(time);
        return config;
    }
}
