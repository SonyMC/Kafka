package com.mailsonymathew.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;
import java.util.Map;

/*
No. of edits per 10s as a time series
 */
public class EventCountTimeseriesBuilder {


    private static final String TIMESERIES_TOPIC = "wikimedia.stats.timeseries";  // autocreated wikimedia topic( ( no need to create manually))
    private static final String TIMESERIES_STORE = "event-count-store";  // variable to store teh edits for the timeseries
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();  // object mapper

    private final KStream<String, String> inputStream;  // Kafka input stream

    // Constructor with input as teh Kafka Stream
    public EventCountTimeseriesBuilder(KStream<String, String> inputStream) {

        this.inputStream = inputStream;
    }

    // Calculate the no. of edits and write to the topic "wikimedia.stats.timeseries"
    public void setup() {
        // Time Window ot 10 secs
        final TimeWindows timeWindows = TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(10));
        this.inputStream
                /*
                Set a new key "key-to-group"" for each input record.
                The provided KeyValueMapper is applied to each input record and computes a new key for it.
                Thus, an input record <K,V> can be transformed into an output record <K':V>.
                This is a stateless record-by-record operation.
                 */
                .selectKey((key, value) -> "key-to-group")
                .groupByKey()  // group by the new key = "key-to-group"
                .windowedBy(timeWindows)// window every 10 secs
                /*
                Used to describe how a StateStore should be materialized.
                 */
                .count(Materialized.as(TIMESERIES_STORE))
                .toStream() // to stream
                .mapValues((readOnlyKey, value) -> {  // map values
                    final Map<String, Object> kvMap = Map.of(   // new key values map wiyth following values
                            "start_time", readOnlyKey.window().startTime().toString(),
                            "end_time", readOnlyKey.window().endTime().toString(),
                            "window_size", timeWindows.size(),
                            "event_count", value
                    );
                    try {
                        return OBJECT_MAPPER.writeValueAsString(kvMap);  // write out the above map a String Object
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                /*
                Produce to Topic= wikimedia.stats.timeseries
                 */
                .to(TIMESERIES_TOPIC, Produced.with(
                        /*
                        Construct a TimeWindowedSerde object to deserialize changelog topic for the specified inner class type and window size.
                         */
                        WindowedSerdes.timeWindowedSerdeFrom(String.class, timeWindows.size()),
                        Serdes.String()
                ));
    }
}
