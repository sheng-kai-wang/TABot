package ntou.soselab.tabot.Repository;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * CURD of Google Sheets API.
 */
public class SheetsHandler {
    private final Sheets sheetsService;
    private final String applicationName;
    private final String spreadsheetId;
    private final String credentialsFilePath;
    private final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private final Gson gson;
    private static final int INT_TO_CHAR = 65;

    /**
     * It's the constructor,
     * and initialize the "getSheetsService" method.
     *
     * @param sheetName like "course" or "exam"
     */
    public SheetsHandler(String sheetName) {
        Properties properties = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/application.properties")) {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.applicationName = properties.getProperty("sheets.application.name");
        this.spreadsheetId = properties.getProperty("sheets." + sheetName + ".id");
        this.credentialsFilePath = properties.getProperty("sheets.credentials.path");
        this.sheetsService = getSheetsService();
        this.gson = new Gson();
        trimWorksheet();
    }

    /**
     * Get the sheets service,
     * and then we can perform CRUD operations on sheets.
     *
     * @return a Sheets Service
     */
    private Sheets getSheetsService() {
        Credential credential = authorize();
        try {
            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(applicationName)
                    .build();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get credential by Service Account,
     * Sheets must be authorized to this Service Account.
     *
     * @return
     */
    private Credential authorize() {

        InputStream is = SheetsHandler.class.getResourceAsStream(credentialsFilePath);

//        Verify by Service Account
        GoogleCredential credential = null;
        try {
            credential = GoogleCredential.fromStream(is).createScoped(SCOPES);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return credential;


//         Verify by OAuth 2.0
//        InputStream is = SheetsHandler.class.getResourceAsStream(credentialsFilePath);
//        assert is != null;
//        GoogleClientSecrets clientSecrets = null;
//        try {
//            clientSecrets = GoogleClientSecrets
//                    .load(JacksonFactory.getDefaultInstance(), new InputStreamReader(is));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        GoogleAuthorizationCodeFlow flow = null;
//        try {
//            assert clientSecrets != null;
//            flow = new GoogleAuthorizationCodeFlow.Builder(
//                GoogleNetHttpTransport.newTrustedTransport(),
//                JacksonFactory.getDefaultInstance(),
//                clientSecrets,
//                SCOPES)
//                .setDataStoreFactory(new FileDataStoreFactory(new File("token")))
//                .setAccessType("offline")
//                .build();
//        } catch (IOException | GeneralSecurityException e) {
//            e.printStackTrace();
//        }
//
//        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
////        LocalServerReceiver receiver = new LocalServerReceiver();
//        try {
//            assert flow != null;
//            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
    }

    /**
     * Clear the unwanted content in the worksheet like "(blank)", "..."
     */
    public void trimWorksheet() {
        for (Sheet sheet : getSheetsList()) {
            String titleString = sheet.getProperties().getTitle();
            String sheetsContentString = readContent(sheet.getProperties().getTitle(), "");
            if (sheetsContentString.equals("null")) continue;
            JSONArray sheetsContent = new JSONArray(sheetsContentString);
            int columnNum = new JSONArray(new JSONArray(sheetsContent).get(0).toString()).length();
            int rowNum = new JSONArray(sheetsContent).length();
            List<List<Object>> updateContents = new ArrayList<>();
            for (int i = 0; i < rowNum; i++) {
                updateContents.add(new ArrayList<>(List.of("", "", "")));
            }
//          "65" is for convert int to char
            updateContent(titleString,
                    (char) (INT_TO_CHAR + columnNum) + "1" + ":" + (char) (INT_TO_CHAR + columnNum + 2) + rowNum,
                    updateContents);
        }
    }

    /**
     * Get the list of sheets.
     *
     * @return list of sheets.
     */
    private List<Sheet> getSheetsList() {
        List<Sheet> sheetsList = null;
        try {
            sheetsList = sheetsService.spreadsheets()
                    .get(spreadsheetId)
                    .setIncludeGridData(false)
                    .execute()
                    .getSheets();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sheetsList;
    }

    /**
     * Read the list of sheets title.
     *
     * @return the list of sheets title.
     */
    public List<String> readSheetsTitles() {
        ArrayList<String> sheetsTitles = new ArrayList<>();
        for (Sheet sheet : getSheetsList()) {
            sheetsTitles.add(sheet.getProperties().getTitle());
        }
        return sheetsTitles;
    }

    /**
     * Read the contents of the sheets.
     *
     * @param worksheet like "FAQ", is the name of the worksheet.
     * @param range     like "A2:B10", is the range block of the worksheet.
     *                  If you submit an empty string like this "", it will respond the content of the entire worksheet.
     * @return is a string of the sheet content, similar in form to a matrix.
     */
    public String readContent(String worksheet, String range) {
        String requestRange = null;
        if ("".equals(range)) requestRange = worksheet;
        else requestRange = worksheet + "!" + range;

        ValueRange response = null;
        try {
            response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, requestRange)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert response != null;
        return gson.toJson(response.getValues());
    }

    /**
     * Read the contents of the sheets by "key", it's the leftmost field name in worksheet.
     *
     * @param worksheet is the name of the worksheet like "FAQ".
     * @param key       is the leftmost field name in worksheet like "常見問題_Java亂碼".
     * @return It will return a JSONObject containing the header of the worksheet and the value to be queried,
     * just like "{'問題':['...'],'意圖名稱':['...'],'回答':['...']}".
     */
    public JSONObject readContentByKey(String worksheet, String key) {
        JSONObject result = new JSONObject();
        JSONArray sheetsContent = new JSONArray(readContent(worksheet, ""));
        JSONArray headers = new JSONArray(sheetsContent.get(0).toString());
        for (int i = 1; i < sheetsContent.length(); i++) {
            JSONArray rows = new JSONArray(sheetsContent.get(i).toString());
            if (rows.get(0).equals(key)) {
                for (int j = 0; j < rows.length(); j++) {
                    result.append(headers.get(j).toString(), rows.get(j));
                }
            }
        }
        return result;
    }

    /**
     * Read the contents of the sheets by "header", it's the first row in worksheet.
     *
     * @param worksheet is the name of the worksheet like "QuestionBank".
     * @param header    is the top row in worksheet like "corresponding exam / exam".
     * @return It will return a JSONObject containing the leftmost field name (key) of the worksheet and the value to be queried,
     * just like "{'1':['...'],'2':['...'],'3':['...']}".
     */
    public JSONObject readContentByHeader(String worksheet, String header) {
        JSONObject result = new JSONObject();

        // get the leftmost column as the "key"
        JSONArray keys = new JSONArray(readContent(worksheet, "A:A"));

        // find the coordinates corresponding to the header
        JSONArray headers = new JSONArray(readContent(worksheet, "1:1")).getJSONArray(0);
        int columnIndex = 0;
        for (int i = 0; i < headers.length(); i++) {
            if (headers.get(i).equals(header)) columnIndex = i;
        }
        char coordinateChar = (char) (columnIndex + INT_TO_CHAR);

        // get the values in the column
        JSONArray values = new JSONArray(readContent(worksheet, coordinateChar + ":" + coordinateChar));

        // package the result into the key-value pairs
        // start at 1, skip the header
        for (int i = 1; i < keys.length(); i++) {
            String key = keys.get(i).toString().split("\"")[1];
            String value;
            try {
                // some value is [], there's not "\"",
                // and the length of values may be shorter than keys, so it must be filled with "<<null>>".
                value = values.get(i).toString().split("\"")[1];
            } catch (ArrayIndexOutOfBoundsException | JSONException e) {
                value = "<<null>>";
            }
            // some value is ["v"], the others are ["<<null>>"]
            result.append(key, value);
        }
        return result;
    }

    /**
     * Create the contents of the sheets by row.
     *
     * @param worksheet is the name of the worksheet like "FAQ".
     * @param contents  is the data you will add in unit of row, and you can add multiple rows at once by List.
     */
    public void createContent(String worksheet, List<List<Object>> contents) {
        ValueRange body = new ValueRange().setValues(contents);

        try {
            sheetsService.spreadsheets().values()
                    .append(spreadsheetId, worksheet, body)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .setIncludeValuesInResponse(true)
                    .execute();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the contents of the sheets by row.
     *
     * @param worksheet is the name of worksheet like "FAQ".
     * @param range     is the name of range block like "A17:C18".
     * @param contents  is the data you will add in unit of row, and you can add multiple rows at once by List.
     */
    public void updateContent(String worksheet, String range, List<List<Object>> contents) {
        ValueRange body = new ValueRange().setValues(contents);

        try {
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, worksheet + "!" + range, body)
                    .setValueInputOption("RAW")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete the contents of the sheets by row.
     *
     * @param worksheet The worksheet will be deleted some data.
     * @param index     The row index will be deleted.
     */
    public void deleteContent(String worksheet, int index) {
        try {
            sheetsService.spreadsheets().values()
                    .clear(spreadsheetId, worksheet + "!" + index + ":" + index, new ClearValuesRequest())
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Clear the contents of the sheets.
     *
     * @param worksheet The worksheet will be clear.
     */
    public void clearContent(String worksheet) {
        JSONArray sheetsContent = new JSONArray(new JSONArray(readContent(worksheet, "")));

        int rowNum = sheetsContent.length();
        int columnNum = new JSONArray(sheetsContent.get(0).toString()).length();
        String range = worksheet + "!A1:" + (char) (INT_TO_CHAR + columnNum) + rowNum;

        try {
            sheetsService.spreadsheets().values()
                    .clear(spreadsheetId, range, new ClearValuesRequest())
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}