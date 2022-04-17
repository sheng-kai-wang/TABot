package ntou.soselab.tabot.Service.CrawlService;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GradesCrawler {

    private final String loginUrl;
    private final String username;
    private final String password;

    @Autowired
    public GradesCrawler(Environment env){
        this.loginUrl = env.getProperty("tronclass.login.url");
        this.username = env.getProperty("tronclass.account.username");
        this.password = env.getProperty("tronclass.account.password");
    }

    public void jsoupLogin() {
        try {
            Connection.Response login = Jsoup.connect(this.loginUrl)
                    .ignoreContentType(true)
                    .followRedirects(false)
                    .postDataCharset("utf-8")
                    .header("Accept","application/json")
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .header("User-Agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36")
                    .data()
                    .method(Connection.Method.POST)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
