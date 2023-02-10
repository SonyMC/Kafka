package com.mailsonymathew.kafka.streams;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

public class WordCountApp {






	public static void main(String[] args) {
		
		/*
		 *  Define configurations
		 */
        Properties config = new Properties();
        //Application ID
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-application");
        // Bootstrap Server 
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        // If our app is restarted or disconnected, read from the earlier data available
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Serde - Serialization and Deserialization for key
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        // Serde - Serialization and Deserialization for value
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());



        WordCountApp wordCountApp = new WordCountApp();

        KafkaStreams streams = new KafkaStreams(wordCountApp.createTopology(), config);

        // Start the Stream
        streams.start();

        // Shutdown hook to correctly close the streams application
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        // Print the topology every 10 seconds for learning purposes
        while(true){
            streams.metadataForLocalThreads().forEach(data -> System.out.println(data));
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }
        }
		
	}

    /*

    *** Note:  We will do the following 7 operations to do a word counts:

    1. Create a Stream from Kafka
            - E.g. <null, "Kafka Kafka Streams">
    2. Map the values to lowercase
            - E.g. <null, "kafka kafka streams">
    3. FlatMapValues split by space
            - E.g. <null,"kafka">,<null,"kafka">,<null,"streams">
    4. SelectKey to apply a key
            - E.g. <"kafka","kafka">,<"kafka","kafka">,<"streams","streams">
    5. GroupByKey before aggregation
            - E.g. (<"kafka","kafka">,<"kafka","kafka">),(<"streams","streams">)
    6. Count occurrences in each group
            - E.g. <"kafka",2>,<"streams",1>)
    7. 'To' operation in order to write results back to Kafka
            - data point is written to Kafka

     */
        public Topology createTopology(){

            // Note: Use KStream Builder for older Kafka versions
            StreamsBuilder builder = new StreamsBuilder();
            // 1a. - stream from Kafka topic "word-count-input"
            KStream<String, String> textLines = builder.stream("word-count-input");

            KTable<String, Long> wordCounts =
                    textLines
                    // 2 - map values to lowercase
                    .mapValues(textLine -> textLine.toLowerCase())
                    // can be alternatively written as a method reference:
                    // .mapValues(String::toLowerCase)
                    // 3 - flatmap values split by space and put it in a List
                    .flatMapValues(textLine -> Arrays.asList(textLine.split("\\W+")))
                    // 4 - select key to apply a key (we discard the old key)
                    .selectKey((key, word) -> word)  // Here we are replacing the key with the word value
                    // 5 - group by key before aggregation
                    .groupByKey()
                    // 6 - count occurrences
                    .count(Materialized.as("Counts"));

            // 7 - to in order to write the results back to kafka
            wordCounts
                    .toStream()
                    .to("word-count-output",   Produced.with(Serdes.String(), Serdes.Long()));

            return builder.build(); // Return the built topology
        }

}
