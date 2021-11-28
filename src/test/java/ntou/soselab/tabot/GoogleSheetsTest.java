package ntou.soselab.tabot;

import ntou.soselab.tabot.repository.SheetsHandler;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class GoogleSheetsTest {
    @Test
    public void trimTest() {
        new SheetsHandler("Java");
    }

    @Test
    public void readTestWithRange() {
        String response1 = new SheetsHandler("Java").readContent("FAQ", "A1:C3");
        System.out.println(response1);
    }

    @Test
    public void readTestAllWorksheet() {
        String response2 = new SheetsHandler("Java").readContent("FAQ", "");
        System.out.println(response2);
    }

    @Test
    public void readByValueTest() {
        JSONObject value = new SheetsHandler("Java").readContentByKey("FAQ", "garbage_collection");
        System.out.println(value);
    }

    @Test
    public void createTest() {
        List<List<Object>> lists = new ArrayList<>(List.of(new ArrayList<>(List.of("aaa", 123, true))));
        new SheetsHandler("Java").createContent("FAQ", lists);
    }

    @Test
    public void updateTest() {
        List<List<Object>> lists2 = new ArrayList<>(List.of(
                new ArrayList<>(List.of("00111", "00222", "00333")),
                new ArrayList<>(List.of("00444", "00555", "00666"))));
        new SheetsHandler("Java").updateContent("FAQ", "A17:C18", lists2);
    }

    @Test
    public void deleteTest() {
        new SheetsHandler("Java").deleteContent("FAQ", 17);
        new SheetsHandler("Java").deleteContent("FAQ", 18);
    }
}

