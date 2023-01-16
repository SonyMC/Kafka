package com.mailsonymathew;

import com.mailsonymathew.processor.BotCountStreamBuilder;
import com.mailsonymathew.processor.EventCountTimeseriesBuilder;
import com.mailsonymathew.processor.WebsiteCountStreamBuilder;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
/*
App to do the following from the wikimedia.recentchange topic:
		- Count the number of times a change was created by a bot versus a human  - processor/BotCountStreamBuilder.java
		- Analyze number of changes per Wikimedia website - processor/WebsiteCountStreamBuilder.java
		- No. of edits per 10s as a time series - processor/EventCountTimeseriesBuilder.java
 */
public class WikimediaStreamsApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikimediaStreamsApp.class);

    /*
     TConfigure Stream Properties.
     The Properties can be saved to a stream or loaded from a stream.
     Each key and its corresponding value in the property list is a string.
     */
    private static final Properties properties;  // this final variable is initialized in teh static block further down.
    // Topic to be used
    private static final String INPUT_TOPIC = "wikimedia.recentchange";

    static {
        properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "wikimedia-stats-application");    // Stream Name
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");  // Kafka Cluster url
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());  // Factory for creating serializers / deserializers for Key.
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass()); // Factory for creating serializers / deserializers for Value.
    }

    public static void main(String[] args) {

        // Stream Builder Object
        StreamsBuilder builder = new StreamsBuilder();

        // Kafka Stream using the Stream Builder Object taking the stream from the topic ""wikimedia.recentchange"
        KStream<String, String> changeJsonStream = builder.stream(INPUT_TOPIC);

        // Topology to Count the number of times a change was created by a bot versus a human  - processor/BotCountStreamBuilder.java
        BotCountStreamBuilder botCountStreamBuilder = new BotCountStreamBuilder(changeJsonStream);
        botCountStreamBuilder.setup();

        // Topology to Analyze number of changes per Wikimedia websie - processor/WebsiteCountStreamBuilder.java
        WebsiteCountStreamBuilder websiteCountStreamBuilder = new WebsiteCountStreamBuilder(changeJsonStream);
        websiteCountStreamBuilder.setup();

        // Topology for No. of edits per 10s as a time series - processor/EventCountTimeseriesBuilder.java
        EventCountTimeseriesBuilder eventCountTimeseriesBuilder = new EventCountTimeseriesBuilder(changeJsonStream);
        eventCountTimeseriesBuilder.setup();

        /*
         Topology -> A logical representation of a ProcessorTopology.
         Reference : https://docs.confluent.io/platform/current/streams/architecture.html#processor-topology
         A topology is an acyclic graph of sources, processors, and sinks.
         A source is a node in the graph that consumes one or more Kafka topics and forwards them to its successor nodes.
         A processor is a node in the graph that receives input records from upstream nodes, processes the records, and optionally forwarding new records to one or all of its downstream nodes.
         Finally, a sink is a node in the graph that receives records from upstream nodes and writes them to a Kafka topic.
         A Topology allows you to construct an acyclic graph of these nodes, and then passed into a new KafkaStreams instance that will then begin consuming, processing, and producing records.
         Note : Here we have used Processor Topology.
         */
        final Topology appTopology = builder.build();
        LOGGER.info("Topology: {}", appTopology.describe());  // Log the Topology
        KafkaStreams streams = new KafkaStreams(appTopology, properties); // New Kafka Stream using the Topology and stream Properties ( defined at the beginning)
        streams.start();  // Start the Stream
    }


}
