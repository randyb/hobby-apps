package org.randyb.statusboard;

import com.mongodb.client.model.Sorts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.randyb.Main.DB;
import static spark.Spark.get;
import static spark.Spark.port;

public class StatusBoardServices {

    private static final Logger LOG = LoggerFactory.getLogger(StatusBoardServices.class);

    public static void addServices() {

        get("/board/door", (req, res) -> {
            res.type("text/plain");

            SimpleDateFormat sdf = new SimpleDateFormat("\"EEE, MMM d\",h:mm a");
            sdf.setTimeZone(TimeZone.getTimeZone("US/Central"));

            List<String> rows =
                DB.getCollection("door")
                    .find()
                    .limit(20)
                    .sort(Sorts.descending("time"))
                    .map(d -> sdf.format(d.getDate("time")))
                    .into(new ArrayList<>());

            return String.join("\n", rows);
        });
    }

    public static void main(String[] args) throws Exception {
        port(80);
        addServices();
    }

}
