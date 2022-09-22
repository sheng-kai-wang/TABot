package ntou.soselab.tabot.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;


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

    public Map readPairAll(String groupName) {
//        setSerializer();
        return hashOperations.entries(groupName);
    }

    public String readPairByKey(String groupName, String key) {
//        setSerializer();
        String oldKeyString = getCompletedKey(groupName, key);
        return hashOperations.get(groupName, oldKeyString).toString();
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

    public boolean hasSameKey(String groupName, String keys) {
        Set<String> newKeySet = new HashSet<>(List.of(keys.split(",")));
        ArrayList<String> oldKeyList = new ArrayList<>();
        hashOperations.keys(groupName).forEach(k -> {
            oldKeyList.addAll(List.of(k.toString().split(",")));
        });
        Iterator<String> it = newKeySet.iterator();
        while (it.hasNext()) {
            if (oldKeyList.contains(it.next())) return true;
        }
        return false;
    }

    public String getCompletedKey(String groupName, String key) {
        Iterator it = hashOperations.keys(groupName).iterator();
        while (it.hasNext()) {
            String completedKeyString = it.next().toString();
            if (completedKeyString.contains(key)) return completedKeyString;
        }
        return null;
    }

    private void setSerializer() {
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
    }
}
