package ntou.soselab.tabot;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.junit.jupiter.api.Test;

public class GoogleSheetsTest {

    private static final String APPLICATION_NAME = "Google Sheets API";
    private static final String SHEET_ID = "1sqlqVZ-JoCJ27U4M_7oxhTz8XFuIkNoE-F-bVgKrglQ";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    @Test
    public static void main(String[] args) {
        try {
            new GoogleSheetsTest().doGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doGet() throws IOException {
        try {
            // sheet檔案的Id;
            String range = "A1:B10";
            Sheets service = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    getCredentials())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            ValueRange response = service.spreadsheets().values().get(SHEET_ID, range).execute();
            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                System.out.println("No data found.");
            } else {
                for (List row : values) {
                    // 列印出欄位A與E.
                    System.out.printf("%s, %s\n", row.get(0), row.get(1));
                }
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private Credential getCredentials() throws IOException {
        // 載入resource目錄下的「憑證」檔案。
        InputStream is = GoogleSheetsTest.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        assert is != null;
        GoogleCredential credential = GoogleCredential.fromStream(is).createScoped(SCOPES);

        return credential;
    }
}

