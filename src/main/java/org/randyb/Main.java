package org.randyb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.randyb.alexa.AlexaServices;
import org.randyb.applestore.AppleStore;
import org.randyb.door.DoorServices;
import org.randyb.phone.PhoneServices;
import org.randyb.statusboard.StatusBoardServices;
import org.randyb.steam.SteamTwitterBot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Exception;

import static spark.Spark.*;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    
    // heroku assigns incoming HTTP port via environment variable
    private static final int PORT = Integer.parseInt(System.getenv("PORT"));

    // mongo db connection
    public static final MongoDatabase DB;
    static {
        MongoClientURI uri = new MongoClientURI(System.getenv("MONGOLAB_URI"));
        DB = new MongoClient(uri).getDatabase(uri.getDatabase());
    }

    // json serializer
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        port(PORT);
        staticFileLocation("/");
        before((req, res) -> res.header("Access-Control-Allow-Origin", "*"));
        exception(Exception.class, (e, req, res) -> LOG.error("Exception from route", e));

        DoorServices.addServices();
        PhoneServices.addServices();
        SteamTwitterBot.addServices();
        AlexaServices.addServices();
        StatusBoardServices.addServices();
        AppleStore.addServices();

        awaitInitialization();
        LOG.info("Finished initialization");
    }
}
