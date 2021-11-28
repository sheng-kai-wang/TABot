package ntou.soselab.tabot.repository;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import org.neo4j.driver.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Cypher statement for curriculum map related functions,
 * currently java and SE are in the same database.
 */
public class Neo4jHandler implements AutoCloseable {

    private String url;
    private String username;
    private String password;
    private Driver driver;

    private Map<String, String> cypherData;
    private Gson gson;

    /**
     * It's the constructor,
     * we configure member variables by application.yml,
     * and "cypher" statement is come from cypher.yml.
     *
     * @param course like "SE" or "Java"
     */
    public Neo4jHandler(String course) {
        InputStream configInputStream = getClass().getResourceAsStream("/application.yml");
        Map<String, Map<String, String>> configData = new Yaml().load(configInputStream);

        this.url = configData.get("neo4j").get(course + "Url");
        this.username = configData.get("neo4j").get("username");
        this.password = configData.get("neo4j").get("password");
        this.driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));
        try {
            assert configInputStream != null;
            configInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        InputStream cypherInputStream = getClass().getResourceAsStream("/cypher.yml");
        cypherData = new Yaml().load(cypherInputStream);
        try {
            assert cypherInputStream != null;
            cypherInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.gson = new Gson();
    }

    /**
     * Used to close the driver of neo4j.
     */
    @Override
    public void close() {
        driver.close();
    }

    /**
     * Called by the following other functions.
     *
     * @param cypherString for do something on neo4f.
     * @return Get the returned data from neo4j.
     */
    public List<String> doCypher(final String cypherString) {
        try (Session session = driver.session()) {
            List<String> response = session.writeTransaction((Transaction tx) -> {
                List<String> dataList = new ArrayList<>();
                final Result result = tx.run(cypherString);
                while (result.hasNext()) {
                    Record record = result.next();
                    dataList.add(gson.toJson(record));
                }
                return dataList;
            });
            return response;
        }
    }

    /**
     * read curriculum map content.
     *
     * @param queryName the Chapter or Section name of curriculum map you want to read.
     * @return Use JSON string to describe the Section list in the Chapter.
     */
    public String readCurriculumMap(String queryName) {
        String cypherString = cypherData.get("read-curriculum-map").replace("<<queryName>>", queryName);
        List<String> cypherResponses = doCypher(cypherString);
        Set<String> results = new HashSet<>();
        for (String cypherResponse : cypherResponses) {
            results.add(JsonPath.read(cypherResponse, "$.values[0].adapted.properties.name.val"));
        }
        return gson.toJson(results);
    }

    /**
     * read the slideshow of curriculum map.
     *
     * @param sectionName the Section name of curriculum map you want to read.
     * @return the URL of the Section's slideshow.
     */
    public String readSlideshow(String sectionName) {
        String cypherString = cypherData.get("read-slideshow").replace("<<sectionName>>", sectionName);
        List<String> cypherResponses = doCypher(cypherString);
        return JsonPath.read(cypherResponses.get(0), "$.values[0].val");
    }

    /**
     * add reference of Section in curriculum map.
     *
     * @param sectionName the Section name of curriculum map you want to add data.
     * @param referenceName the reference's name you want to add.
     * @param referenceURL the reference's URL you want to add.
     */
    public void addReference(String sectionName, String referenceName, String referenceURL, String referenceRemark) {
        String cypherString = cypherData.get("add-reference")
                .replace("<<sectionName>>", sectionName)
                .replace("<<referenceName>>", referenceName)
                .replace("<<referenceURL>>", referenceURL)
                .replace("<<referenceRemark>>", referenceRemark);
        doCypher(cypherString);
    }

    /**
     * read personalized test of the curriculum map.
     *
     * @param studentID is in the Google sheets and Firebase.
     * @return the test belonging to weakness.
     */
    public String readPersonalizedTest(String studentID) {
        String cypherString = cypherData.get("read-personalized-test").replace("<<studentID>>", studentID);
        List<String> cypherResponses = doCypher(cypherString);
        List<String> results = new ArrayList<>();
        for (String cypherResponse : cypherResponses) {
            results.add(JsonPath.read(cypherResponse, "$.values[0].adapted.properties.name.val"));
        }
        return gson.toJson(results);
    }

    /**
     * read personalized subject matter of the curriculum map.
     *
     * @param studentID is in the Google sheets and Firebase.
     * @return the Section belonging to weakness.
     */
    public String readPersonalizedSubjectMatter(String studentID) {
        String cypherString = cypherData.get("read-personalized-subject-matter").replace("<<studentID>>", studentID);
        List<String> cypherResponses = doCypher(cypherString);
        Set<String> results = new HashSet<>();
        for (String cypherResponse : cypherResponses) {
            results.add(JsonPath.read(cypherResponse, "$.values[0].adapted.properties.name.val"));
        }
        return gson.toJson(results);
    }
}