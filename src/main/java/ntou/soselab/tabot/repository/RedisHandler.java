package ntou.soselab.tabot.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;


@Service
public class RedisHandler {
    @Resource
    RedisTemplate<String, HashMap<String, String>> redisTemplate;

    HashOperations<String, String, String> hashOperations;
    public final String GITHUB_REPOSITORY_KEEP_KEY_PREFIX = "[repo]";
    private static String JUDGE_GROUP_NAME_PREFIX;

    @Autowired
    public RedisHandler(Environment env) {
        RedisHandler.JUDGE_GROUP_NAME_PREFIX = env.getProperty("judge-group-name.prefix");
    }

    @PostConstruct
    public void init() {
        this.hashOperations = redisTemplate.opsForHash();
    }

    public void createPair(String groupName, String key, String value) {
        setSerializer();
        hashOperations.putIfAbsent(groupName, key, value);
    }

    public Map<String, String> readPairAll(String groupName) {
        setSerializer();
        return hashOperations.entries(groupName);
    }

    public String readPairByKey(String groupName, String key) {
        setSerializer();
        String oldKeyString = getCompletedKey(groupName, key);
        return hashOperations.get(groupName, oldKeyString);
    }

    public String updatePair(String groupName, String key, String value) {
        setSerializer();
        String oldValue = deletePair(groupName, key);
        hashOperations.putIfAbsent(groupName, key, value);
        return oldValue;
    }

    public String deletePair(String groupName, String key) {
        setSerializer();
        String deletedValue = hashOperations.get(groupName, key);
        hashOperations.delete(groupName, key);
        return deletedValue;
    }

    public boolean hasSameKey(String groupName, String keys) {
        setSerializer();
        Set<String> newKeySet = new HashSet<>(List.of(keys.split(",")));
        ArrayList<String> oldKeyList = new ArrayList<>();
        hashOperations.keys(groupName).forEach(k -> {
            oldKeyList.addAll(List.of(k.split(",")));
        });
        for (String s : newKeySet) {
            if (oldKeyList.contains(s)) return true;
        }
        return false;
    }

    public String getCompletedKey(String groupName, String key) {
        setSerializer();
        for (String keyAliases : hashOperations.keys(groupName)) {
            String[] keyAliasesArray = keyAliases.split(",");
            if (Arrays.asList(keyAliasesArray).contains(key)) return keyAliases;
        }
        return null;
    }

    /**
     * @return like {"GROUP 1": [{"username":"sheng-kai-wang", "repository":"TABot"}, ...]}
     */
    public JsonObject getAllRepository() {
        setSerializer();
        JsonObject allRepo = new JsonObject();
        try (RedisConnection redisConnection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {
            ScanOptions options = ScanOptions.scanOptions().match(JUDGE_GROUP_NAME_PREFIX.concat("*")).count(100).build();
            Cursor<byte[]> c = redisConnection.scan(options);

            while (c.hasNext()) {
                String groupName = new String(c.next());
                JsonArray groupRepos = new JsonArray();
                hashOperations
                        .keys(groupName)
                        .forEach(k -> {
                            if (k.contains(GITHUB_REPOSITORY_KEEP_KEY_PREFIX)) {
                                String repo = k.split(",")[0].replace(GITHUB_REPOSITORY_KEEP_KEY_PREFIX, "");
                                String username = Objects.requireNonNull(hashOperations.get(groupName, k)).split("/")[3];
                                JsonObject content = new JsonObject();
                                content.addProperty("username", username);
                                content.addProperty("repository", repo);
                                groupRepos.add(content);
                            }
                        });
                allRepo.add(groupName, groupRepos);
            }
        }
        return allRepo;
    }

    private void setSerializer() {
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
    }
}
