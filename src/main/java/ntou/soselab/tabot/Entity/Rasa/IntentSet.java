package ntou.soselab.tabot.Entity.Rasa;

public class IntentSet {
    public String intent;
    public String entity;
    public boolean reviewResult;
    public String responseMessage;
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

    public void setReviewResult(boolean reviewResult) {
        this.reviewResult = reviewResult;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
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

    public boolean isReviewResult() {
        return reviewResult;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public boolean isEndOfChat() {
        return endOfChat;
    }

    @Override
    public String toString() {
        return "IntentSet{" +
                "intent='" + intent + '\'' +
                ", entity='" + entity + '\'' +
                ", reviewResult=" + reviewResult +
                ", responseMessage='" + responseMessage + '\'' +
                ", endOfChat=" + endOfChat +
                '}';
    }
}
