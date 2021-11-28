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
        System.out.println("result: " + result);
    }

    /**
     * 查課程地圖 by Section
     */
    @Test
    public void readCurriculumMapBySectionTest() {
        String result = new Neo4jHandler("Java").readCurriculumMap("Argument Promotion and Casting");
        System.out.println("result: " + result);
    }

    /**
     * 查投影片 by Chapter name
     */
    @Test
    public void readSlideshowByChapterNameTest() {
        String result = new Neo4jHandler("Java").readSlideshowByName("Control Statements");
        System.out.println("result: " + result);
    }

    /**
     * 查投影片 by Section name
     */
    @Test
    public void readSlideshowBySectionNameTest() {
        String result = new Neo4jHandler("Java").readSlideshowByName("Introducing enum Types");
        System.out.println("result: " + result);
    }

    /**
     * 查投影片 by Chapter id
     */
    @Test
    public void readSlideshowByChapterIdTest() {
        String result = new Neo4jHandler("Java").readSlideshowById(1);
        System.out.println("result: " + result);
    }

    /**
     * 擴增課程地圖
     */
    @Test
    public void addReferenceTest() {
        new Neo4jHandler("Java").addReference(
                "Control Statements",
                "test",
                "testURL",
                "testRemark");
    }

    /**
     * 查個人化考題
     */
    @Test
    public void readPersonalizedTestTest() {
        String result = new Neo4jHandler("Java").readPersonalizedTest("0076D053");
        System.out.println(result);
    }

    /**
     * 查個人化教材
     */
    @Test
    public void readPersonalizedSubjectMatterTest() {
        String result = new Neo4jHandler("Java").readPersonalizedSubjectMatter("0076D053");
        System.out.println("result: " + result);
    }
}
