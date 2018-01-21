package org.randyb.alexa;

import com.amazon.speech.Sdk;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.json.SpeechletResponseEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SpeechletRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.authentication.SpeechletRequestSignatureVerifier;
import com.amazon.speech.speechlet.verifier.ApplicationIdSpeechletRequestEnvelopeVerifier;
import com.amazon.speech.speechlet.verifier.TimestampSpeechletRequestVerifier;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import okhttp3.OkHttpClient;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.randyb.Main.DB;
import static org.randyb.Main.GSON;
import static spark.Spark.*;

public class AlexaServices {
    private static final Logger LOG = LoggerFactory.getLogger(AlexaServices.class);
    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final Random RANDOM = new Random();
    private static int foodCounter = 0;

    // rejects incoming requests if they are too old. required by alexa API.
    private static final TimestampSpeechletRequestVerifier TIME_VERIFIER =
        new TimestampSpeechletRequestVerifier(150, TimeUnit.SECONDS);

    // verifies incoming skill IDs. required by alexa API.
    private static final ApplicationIdSpeechletRequestEnvelopeVerifier APP_ID_VERIFIER =
            new ApplicationIdSpeechletRequestEnvelopeVerifier(ImmutableSet.copyOf(
                    System.getenv("SKILL_IDS").split(",")));

    // maps incoming intent to its static handler method
    private static final Map<String, Function<Intent, String>> INTENTS = ImmutableMap.of(
            "OverwatchIntent", AlexaServices::overwatch,
            "DoorIntent", AlexaServices::door,
            "FoodIntent", AlexaServices::food,
            "SillyIntent", AlexaServices::silly);

    private static List<String> SILLY_WORDS;
    static {
        try {
            SILLY_WORDS = Resources.readLines(AlexaServices.class.getResource(
                    "/alexa/sillywords.txt"), Charsets.UTF_8);
        } catch (Exception e) {
            LOG.error("Error reading sillywords.txt", e);
        }
    }

    public static void addServices() {
        get("/alexa", (req, res) -> new Date());
        post("/alexa", AlexaServices::handleAlexaRequest);
        before("/alexa", (req, res) -> LOG.info("REQUEST:\n{}\n", GSON.toJson(JSON_PARSER.parse(req.body()))));
        after("/alexa", (req, res) -> LOG.info("RESPONSE:\n{}\n", GSON.toJson(JSON_PARSER.parse(res.body()))));
    }

    private static String handleAlexaRequest(Request request, Response response) throws Exception {
        SpeechletRequest speechletRequest = getSpeechRequest(request);

        final String textToSay;

        if (speechletRequest instanceof IntentRequest) {
            Intent intent = ((IntentRequest) speechletRequest).getIntent();
            textToSay = INTENTS.get(intent.getName()).apply(intent);
        } else if (speechletRequest instanceof LaunchRequest) {
            textToSay = silly(null);
        } else {
            throw new UnsupportedOperationException();
        }

        SpeechletResponseEnvelope responseEnvelope = new SpeechletResponseEnvelope();
        responseEnvelope.setVersion(Sdk.VERSION);
        SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
        outputSpeech.setSsml("<speak>" + textToSay + "</speak>");
        responseEnvelope.setResponse(SpeechletResponse.newTellResponse(outputSpeech));
        response.type("application/json");
        return responseEnvelope.toJsonString();
    }

    private static SpeechletRequest getSpeechRequest(Request request) throws Exception {
        SpeechletRequestSignatureVerifier.checkRequestSignature(
                request.bodyAsBytes(),
                request.headers(Sdk.SIGNATURE_REQUEST_HEADER),
                request.headers(Sdk.SIGNATURE_CERTIFICATE_CHAIN_URL_REQUEST_HEADER));

        SpeechletRequestEnvelope<?> requestEnvelope = SpeechletRequestEnvelope.fromJson(
                request.bodyAsBytes());

        if (!APP_ID_VERIFIER.verify(requestEnvelope)) {
            throw new IllegalArgumentException("bad app id");
        }

        SpeechletRequest speechRequest = requestEnvelope.getRequest();

        if (!TIME_VERIFIER.verify(speechRequest, requestEnvelope.getSession())) {
            throw new IllegalArgumentException("bad timestamp");
        }

        return speechRequest;
    }

    private static String door(Intent intent) {
        MongoCollection<Document> collection = DB.getCollection("door");
        Date date = collection.find()
                .limit(1)
                .sort(Sorts.descending("time"))
                .first()
                .getDate("time");
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("US/Central"));
        return "The door was last opened at " + df.format(date);
    }

    private static String overwatch(Intent intent)  {
        try {
            String url = "https://owapi.net/api/v3/u/Randy-1616/stats";
            okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
            String statsString = HTTP_CLIENT.newCall(request).execute().body().string();
            JsonObject stats = JSON_PARSER.parse(statsString).getAsJsonObject();
            JsonObject overallStats = stats
                    .getAsJsonObject("us")
                    .getAsJsonObject("stats")
                    .getAsJsonObject("competitive")
                    .getAsJsonObject("overall_stats");
            int compRank = overallStats.get("comprank").getAsInt();
            String tier = overallStats.get("tier").getAsString();
            double winRate = overallStats.get("win_rate").getAsDouble();

            return String.format("You are %s tier with a skill rating of %d and win rate of %.1f%%",
                    tier, compRank, winRate);
        } catch (Exception e) {
            LOG.error("Error getting overwatch stats", e);
            return "Sorry, I couldn't get your stats.";
        }
    }

    private static String food(Intent intent) {
        String[] words = {"hungry", "hungover", "hungarian"};
        return String.format("Jinkies, ... are you ... %s?", words[foodCounter++ % words.length]);
    }

    private static String silly(Intent intent) {
        int numWords = 5;

        if (intent != null) {
            String slot = intent.getSlot("numWords").getValue();
            if (slot != null) {
                numWords = Integer.parseInt(slot);
            }
        }

        numWords = IntStream.of(50, numWords, SILLY_WORDS.size()).min().getAsInt();

        Set<String> words = new HashSet<>();
        while (words.size() < numWords) {
            words.add(SILLY_WORDS.get(RANDOM.nextInt(SILLY_WORDS.size())));
        }

        return words.stream()
                .map(w -> "<say-as interpret-as=\"interjection\">" + w + "</say-as><break/>")
                .collect(Collectors.joining());
    }
}
