package ntou.soselab.tabot.Entity.Rasa;

public class IntentSet {
    public String intent;
    public String entity;
    public boolean endOfChat;

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setEndOfChat(boolean endOfChat) {
        this.endOfChat = endOfChat;
    }

    public String getIntent() {
        return intent;
    }

    public String getEntity() {
        return entity;
    }

    public boolean hasLostName() {
        return endOfChat;
    }

    @Override
    public String toString() {
        return "IntentSet{" +
                "intent='" + intent + '\'' +
                ", entity='" + entity + '\'' +
                ", endOfChat=" + endOfChat +
                '}';
    }
}
