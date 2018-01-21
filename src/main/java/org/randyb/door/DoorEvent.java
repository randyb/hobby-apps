package org.randyb.door;

import org.bson.Document;

import java.util.Date;

public class DoorEvent {
    private long time;
    private double temp;
    private boolean fake;
    private String door;

    public DoorEvent(Date time, double temp, boolean fake, String door) {
        this.time = time.getTime();
        this.temp = temp;
        this.fake = fake;
        this.door = door;
    }

    public DoorEvent(Document bson) {
        this.time = bson.getDate("time").getTime();
        this.temp = bson.getDouble("temp");
        this.fake = bson.getBoolean("fake");
        this.door = bson.getString("door");
    }

    public Document toBson() {
        return new Document()
            .append("time", new Date(time))
            .append("temp", temp)
            .append("fake", fake)
            .append("door", door);
    }
}
