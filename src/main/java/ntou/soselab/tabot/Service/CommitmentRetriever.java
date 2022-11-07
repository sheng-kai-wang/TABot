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
public class CommitmentRetriever {
    private final RedisHandler redisHandler;
    private final String group01Url;
    private final String group02Url;
    private final String group03Url;
    private final String group04Url;
    private final String group05Url;
    private final String group06Url;
    private final String groupOtherUrl;
    private final String registerPath;
    private final String retrievalPath;
    public final static String NO_RESULT = "all cosine is 0";

    @Autowired

    public CommitmentRetriever(Environment env, RedisHandler redisHandler) {
        this.redisHandler = redisHandler;
        this.group01Url = env.getProperty("github-commit-message-searcher.group-01.url");
        this.group02Url = env.getProperty("github-commit-message-searcher.group-02.url");
        this.group03Url = env.getProperty("github-commit-message-searcher.group-03.url");
        this.group04Url = env.getProperty("github-commit-message-searcher.group-04.url");
        this.group05Url = env.getProperty("github-commit-message-searcher.group-05.url");
        this.group06Url = env.getProperty("github-commit-message-searcher.group-06.url");
        this.groupOtherUrl = env.getProperty("github-commit-message-searcher.group-other.url");
        this.registerPath = env.getProperty("github-commit-message-searcher.register.path");
        this.retrievalPath = env.getProperty("github-commit-message-searcher.retrieval.path");
        registerAllRepository();
    }

    /**
     * execute every day
     */
    @Scheduled(cron = "0 0 0 * * *")
    private void registerAllRepository() {
        registerRepository("GROUP 1");
        registerRepository("GROUP 2");
        registerRepository("GROUP 3");
        registerRepository("GROUP 4");
        registerRepository("GROUP 5");
        registerRepository("GROUP 6");
        registerRepository("GROUP 7");
        registerRepository("GROUP 8");
        registerRepository("GROUP 9");
        registerRepository("GROUP 10");
    }

    public void registerRepository(String groupName) {
        System.out.println("[DEBUG][CommitmentRetriever] start to register " + groupName);

        // setup request payload
        JsonObject payload = new JsonObject();
        JsonArray allReposData = new JsonArray();
        JsonObject allRepos = redisHandler.getGroupRepository(groupName);
        JsonObject groupData = new JsonObject();
        groupData.addProperty("projectName", groupName);
        groupData.add("repoNames", allRepos.get(groupName));
        allReposData.add(groupData);

        if (allReposData.isEmpty()) {
            System.out.println("--- [DEBUG] no repos yet");
            return;
        }
        payload.add("allProjects", allReposData);

//        RestTemplate template = new RestTemplate(getRequestTimeoutConfig(1000_000));
        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
        String url = judgeGroupUrl(groupName) + registerPath;
        ResponseEntity<String> response = template.exchange(url, HttpMethod.POST, entity, String.class);
        JsonObject responseMsg = new Gson().fromJson(response.getBody(), JsonObject.class);
        System.out.println("[DEBUG][CommitmentRetriever] " + responseMsg.get("status").getAsString());
    }

    /**
     * execute every day (deprecated)
     */
//    @Scheduled(cron = "0 0 0 * * *")
    public void registerAllRepositoryOneContainer() {
        System.out.println("[DEBUG][CommitmentRetriever] start to register all repos");

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

//        RestTemplate template = new RestTemplate(getRequestTimeoutConfig(1000_000));
        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
        ResponseEntity<String> response = template.exchange(registerPath, HttpMethod.POST, entity, String.class);
        JsonObject responseMsg = new Gson().fromJson(response.getBody(), JsonObject.class);
        System.out.println("[DEBUG][CommitmentRetriever] " + responseMsg.get("status").getAsString());
    }

    public JsonArray retrieveCommitMsg(String groupName, String keywords, JsonArray range, int quantity) {
        RestTemplate template = new RestTemplate(getRequestTimeoutConfig(5_0000));
        String baseUrl = judgeGroupUrl(groupName) + retrievalPath;
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("projectName", groupName)
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
        System.out.println("[DEBUG][CommitmentRetriever] " + responseMsg.get("status").getAsString());
        if (responseMsg.get("rank").isJsonNull()) return null;
        if (responseMsg.get("rank").isJsonArray()) return responseMsg.get("rank").getAsJsonArray();
        if (responseMsg.get("rank").getAsString().equals(NO_RESULT)) return new JsonArray();
        return null;
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

    private String judgeGroupUrl(String groupName) {
        String groupNumber = groupName.split(" ")[1];
        if (groupNumber.equals("1")) return group01Url;
        if (groupNumber.equals("2")) return group02Url;
        if (groupNumber.equals("3")) return group03Url;
        if (groupNumber.equals("4")) return group04Url;
        if (groupNumber.equals("5")) return group05Url;
        if (groupNumber.equals("6")) return group06Url;
        return groupOtherUrl;
    }
}
