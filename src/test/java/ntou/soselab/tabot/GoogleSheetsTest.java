package ntou.soselab.tabot;

import ntou.soselab.tabot.repository.SheetsHandler;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class GoogleSheetsTest {
    /**
     * Clear unimportant characters in google sheet
     */
    @Test
    public void trimTest() {
        new SheetsHandler("Java");
    }

    /**
     * Read specific range
     */
    @Test
    public void readWithRangeTest() {
        String result = new SheetsHandler("Java").readContent("FAQ", "A1:C3");
        System.out.println(result);
    }

    /**
     * Read full range
     */
    @Test
    public void readAllWorksheetTest() {
        String result = new SheetsHandler("Java").readContent("FAQ", "");
        System.out.println(result);
    }

    /**
     * Use the first column on the left as the key to package the result into a key-value pair
     */
    @Test
    public void readByKeyTest() {
//        JSONObject result = new SheetsHandler("Java").readContentByKey("FAQ", "garbage_collection");
        JSONObject result = new SheetsHandler("Java").readContentByKey("Grades", "00457122");
        System.out.println(result);
    }

    /**
     * Use the first row on the top as the header,
     * and use the first column on the left as the key to package the result into a key-value pair.
     */
    @Test
    public void readByHeaderTest() {
//        JSONObject result = new SheetsHandler("Java").readContentByHeader("QuestionBank", "corresponding exam / exam");
//        System.out.println("result: " + result);
        JSONObject result = new SheetsHandler("Java").readContentByHeader("QuestionBank", "publishable");
        System.out.println("result: " + result);
    }

    /**
     * Create a new row at the bottom
     */
//    @Test
//    public void createTest() {
//        List<List<Object>> lists = new ArrayList<>(List.of(new ArrayList<>(List.of("aaa", 123, true))));
//        new SheetsHandler("Java").createContent("FAQ", lists);
//    }

    /**
     * Update specific range of data
     */
//    @Test
//    public void updateTest() {
//        List<List<Object>> lists2 = new ArrayList<>(List.of(
//                new ArrayList<>(List.of("00111", "00222", "00333")),
//                new ArrayList<>(List.of("00444", "00555", "00666"))));
//        new SheetsHandler("Java").updateContent("Grades Test", "A17:C18", lists2);
//    }

    /**
     * Delete a row data
     */
//    @Test
//    public void deleteTest() {
//        new SheetsHandler("Java").deleteContent("FAQ", 17);
//        new SheetsHandler("Java").deleteContent("FAQ", 18);
//    }
}

