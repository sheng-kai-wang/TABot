package ntou.soselab.tabot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ntou.soselab.tabot.repository.RedisHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
@EnableScheduling
public class CommitmentRetrievalService {
    private final RedisHandler redisHandler;
    private final String crawlCommitmentUrl;

    @Autowired
    public CommitmentRetrievalService(Environment env, RedisHandler redisHandler) {
        this.redisHandler = redisHandler;
        String tabotCrawlerHost = env.getProperty("tabot-crawler.host");
        String tabotCrawlerPort = env.getProperty("tabot-crawler.port");
        String crawlCommitmentPath = env.getProperty("crawl-commitment.path");
        this.crawlCommitmentUrl = "http://" + tabotCrawlerHost + ":" + tabotCrawlerPort + crawlCommitmentPath;
        index();
    }

    /**
     * execute every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    private void index() {
        System.out.println("[DEBUG][CommitmentRetrievalService] trigger index regularly.");
        crawlCommitment();
    }

    private void crawlCommitment() {
        // setup request parameter
        JsonArray payload = new JsonArray();
        JsonObject allRepo = redisHandler.getAllRepository();
        for (String groupName : allRepo.keySet()) {
            JsonObject groupData = new JsonObject();
            groupData.addProperty("groupName", groupName);
            groupData.add("repositoryList", allRepo.get(groupName));
            payload.add(groupData);
        }

        if (payload.isEmpty()) return;

        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
        ResponseEntity<String> response = template.exchange(crawlCommitmentUrl, HttpMethod.POST, entity, String.class);
        System.out.println("[DEBUG] " + response.getBody());
    }
}
