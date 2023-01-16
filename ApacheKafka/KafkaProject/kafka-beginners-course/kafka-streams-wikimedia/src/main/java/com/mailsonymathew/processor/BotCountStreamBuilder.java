package com.mailsonymathew.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;

import java.io.IOException;
import java.util.Map;

/*
Count the number of times a change was created by a bot versus a human
 */
public class BotCountStreamBuilder {

    private static final String BOT_COUNT_STORE = "bot-count-store";   // variable to store the bot count
    private static final String BOT_COUNT_TOPIC = "wikimedia.stats.bots";  // autocreated wikimedia topic for the bot count( no need to create manually)
    /*
    ObjectMapper provides functionality for reading and writing JSON, either to and from basic POJOs (Plain Old Java Objects),
    or to and from a general-purpose JSON Tree Model (JsonNode), as well as related functionality for performing conversions
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Kafka Input Stream
    private final KStream<String, String> inputStream;

    // Constructor Definition accepting the Kafka Input Stream
    public BotCountStreamBuilder(KStream<String, String> inputStream) {
        this.inputStream = inputStream;
    }


    /*
    Function which calculates tee Bot Count and write to the topic "wikimedia.stats.bots"
     */
    public void setup() {
        this.inputStream  // Refers to the Kafka Input Stream passed in the constructor above
                .mapValues(changeJson -> {  // for the JSON msg in the Kafka Input Stream
                    try {
                        final JsonNode jsonNode = OBJECT_MAPPER.readTree(changeJson);   // Get the JSON node
                        if (jsonNode.get("bot").asBoolean()) {   // if the node contains a value "bot"
                            return "bot";  // return string "bot"
                        }
                        return "non-bot";   // else return string "non-bot"
                    } catch (IOException e) {
                        return "parse-error";  // for a parsing error , return string "parse-error"
                    }
                })
                .groupBy((key, botOrNot) -> botOrNot)  // Group the recs of the Kafka Stream
                .count(Materialized.as(BOT_COUNT_STORE))  // materialize the above grouping into the final String variable "BOT_COUNT_STORE"
                .toStream() // convert to stream
                .mapValues((key, value) -> {
                    final Map<String, Long> kvMap = Map.of(String.valueOf(key), value);  // extract to a key,value map with the key containing the key value( E.g. 'bot':123, 'non-bot': 999)
                    try {
                        return OBJECT_MAPPER.writeValueAsString(kvMap);   // Write to a String Object
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .to(BOT_COUNT_TOPIC); // Write to Topic = "BOT_COUNT_TOPIC"
    }


}
