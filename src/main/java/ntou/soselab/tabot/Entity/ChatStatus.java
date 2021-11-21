package ntou.soselab.tabot.Entity;

import ntou.soselab.tabot.Entity.Rasa.Intent;

/**
 * user's chat status
 */
public class ChatStatus {

    public String userId;
    public long timestamp;
    public Intent currentIntent;

    public ChatStatus(String userId, long timestamp, Intent intent){
        this.userId = userId;
        this.timestamp = timestamp;
        this.currentIntent = intent;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setCurrentIntent(Intent currentIntent) {
        this.currentIntent = currentIntent;
    }

    public String getUserId() {
        return userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Intent getCurrentIntent() {
        return currentIntent;
    }

    @Override
    public String toString() {
        return "ChatStatus{" +
                "userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                ", currentIntent=" + currentIntent +
                '}';
    }
}
