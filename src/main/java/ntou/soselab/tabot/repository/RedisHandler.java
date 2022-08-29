package ntou.soselab.tabot.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

@Service
public class RedisHandler {
    @Autowired
    RedisTemplate redisTemplate;

    HashOperations hashOperations;

    @PostConstruct
    public void init() {
        this.hashOperations = redisTemplate.opsForHash();
    }
    public void createPair(String groupName, String key, String value) {
//        setSerializer();
        hashOperations.putIfAbsent(groupName, key, value);
    }

    public Map readPair(String groupName) {
//        setSerializer();
        return hashOperations.entries(groupName);
    }

    public String updatePair(String groupName, String key, String value) {
//        setSerializer();
        String oldValue = deletePair(groupName, key);
        hashOperations.putIfAbsent(groupName, key, value);
        return oldValue;
    }

    public String deletePair(String groupName, String key) {
//        setSerializer();
        String deletedValue = (String) hashOperations.get(groupName, key);
        hashOperations.delete(groupName, key);
        return deletedValue;
    }

    public boolean hasContent(String groupName, String key) {
        return hashOperations.hasKey(groupName, key);
    }

    private void setSerializer() {
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
    }
}
