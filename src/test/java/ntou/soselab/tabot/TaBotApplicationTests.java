package ntou.soselab.tabot;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

//@SpringBootTest
class TaBotApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void languageTest(){
        String testMsg = "中文測試";
        String testMsg1 = "天氣總算放晴，沒有rain、太陽很big、有點hot、讓我想到以前還是student時，喜歡在這樣的天氣，吃一球ice cream，真的會讓人很happy";
        String testMsg2 = "我是很busy，因為我很多things要do";
        String testMsg3 = "english testing";
        String testMsg4 = "import(引入) is one of the keep word in Java";

        LanguageDetector detector = LanguageDetectorBuilder.fromLanguages(Language.CHINESE, Language.ENGLISH).build();
//        String test = new String(testMsg2.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
//        System.out.println(test);
        System.out.println(testMsg2);
        System.out.println(detector.detectLanguageOf(testMsg2));
    }

}