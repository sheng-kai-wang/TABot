package ntou.soselab.tabot.repository;

import com.google.gson.Gson;
import org.neo4j.driver.*;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class Neo4jHandler implements AutoCloseable {

    private String url;
    private String username;
    private String password;
    private Driver driver;

    private Gson gson;

    public Neo4jHandler() {
        InputStream inputStream = getClass().getResourceAsStream("/application.yml");
        Map<String, Map<String, String>> data = new Yaml().load(inputStream);

        this.url = data.get("neo4j").get("url");
        this.username = data.get("neo4j").get("username");
        this.password = data.get("neo4j").get("password");
        this.driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));

        this.gson = new Gson();
    }

    @Override
    public void close() {
        driver.close();
    }

    public List<String> doCypher(final String cypher) {
        try (Session session = driver.session()) {
            List<String> response = session.writeTransaction((Transaction tx) -> {
                List<String> dataList = new ArrayList<>();
                final Result result = tx.run(cypher);
                while (result.hasNext()) {
                    Record record = result.next();
                    dataList.add(gson.toJson(record));
                }
                return dataList;
            });
            close();
            return response;
        }
    }

    public static void main(String... args) throws Exception {
//        "課程知識地圖"
//        "查投影片"
//        "MATCH (a:Java_Section{name: $name})-[rel:Item]-(b)-[rel2:Item]-(c) RETURN a,c"
//        "個人化教材"
//        "MATCH (a:Java109_Student{name: $name})-[rel]-(b)-[rel2]-(c)  RETURN c"
//        "個人化考題"
//        "MATCH (a:Java109_Student{name: $name})-[rel]-(b) RETURN b"
        List<String> results = new Neo4jHandler().doCypher("MATCH p=()-[r:BELONG]->() RETURN p LIMIT 25");
        System.out.println(results);
    }
}