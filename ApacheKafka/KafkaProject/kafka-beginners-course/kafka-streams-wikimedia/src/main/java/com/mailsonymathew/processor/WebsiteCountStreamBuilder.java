package com.mailsonymathew.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/*
Analyze number of changes per website
 */
public class WebsiteCountStreamBuilder {


    private static final String WEBSITE_COUNT_STORE = "website-count-store";  // variable for website count
    private static final String WEBSITE_COUNT_TOPIC = "wikimedia.stats.website"; // autocreated wikimedia topic( ( no need to create manually))
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();  // object mapper

    private final KStream<String, String> inputStream; // kafka input stream

    // Constructor Definition accepting the Kafka Input Stream
    public WebsiteCountStreamBuilder(KStream<String, String> inputStream) {

        this.inputStream = inputStream;
    }

    /*
    number of changes per website and write to the topic "wikimedia.stats.website"
     */
    public void setup() {
        // time window
        final TimeWindows timeWindows = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1L));
        this.inputStream
                /*
                Set a new key  for each input record.
                The provided KeyValueMapper is applied to each input record and computes a new key for it. T
                hus, an input record <K,V> can be transformed into an output record <K':V
                 */
                .selectKey((k, changeJson) -> {
                    try {
                        final JsonNode jsonNode = OBJECT_MAPPER.readTree(changeJson);  // get nodes
                        return jsonNode.get("server_name").asText();  // get node = "server-name"
                    } catch (IOException e) {
                        return "parse-error";
                    }
                })
                .groupByKey()  // group by key
                .windowedBy(timeWindows)  //window by 1 minute
                /*
                Used to describe how a StateStore should be materialized.
                */
                .count(Materialized.as(WEBSITE_COUNT_STORE))
                .toStream()
                .mapValues((key, value) -> {  // ne wmap
                    final Map<String, Object> kvMap = Map.of(
                            "website", key.key(),  //key
                            "count", value  //value
                    );
                    try {
                        return OBJECT_MAPPER.writeValueAsString(kvMap);   // Return as String Object
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                /*
                Produce to Topic= wikimedia.stats.website
               */
                .to(WEBSITE_COUNT_TOPIC, Produced.with(
                        WindowedSerdes.timeWindowedSerdeFrom(String.class, timeWindows.size()),
                        Serdes.String()
                ));
    }
}
