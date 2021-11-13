package ntou.soselab.tabot.repository;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * CURD of Google Sheets API.
 */
public class SheetsHandler {

    private Sheets sheetsService;
    private String applicationName;
    private String spreadsheetId;
    private String credentialsFilePath;
    private final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    private Gson gson;

    /**
     * It's the constructor,
     * we configure member variables by application.yml,
     * and initialize the "getSheetsService" method.
     *
     * @param course like "SE" or "java"
     */
    public SheetsHandler(String course) {
        InputStream inputStream = getClass().getResourceAsStream("/application.yml");
        Map<String, Map<String, String>> configData = new Yaml().load(inputStream);

        this.applicationName = configData.get("sheets").get("application-name");
        this.spreadsheetId = configData.get("sheets").get("spread-sheet-id-" + course);
        this.credentialsFilePath = configData.get("sheets").get("credentials-file-path");
        try {
            assert inputStream != null;
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        List<Sheet> worksheetList = null;
        try {
            worksheetList = sheetsService.spreadsheets()
                    .get(spreadsheetId)
                    .setIncludeGridData(false)
                    .execute()
                    .getSheets();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert worksheetList != null;
        for (Sheet sheet : worksheetList) {
            String titleString = sheet.getProperties().getTitle();
            JSONArray sheetsContent = new JSONArray(readContent(titleString, ""));
            int columnNum = new JSONArray(new JSONArray(sheetsContent).get(0).toString()).length();
            int rowNum = new JSONArray(sheetsContent).length();
            List<List<Object>> contents = new ArrayList<>();
            for (int i = 0; i < rowNum; i++) {
                contents.add(new ArrayList<>(List.of("", "", "")));
            }
            updateContent(titleString,
                    (char) (65 + columnNum) + "1" + ":" + (char) (65 + columnNum + 2) + rowNum,
                    contents);
        }
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
        System.setProperty("webdriver.chrome.driver", "/path/to/chromedriver");

        String requestRange = null;
        if ("".equals(range)) {
            requestRange = worksheet;
        } else {
            requestRange = worksheet + "!" + range;
        }

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
                for (int j = 0; j < headers.length(); j++) {
                    result.append(headers.get(j).toString(), rows.get(j));
                }
            }
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

//        delete by "read" and "update" methods
        JSONArray sheetsContent = new JSONArray(readContent(worksheet, ""));
        int columnNum = new JSONArray(new JSONArray(sheetsContent).get(0).toString()).length();

        ArrayList<Object> list = new ArrayList<>();
        for (int i = 0; i < columnNum; i++) {
            list.add("");
        }
        List<List<Object>> contents = List.of(list);
        updateContent(worksheet, index + ":" + index, contents);


//        Delete by "DeleteDimensionRequest"
//        response = sheetsService.spreadsheets().values()
//                    .get(spreadsheetId, range)
//                    .execute();

//        DeleteDimensionRequest deleteRequest = new DeleteDimensionRequest()
//                .setRange(
//                        new DimensionRange()
//                                .setSheetId(0)
//                                .setDimension("ROWS")
//                                .setStartIndex(startIndex)
//                                .setEndIndex(endIndex)
//                );
////
//        List<Request> requests = new ArrayList<>();
//        requests.add(new Request().setDeleteDimension(deleteRequest));
////
//        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
//        try {
//            sheetsService.spreadsheets().batchUpdate(spreadsheetId, body).execute();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}