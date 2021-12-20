package ntou.soselab.tabot.Entity.Rasa;

import com.google.gson.Gson;

public class Intent {
//    private static final Gson gson = new Gson();
    /* chat user id, assume this is 'discord user' id */
    public String recipient_id;
    /* response object from rasa */
    public IntentSet custom;

    public void setCustom(IntentSet custom) {
        this.custom = custom;
    }

    public void setRecipient_id(String recipient_id) {
        this.recipient_id = recipient_id;
    }

    public IntentSet getCustom() {
        return custom;
    }

    public String getRecipient_id() {
        return recipient_id;
    }

    @Override
    public String toString() {
        return "Intent{" +
                "recipient_id='" + recipient_id + '\'' +
                ", custom=" + custom +
                '}';
    }

}
