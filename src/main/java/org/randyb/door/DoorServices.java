package org.randyb.door;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.randyb.Main;

import javax.servlet.ServletOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static spark.Spark.get;
import static spark.Spark.post;

public class DoorServices {

    // secret key for knowing if an event is fake (e.g, from testing)
    private static final String SECRET_KEY = System.getenv("SECRET_KEY");

    public static void addServices() {
        post("/door/events", (request, response) -> {
            Double temp = Double.parseDouble(request.queryParams("temp"));
            Boolean fake = !SECRET_KEY.equals(request.queryParams("key"));
            String door = request.queryParams("door");

            // insert event into database
            DoorEvent event = new DoorEvent(new Date(), temp, fake, door);
            Main.DB.getCollection("door").insertOne(event.toBson());
            return "success";
        });

        get("/door/events", (request, response) -> {
            String start = request.queryParams("start");
            String limit = request.queryParams("limit");
            String door = request.queryParams("door");

            // write a byte to start heroku's 55 second timeout window
            response.type("application/json");
            ServletOutputStream out = response.raw().getOutputStream();
            out.write(' ');
            out.flush();

            // build query
            List<Bson> criteria = new ArrayList<>();
            if (start != null) {
                criteria.add(Filters.gt("time", new Date(Long.parseLong(start))));
            }
            if (door != null) {
                criteria.add(Filters.eq("door", door));
            }
            final Bson query;
            if (criteria.isEmpty()) {
                query = new BsonDocument();
            } else if (criteria.size() == 1) {
                query = criteria.get(0);
            } else {
                query = Filters.and(criteria);
            }

            Bson projection = Projections.excludeId();
            int intLimit = limit != null ? Integer.parseInt(limit) : Integer.MAX_VALUE;
            Bson sort = Sorts.descending("time");
            MongoCollection<Document> collection = Main.DB.getCollection("door");

            // give up after 50 seconds to avoid heroku's timeout
            long timeout = System.currentTimeMillis() + (50 * 1000);
            List<DoorEvent> events = new ArrayList<>();
            while (events.size() == 0 && System.currentTimeMillis() < timeout) {
                // run query and collect results
                FindIterable<Document> cursor = collection
                    .find(query)
                    .projection(projection)
                    .limit(intLimit)
                    .sort(sort);

                for (Document document : cursor) {
                    events.add(new DoorEvent(document));
                }

                // sleep if might be looping again
                if (events.isEmpty()) {
                    Thread.sleep(500);
                }
            }

            // write json response
            out.print(Main.GSON.toJson(events));
            out.flush();
            return "";
        });
    }
}
