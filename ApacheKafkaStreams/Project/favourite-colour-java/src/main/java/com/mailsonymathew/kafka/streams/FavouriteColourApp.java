package com.mailsonymathew.kafka.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Arrays;
import java.util.Properties;

public class FavouriteColourApp {

    public static void main(String[] args) {

        /*
         *  Define configurations
         */
        Properties config = new Properties();
        //Application ID
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "favourite-colour-java");
        // Bootstrap Server
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        // If our app is restarted or disconnected, read from the earlier data available
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Serde - Serialization and Deserialization for key
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        // Serde - Serialization and Deserialization for value
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // Important: Additional step only for DEV: we disable the cache to demonstrate all the "steps" involved in the transformation - not recommended in prod
        // Cache is enabled by default and helps improves efficiency
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");

        FavouriteColourApp favouriteColourApp = new FavouriteColourApp();

        KafkaStreams streams =new KafkaStreams(favouriteColourApp.buildTopology(), config);

        // Only do this in dev - not in prod
        streams.cleanUp();
        //Start the Stream
        streams.start();



        // Print the topology every 10 seconds for learning purposes
        while(true){
            streams.metadataForLocalThreads().forEach(data -> System.out.println(data));
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }
        }

        //Shutdown hook to correctly close the streams application
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private Topology buildTopology() {


    StreamsBuilder builder = new StreamsBuilder();
        // Step 1: We create the topic of users keys to colours
        // Note: Our keys will be null in the input stream and we would need ot create the key from the input values based on the position of the data with respect to the comma
        KStream<String, String> textLines = builder.stream("favourite-colour-input");

        KStream<String, String> usersAndColours =
                textLines
                // 1 - we ensure that a comma is input in the data here as we will split on it
                .filter((key, value) -> value.contains(","))
                // 2 - we select a key that will be the user id (lowercase for safety)
                .selectKey((key, value) -> value.split(",")[0].toLowerCase()) // i.e. take the first element 'id' as the key
                // 3 - we get the colour from the value (lowercase for safety)
                .mapValues(value -> value.split(",")[1].toLowerCase()) // i.e. take the second element as the colour
                // 4 - we filter undesired colours (could be a data sanitization step
                .filter((user, colour) -> Arrays.asList("green", "blue", "red").contains(colour));  // consider only green, blue and red

        // Write above result to the topic '"user-keys-and-colours"'
        usersAndColours.to("user-keys-and-colours");

        // Variable definition
        Serde<String> stringSerde = Serdes.String();
        Serde<Long> longSerde = Serdes.Long();

        // Step 2 - we read that topic as a KTable so that updates are read correctly( Note; Updates will not be available in a KStream and hence we use a KTable)
        KTable<String, String> usersAndColoursTable = builder.table("user-keys-and-colours");

        // Step 3 - we count the occurrences of colours
        KTable<String, Long> favouriteColours = usersAndColoursTable
                // 5 - we group by colour within the KTable
                .groupBy((user, colour) -> new KeyValue<>(colour, colour))
                .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("CountsByColours")
                        .withKeySerde(stringSerde)
                        .withValueSerde(longSerde));

        // 6 - we output the results to a Kafka Topic - don't forget the serializers
        favouriteColours.toStream().to("favourite-colour-output", Produced.with(Serdes.String(),Serdes.Long()));

        return builder.build(); // Return the built topology
    }
}


