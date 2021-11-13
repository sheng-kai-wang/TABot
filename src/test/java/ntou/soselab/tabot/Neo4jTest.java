package ntou.soselab.tabot;

import ntou.soselab.tabot.repository.Neo4jHandler;
import org.junit.jupiter.api.Test;

public class Neo4jTest {
    /**
     * 查課程地圖 by Chapter
     */
    @Test
    public void readCurriculumMapByChapterTest() {
        String result = new Neo4jHandler("Java").readCurriculumMap("Methods");
//        String result = new Neo4jHandler("SE").readCurriculumMap("需求工程");
        System.out.println("result: " + result);
    }

    /**
     * 查課程地圖 by Chapter
     */
    @Test
    public void readCurriculumMapBySectionTest() {
        String result = new Neo4jHandler("Java").readCurriculumMap("Argument Promotion and Casting");
        System.out.println("result: " + result);
    }

    /**
     * 查投影片
     */
    @Test
    public void readSlideshowTest() {
        String result2 = new Neo4jHandler("Java").readSlideshow("Introducing enum Types");
        System.out.println("result: " + result2);
    }

    /**
     * 擴增課程地圖
     */
    @Test
    public void addReferenceTest() {
        new Neo4jHandler("Java").addReference("Control Statements", "test", "testURL");
    }

    /**
     * 查個人化考題
     */
    @Test
    public void readPersonalizedTestTest() {
        String result3 = new Neo4jHandler("Java").readPersonalizedTest("0076D053");
        System.out.println(result3);
    }

    /**
     * 查個人化教材
     */
    @Test
    public void readPersonalizedSubjectMatterTest() {
        String result4 = new Neo4jHandler("Java").readPersonalizedSubjectMatter("0076D053");
        System.out.println("result: " + result4);
    }
}
